package com.cineseat.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 접속 정보를 읽어 JDBC 연결을 만들어 준다.
 *
 * <p>접속 정보는 {@code config/config.properties} 에서 읽고, 같은 이름의 환경 변수가
 * 있으면 그 값이 우선한다. 자격 증명은 저장소에 커밋하지 않는다.
 */
public final class Database {

    private static final Path CONFIG_FILE = Path.of("config", "config.properties");

    private static final String url;
    private static final String user;
    private static final String password;

    static {
        Properties props = loadProperties();
        url = resolve(props, "db.url", "CINESEAT_DB_URL", "jdbc:mysql://localhost:3306/moviedb");
        user = resolve(props, "db.username", "CINESEAT_DB_USERNAME", "root");
        password = resolve(props, "db.password", "CINESEAT_DB_PASSWORD", "");
    }

    private Database() {
    }

    /** 새 연결을 연다. 호출한 쪽에서 try-with-resources 로 닫아야 한다. */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException("데이터베이스에 연결하지 못했습니다: " + url, e);
        }
    }

    /** 시작 시점에 접속 가능 여부를 확인한다. */
    public static void verifyConnection() {
        try (Connection ignored = getConnection()) {
            // 연결에 성공하면 그대로 닫는다.
        } catch (SQLException e) {
            throw new DataAccessException("데이터베이스 연결 확인에 실패했습니다.", e);
        }
    }

    public static String describeTarget() {
        return user + "@" + url;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException e) {
                throw new DataAccessException("설정 파일을 읽지 못했습니다: " + CONFIG_FILE.toAbsolutePath(), e);
            }
        }
        return props;
    }

    private static String resolve(Properties props, String key, String envKey, String fallback) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return props.getProperty(key, fallback);
    }
}
