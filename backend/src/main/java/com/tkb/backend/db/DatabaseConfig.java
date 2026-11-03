package com.tkb.backend.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * B1.4 - DatabaseConfig: Quan ly Connection Pool bang HikariCP cho MySQL (chay tren Docker).
 *
 * Dac diem:
 *   - Doc cau hinh tu bien moi truong (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS)
 *     hoac System Properties (-DDB_HOST=...), khong ghi cung mat khau vao logic nghiep vu.
 *   - Gia tri mac dinh khop san voi docker-compose.yml va script Python (127.0.0.1:3306/tkb_db).
 *   - Bat san cache PreparedStatement cua MySQL JDBC Driver de tang toc do truy van DAO.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/db/DatabaseConfig.java
 */
public final class DatabaseConfig {

    private static volatile HikariDataSource dataSource;

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
     * Luon goi trong khoi try-with-resources:
     *   try (Connection conn = DatabaseConfig.getConnection()) { ... }
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
        String host = getEnvOrDefault("DB_HOST", "127.0.0.1");
        String port = getEnvOrDefault("DB_PORT", "3306");
        String dbName = getEnvOrDefault("DB_NAME", "tkb_db");
        String user = getEnvOrDefault("DB_USER", "root");
        String pass = getEnvOrDefault("DB_PASS", "root");
        int maxPoolSize = Integer.parseInt(getEnvOrDefault("DB_POOL_SIZE", "15"));

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false",
                host, port, dbName
        );

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

        return new HikariDataSource(config);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }
}