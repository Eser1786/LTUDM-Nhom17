package com.tkb.backend.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * B1.4 - DatabaseConfig: Quan ly Connection Pool bang HikariCP cho MySQL (chay tren Docker).
 *
 * Dac diem:
 *   - Tu dong doc bien moi truong tu System Properties (-DDB_...), System.getenv(),
 *     VA tu dong doc file .env o thu muc goc du an (khi chay bang nut Run tren VS Code / IntelliJ).
 *   - Ho tro ca hai bo ten bien:
 *       + DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
 *       + MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD
 *   - In ro dia chi host:port/database khi khoi dong de de kiem tra ket noi Docker.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/db/DatabaseConfig.java
 */
public final class DatabaseConfig {

    private static volatile HikariDataSource dataSource;
    private static final Map<String, String> DOTENV_MAP = loadDotEnvFile();

    private DatabaseConfig() {
        // Utility class
    }

    /**
     * Khoi tao (neu chua co) va tra ve HikariDataSource dung chung cho toan Server.
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Lay mot ket noi MySQL tu HikariCP Connection Pool.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Dong Connection Pool khi Server tat (shutdown hook).
     */
    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static HikariDataSource createDataSource() {
        String host = getConfig(List.of("DB_HOST", "MYSQL_HOST"), "127.0.0.1");
        String port = getConfig(List.of("DB_PORT", "MYSQL_PORT", "FORWARD_DB_PORT"), "3306");
        String dbName = getConfig(List.of("DB_NAME", "MYSQL_DATABASE"), "tkb_db");
        String user = getConfig(List.of("DB_USER", "MYSQL_USER"), "root");
        String pass = getConfig(List.of("DB_PASS", "DB_PASSWORD", "MYSQL_PASSWORD", "MYSQL_ROOT_PASSWORD"), "root");
        int maxPoolSize = Integer.parseInt(getConfig(List.of("DB_POOL_SIZE"), "15"));

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false",
                host, port, dbName
        );

        System.out.printf("[DatabaseConfig] Dang ket noi MySQL tai %s:%s/%s (user='%s')...%n", host, port, dbName, user);

        HikariConfig config = new HikariConfig();
        config.setPoolName("TKB-HikariPool");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000); // 10 giay
        config.setIdleTimeout(300_000);      // 5 phut
        config.setMaxLifetime(1_800_000);    // 30 phut

        // Toi uu hoa PreparedStatement cho MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            return new HikariDataSource(config);
        } catch (RuntimeException e) {
            System.err.println("==========================================================================");
            System.err.printf("[DatabaseConfig] LOI KET NOI MYSQL TAI %s:%s/%s (user='%s')%n", host, port, dbName, user);
            System.err.println("Nguyen nhan thuong gap:");
            System.err.println("  1. Container MySQL trong Docker chua chay -> Hay chay lenh: docker compose up -d");
            System.err.println("  2. Cong MySQL tren may host khong phai 3306 -> Kiem tra bang lenh: docker ps");
            System.err.println("     va dat DB_PORT=<cong> trong file .env hoac bien moi truong.");
            System.err.println("==========================================================================");
            throw e;
        }
    }

    /**
     * Tim gia tri cau hinh theo thu tu uu tien:
     *   1. JVM System Properties (-DKEY=val)
     *   2. OS Environment Variables (export KEY=val)
     *   3. File .env tai thu muc goc du an
     *   4. Gia tri mac dinh (defaultValue)
     */
    private static String getConfig(List<String> keys, String defaultValue) {
        for (String key : keys) {
            String prop = System.getProperty(key);
            if (prop != null && !prop.isBlank()) {
                return prop.trim();
            }
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                return env.trim();
            }
            String dotenvVal = DOTENV_MAP.get(key);
            if (dotenvVal != null && !dotenvVal.isBlank()) {
                return dotenvVal.trim();
            }
        }
        return defaultValue;
    }

    /**
     * Tu dong tim va doc file .env o thu muc hien tai (./.env) hoac thu muc cha (../.env).
     */
    private static Map<String, String> loadDotEnvFile() {
        Map<String, String> map = new HashMap<>();
        List<Path> candidates = List.of(Path.of(".env"), Path.of("..", ".env"));
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String rawLine : lines) {
                        String line = rawLine.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String k = line.substring(0, eqIdx).trim();
                            String v = line.substring(eqIdx + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            map.putIfAbsent(k, v);
                        }
                    }
                    break;
                } catch (IOException ignored) {
                }
            }
        }
        return map;
    }
}