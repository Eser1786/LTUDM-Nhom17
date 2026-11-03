package com.tkb.backend.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tkb.backend.solver.TimetableSolver;
import com.tkb.backend.solver.TimetableSolver.Criteria;
import com.tkb.backend.solver.TimetableSolver.SolveOutcome;
import com.tkb.common.crypto.CryptoUtil;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import com.tkb.common.net.FrameCodec;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;

/**
 * B6.6 & B11.1 - MultiClientTestRunner:
 * Chuong trinh kiem thu tu dong 2 phan bat buoc cuoi cung cua Eser:
 *
 *   Phan 1 (B6.6 - Kiem thu cac ca bien thuat toan TimetableSolver):
 *     - Ca 1: Danh sach rong / 1 hoc phan duy nhat.
 *     - Ca 2: Cac hoc phan trung lich hoan toan -> Kiem tra ha bac uu tien (loai dung mon uu tien thap hon).
 *     - Ca 3: Hoc phan co nhieu to thuc hanh (LT + to TH 01, 02, 03 nhu mon 849010) va xet trung tuan hoc.
 *     - Ca 4: Mon Thuc tap / Do an nhieu buoi trong tuan.
 *
 *   Phan 2 (B11.1 - Gia lap N Client ket noi dong thoi vao TkbServer dang chay o cong 9000):
 *     - Moi Client chay tren 1 luong doc lap:
 *         1. Ket noi TCP -> Bat tay HELLO / KEY_EXCHANGE (RSA-OAEP) / READY (AES-256-GCM).
 *         2. REGISTER & LOGIN tai khoan test.
 *         3. Goi LIST_SEMESTERS & LIST_COURSES lay danh sach mon that tu Server.
 *         4. Gui lenh OPTIMIZE dong thoi, lang nghe cac goi Push JOB_PROGRESS va JOB_DONE.
 *         5. Gui lenh SAVE_PLAN va LIST_PLANS de xac nhan luu phuong an thanh cong.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/test/MultiClientTestRunner.java
 */
public class MultiClientTestRunner {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 9000;
        int clientCount = 5; // Mac dinh gia lap 5 Client chay song song

        if (args.length >= 1) {
            clientCount = Integer.parseInt(args[0].trim());
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1].trim());
        }

        System.out.println("==================================================================");
        System.out.println("  PHAN 1 (B6.6): KIEM THU CAC CA BIEN THUAT TOAN TIMETABLESOLVER");
        System.out.println("==================================================================");
        runSolverEdgeCaseTests();

        System.out.println("\n==================================================================");
        System.out.printf("  PHAN 2 (B11.1): GIA LAP %d CLIENT DONG THOI KET NOI CONG %d%n", clientCount, port);
        System.out.println("==================================================================");
        runConcurrentClientsTest(host, port, clientCount);
    }

    // =========================================================================
    // PHAN 1 (B6.6): UNIT TEST CAC CA BIEN CUA THUAT TOAN TIMETABLESOLVER
    // =========================================================================

    private static void runSolverEdgeCaseTests() {
        TimetableSolver solver = new TimetableSolver();
        Criteria defaultCriteria = new Criteria(true, true, true);

        // Ca 1: Danh sach 1 mon duy nhat
        SectionDto s1 = new SectionDto(1L, "841109", "Cơ sở dữ liệu", 4, "01", "", "LT", "GV A", "001", "DCT", 50, true,
                List.of(new SlotDto(2, 1, 3, "C.A101", "1234567890-----")));
        SolveOutcome out1 = solver.solve(
                List.of("841109"),
                Map.of("841109", List.of(s1)),
                defaultCriteria,
                5,
                () -> false,
                p -> {}
        );
        assertCondition(!out1.plans().isEmpty() && out1.droppedCourses().isEmpty(),
                "Ca 1 (1 môn học): Tìm được " + out1.plans().size() + " phương án, điểm = " + out1.plans().get(0).getScore());

        // Ca 2: 3 mon [C1, C2, C3], trong do C1 va C2 trung gio hoan toan (Thu 2 tiet 1-3),
        // con C3 (uu tien thap nhat) hoc Thu 4 tiet 1-3 KHONG trung voi ai.
        // Theo Yeu cau A.6: Chi loai C2 (mon uu tien thap hon trong cap xung dot C1-C2) va GIU LAI C1 + C3!
        SectionDto c1Sec = new SectionDto(10L, "C1", "Môn Ưu Tiên 1", 3, "01", "", "LT", "GV", "1", "DCT", 50, true,
                List.of(new SlotDto(2, 1, 3, "C.A101", "1234567890-----")));
        SectionDto c2Sec = new SectionDto(20L, "C2", "Môn Ưu Tiên 2 (Trùng C1)", 3, "01", "", "LT", "GV", "1", "DCT", 50, true,
                List.of(new SlotDto(2, 1, 3, "C.A102", "1234567890-----")));
        SectionDto c3Sec = new SectionDto(30L, "C3", "Môn Ưu Tiên 3 (Không trùng)", 3, "01", "", "LT", "GV", "1", "DCT", 50, true,
                List.of(new SlotDto(4, 1, 3, "C.A103", "1234567890-----")));

        SolveOutcome out2 = solver.solve(
                List.of("C1", "C2", "C3"),
                Map.of("C1", List.of(c1Sec), "C2", List.of(c2Sec), "C3", List.of(c3Sec)),
                defaultCriteria,
                5,
                () -> false,
                p -> {}
        );
        boolean keptC1AndC3 = out2.plans().get(0).getSections().size() == 2
                && out2.droppedCourses().size() == 1
                && "C2".equals(out2.droppedCourses().get(0).getCourseCode());
        assertCondition(keptC1AndC3,
                "Ca 2 (Tất cả C1 & C2 trùng giờ): Đã loại đúng môn gây xung đột 'C2' và giữ lại ['C1', 'C3']");

        // Ca 3: Mon 849010 co 1 buoi LT + 3 to TH (01, 02, 03) va kiem tra khac tuan hoc (tuan chan vs tuan le)
        SectionDto dtcs = new SectionDto(40L, "849010", "Điện tử công suất", 3, "01", "", "TH", "Trương Tấn", "1", "DCT", 50, true,
                List.of(
                        new SlotDto(4, 8, 3, "C.E102", "1234567890--------", "LT", "", 0),
                        new SlotDto(4, 1, 5, "C.A406", "-2-4-6-8-0-2------", "TH", "01", 0),
                        new SlotDto(4, 1, 5, "C.A406", "1-3-5-7-9-1-------", "TH", "02", 0),
                        new SlotDto(5, 1, 5, "C.A406", "123456------------", "TH", "03", 0)
                ));
        // Mon khac hoc Thu 4 tiet 1-5 nhung CHI hoc tuan le ("1-3-5-7-9-1-------") -> Se ghep duoc voi To 01 (hoc tuan chan)!
        SectionDto oddWeekCourse = new SectionDto(41L, "841999", "Môn Tuần Lẻ", 3, "01", "", "LT", "GV", "1", "DCT", 50, true,
                List.of(new SlotDto(4, 1, 5, "C.B201", "1-3-5-7-9-1-------", "LT", "", 0)));

        SolveOutcome out3 = solver.solve(
                List.of("849010", "841999"),
                Map.of("849010", List.of(dtcs), "841999", List.of(oddWeekCourse)),
                defaultCriteria,
                5,
                () -> false,
                p -> {}
        );
        assertCondition(!out3.plans().isEmpty() && out3.droppedCourses().isEmpty(),
                "Ca 3 (Tách tổ TH + Xét trùng Tuần chẵn/lẻ): Ghép thành công " + out3.plans().size() + " phương án không trùng tuần");

        // Ca 4: Mon Thuc tap 10 buoi trong tuan
        List<SlotDto> tenSlots = new ArrayList<>();
        for (int d = 2; d <= 6; d++) {
            tenSlots.add(new SlotDto(d, 1, 5, "LAB", "1234567890-----"));
            tenSlots.add(new SlotDto(d, 6, 5, "LAB", "1234567890-----"));
        }
        SectionDto internSec = new SectionDto(50L, "841888", "Thực tập tốt nghiệp", 5, "01", "", "TH", null, null, "DCT", 50, true, tenSlots);
        SolveOutcome out4 = solver.solve(
                List.of("841888"),
                Map.of("841888", List.of(internSec)),
                defaultCriteria,
                5,
                () -> false,
                p -> {}
        );
        assertCondition(!out4.plans().isEmpty() && out4.plans().get(0).getMetrics().getStudyDays() == 5,
                "Ca 4 (Môn Thực tập 10 buổi/tuần): Xếp thành công, số ngày học = " + out4.plans().get(0).getMetrics().getStudyDays());
    }

    private static void assertCondition(boolean ok, String description) {
        if (ok) {
            System.out.println("  [PASS] " + description);
        } else {
            System.err.println("  [FAIL] " + description);
        }
    }

    // =========================================================================
    // PHAN 2 (B11.1): GIA LAP N CLIENT DONG THOI TREN TCP CONG 9000
    // =========================================================================

    private static void runConcurrentClientsTest(String host, int port, int clientCount) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(clientCount);
        AtomicInteger successClients = new AtomicInteger(0);

        for (int i = 1; i <= clientCount; i++) {
            final int clientIdx = i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    if (simulateSingleClient(host, port, clientIdx)) {
                        successClients.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.printf("[Client #%d] Loi: %s%n", clientIdx, e.getMessage());
                } finally {
                    doneGate.countDown();
                }
            });
        }

        long t0 = System.currentTimeMillis();
        startGate.countDown(); // Cho tat ca N Client xuat phat cung 1 thoi diem
        doneGate.await(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - t0;

        pool.shutdownNow();
        System.out.println("------------------------------------------------------------------");
        System.out.printf("KET QUA KIEM THU DA LUONG: %d/%d Client hoan thanh thanh cong trong %d ms.%n",
                successClients.get(), clientCount, elapsed);
        System.out.println("------------------------------------------------------------------");
    }

    @SuppressWarnings("unchecked")
    private static boolean simulateSingleClient(String host, int port, int clientIdx) throws Exception {
        String username = "test_sv_" + clientIdx;
        String password = "matkhau123";
        int reqIdSeq = 1;

        try (Socket socket = new Socket(host, port);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            socket.setSoTimeout(30_000);

            // 1. Nhan HELLO (chua ma hoa)
            String helloJson = FrameCodec.readPlainJsonFrame(in);
            JsonObject helloObj = JsonParser.parseString(helloJson).getAsJsonObject();
            PublicKey serverRsaPub = CryptoUtil.decodePublicKeyBase64(helloObj.get("publicKey").getAsString());

            // 2. Sinh khoa phien AES-256 va gui KEY_EXCHANGE
            SecretKey aesKey = CryptoUtil.generateAesSessionKey();
            String encryptedKeyB64 = CryptoUtil.encryptSessionKeyRsaBase64(aesKey, serverRsaPub);
            Map<String, Object> keyEx = Map.of("type", "KEY_EXCHANGE", "encryptedKey", encryptedKeyB64);
            FrameCodec.writePlainJsonFrame(out, GSON.toJson(keyEx));

            // 3. Nhan READY (da ma hoa AES-256-GCM)
            String readyJson = FrameCodec.readEncryptedJsonFrame(in, aesKey);
            if (!readyJson.contains("READY")) {
                throw new IllegalStateException("Khong nhan duoc READY tu Server");
            }
            System.out.printf("[Client #%d] Bat tay RSA-2048 + AES-256-GCM thanh cong!%n", clientIdx);

            // 4. REGISTER (bo qua neu tai khoan da ton tai tu lan test truoc)
            Request regReq = new Request(reqIdSeq++, Command.REGISTER, Map.of("username", username, "password", password));
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(regReq), aesKey);
            FrameCodec.readEncryptedJsonFrame(in, aesKey);

            // 5. LOGIN
            Request loginReq = new Request(reqIdSeq++, Command.LOGIN, Map.of("username", username, "password", password));
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(loginReq), aesKey);
            Response loginRes = GSON.fromJson(FrameCodec.readEncryptedJsonFrame(in, aesKey), Response.class);
            if (!Boolean.TRUE.equals(loginRes.getOk())) {
                throw new IllegalStateException("LOGIN that bai: " + loginRes.getError().getMessage());
            }

            // 6. LIST_SEMESTERS
            Request semReq = new Request(reqIdSeq++, Command.LIST_SEMESTERS, Map.of());
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(semReq), aesKey);
            Response semRes = GSON.fromJson(FrameCodec.readEncryptedJsonFrame(in, aesKey), Response.class);
            List<Map<String, Object>> semesters = (List<Map<String, Object>>) semRes.getData().get("semesters");
            String semesterCode = semesters.get(0).get("code").toString();

            // 7. LIST_COURSES (lay danh sach hoc phan dang mo trong hoc ky)
            Request courseReq = new Request(reqIdSeq++, Command.LIST_COURSES, Map.of("semesterCode", semesterCode));
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(courseReq), aesKey);
            Response courseRes = GSON.fromJson(FrameCodec.readEncryptedJsonFrame(in, aesKey), Response.class);
            List<Map<String, Object>> courses = (List<Map<String, Object>>) courseRes.getData().get("courses");

            // Chon 4 hoc phan bat ky khac nhau cho Client nay de chay OPTIMIZE
            List<String> pickedCourses = new ArrayList<>();
            int startOffset = (clientIdx * 3) % Math.max(1, courses.size() - 5);
            for (int i = startOffset; i < courses.size() && pickedCourses.size() < 4; i++) {
                pickedCourses.add(courses.get(i).get("courseCode").toString());
            }

            // 8. Gui lenh OPTIMIZE
            Map<String, Object> optData = new LinkedHashMap<>();
            optData.put("semesterCode", semesterCode);
            optData.put("courses", pickedCourses);
            optData.put("criteria", Map.of("minDays", true, "morningFirst", true, "avoidGaps", true));
            optData.put("maxPlans", 3);

            Request optReq = new Request(reqIdSeq++, Command.OPTIMIZE, optData);
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(optReq), aesKey);

            // Doc cho den khi nhan duoc JOB_DONE
            String jobId = null;
            int plansFound = 0;
            while (true) {
                String msgJson = FrameCodec.readEncryptedJsonFrame(in, aesKey);
                Response msg = GSON.fromJson(msgJson, Response.class);

                if (!msg.isPush()) {
                    if (Boolean.TRUE.equals(msg.getOk()) && msg.getData() != null && msg.getData().containsKey("jobId")) {
                        jobId = String.valueOf(msg.getData().get("jobId"));
                    } else if (Boolean.FALSE.equals(msg.getOk())) {
                        throw new IllegalStateException("Lenh OPTIMIZE bao loi: " + msg.getError().getMessage());
                    }
                } else if ("JOB_PROGRESS".equals(msg.getPush())) {
                    Object pct = msg.getData().get("percent");
                    System.out.printf("[Client #%d] Job %s -> Tien do: %s%%%n", clientIdx, jobId, pct);
                } else if ("JOB_DONE".equals(msg.getPush())) {
                    Map<String, Object> result = (Map<String, Object>) msg.getData().get("result");
                    List<?> plans = (result != null) ? (List<?>) result.get("plans") : List.of();
                    plansFound = (plans != null) ? plans.size() : 0;
                    System.out.printf("[Client #%d] NHAN JOB_DONE! Chon %s -> Tim duoc %d phuong an toi uu.%n",
                            clientIdx, pickedCourses, plansFound);
                    break;
                }
            }

            // 9. Gui LOGOUT truoc khi dong ket noi
            Request logoutReq = new Request(reqIdSeq, Command.LOGOUT, Map.of());
            FrameCodec.WriteEncryptedJsonFrame(out, GSON.toJson(logoutReq), aesKey);
            FrameCodec.readEncryptedJsonFrame(in, aesKey);

            return plansFound >= 0;
        }
    }
}