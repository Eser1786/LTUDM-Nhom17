package com.tkb.backend;

import com.tkb.backend.dao.CatalogDao;
import com.tkb.backend.dao.PlanDao;
import com.tkb.backend.dao.UserDao;
import com.tkb.backend.db.DatabaseConfig;
import com.tkb.backend.net.ClientSession;
import com.tkb.backend.service.AuthService;
import com.tkb.backend.service.CatalogCache;
import com.tkb.backend.service.JobManager;
import com.tkb.backend.service.PdfImportService;
import com.tkb.backend.solver.TimetableSolver;
import com.tkb.common.crypto.CryptoUtil;
import com.tkb.common.dto.UserDto;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * B3.1, B3.5 - TkbServer: Class chinh (Main Entry Point) khoi dong TCP Server tai cong 9000.
 *
 * Nhiem vu:
 *   1. Sinh cap khoa RSA-2048 dung chung cho Server de bat tay trao khoa phien AES-256 voi tung Client.
 *   2. Khoi tao Connection Pool HikariCP (DatabaseConfig), cac lop DAO, AuthService (tu tao tai khoan ADMIN dau tien),
 *      CatalogCache (nap san du lieu hoc ky hien tai len RAM), TimetableSolver, JobManager va PdfImportService.
 *   3. Mo ServerSocket da luong phuc vu dong thoi nhieu Client tren mang LAN / Internet.
 *   4. Ghi log day du moi hoat dong cua Server (B3.5).
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/TkbServer.java
 */
public class TkbServer {

    public static final int DEFAULT_PORT = 9000;
    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int port;
    private final long startedAtMs;
    private final KeyPair rsaKeyPair;
    private final CatalogDao catalogDao;
    private final UserDao userDao;
    private final PlanDao planDao;
    private final AuthService authService;
    private final CatalogCache catalogCache;
    private final TimetableSolver solver;
    private final JobManager jobManager;
    private final PdfImportService pdfImportService;

    private final ExecutorService clientExecutor;
    private final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private final AtomicLong sessionSeq = new AtomicLong(1);
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public TkbServer(int port) {
        this.port = port;
        this.startedAtMs = System.currentTimeMillis();
        log("Dang sinh cap khoa RSA-2048 cho Server...");
        this.rsaKeyPair = CryptoUtil.generateRsaKeyPair();

        this.catalogDao = new CatalogDao();
        this.userDao = new UserDao();
        this.planDao = new PlanDao();
        this.authService = new AuthService(userDao);
        this.catalogCache = new CatalogCache(catalogDao);
        this.solver = new TimetableSolver();
        this.jobManager = new JobManager(Runtime.getRuntime().availableProcessors());
        this.pdfImportService = new PdfImportService(catalogCache);

        this.clientExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("TKB-ClientHandler-" + t.threadId());
            return t;
        });
    }

    /**
     * Khoi dong Server va lang nghe ket noi TCP.
     */
    public void start() throws IOException {
        // 1. Kiem tra ket noi MySQL va tao tai khoan ADMIN mac dinh (admin / admin12345) neu chua co (B4.3)
        DatabaseConfig.getDataSource();
        authService.ensureDefaultAdminExists();

        // 2. Nap truoc (warm-up) hoc ky hien tai len RAM Cache (B5.1)
        try {
            String currentSemester = catalogDao.getCurrentSemesterCode();
            if (currentSemester != null) {
                Map<String, ?> map = catalogCache.getAllActiveSectionsByCourse(currentSemester);
                log("Da nap san Cache RAM cho hoc ky hien tai '" + currentSemester + "' (" + map.size() + " hoc phan mo lop).");
            }
        } catch (Exception e) {
            log("Canh bao khi nap truoc Cache RAM: " + e.getMessage());
        }

        // 3. Dang ky Shutdown Hook de dong pool sach se khi tat Server
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "TKB-ShutdownHook"));

        // 4. Mo ServerSocket lang nghe Client
        this.serverSocket = new ServerSocket(port);
        log("=== TKB SERVER DA KHOI DONG TAI CONG TCP " + port + " ===");

        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String sessionId = "s" + sessionSeq.getAndIncrement();

                ClientSession session = new ClientSession(
                        sessionId,
                        clientSocket,
                        this,
                        rsaKeyPair,
                        catalogDao,
                        userDao,
                        planDao,
                        authService,
                        catalogCache,
                        solver,
                        jobManager,
                        pdfImportService
                );
                activeSessions.put(sessionId, session);
                clientExecutor.submit(session);
            } catch (IOException e) {
                if (running) {
                    log("Loi khi nhan ket noi Client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Dung Server va giai phong tai nguyen.
     */
    public void stop() {
        if (!running) return;
        running = false;
        log("Dang dung TKB Server...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientSession s : activeSessions.values()) {
            s.closeSession();
        }
        activeSessions.clear();
        jobManager.shutdown();
        clientExecutor.shutdownNow();
        DatabaseConfig.close();
        log("TKB Server da dung hoan toan.");
    }

    public void unregisterSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public Set<Long> getOnlineUserIds() {
        Set<Long> set = new LinkedHashSet<>();
        for (ClientSession s : activeSessions.values()) {
            UserDto u = s.getAuthenticatedUser();
            if (u != null) {
                set.add(u.getUserId());
            }
        }
        return set;
    }

    public int countOnlineAuthenticatedUsers() {
        return getOnlineUserIds().size();
    }

    public long getUptimeSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - startedAtMs) / 1000L);
    }

    public List<Map<String, Object>> listActiveSessionSummaries() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClientSession s : activeSessions.values()) {
            UserDto u = s.getAuthenticatedUser();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", s.getSessionId());
            item.put("username", u != null ? u.getUsername() : "(chưa đăng nhập)");
            item.put("remoteAddress", s.getRemoteAddress());
            item.put("connectedAt", s.getConnectedAt());
            list.add(item);
        }
        return list;
    }

    public boolean kickSession(String sessionId) {
        ClientSession target = activeSessions.remove(sessionId);
        if (target == null) {
            return false;
        }
        target.closeSession();
        log("Admin da ngat phien [" + sessionId + "]");
        return true;
    }

    public void log(String message) {
        String ts = LocalDateTime.now().format(LOG_TIME_FMT);
        System.out.printf("[%s] [INFO] %s%n", ts, message);
    }

    public void logError(String message, Throwable t) {
        String ts = LocalDateTime.now().format(LOG_TIME_FMT);
        System.err.printf("[%s] [ERROR] %s: %s%n", ts, message, t != null ? t.getMessage() : "");
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("TKB_PORT");
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {
            }
        } else if (envPort != null && !envPort.isBlank()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        TkbServer server = new TkbServer(port);
        try {
            server.start();
        } catch (IOException e) {
            server.logError("Khong the khoi dong TKB Server tai cong " + port, e);
            System.exit(1);
        }
    }
}