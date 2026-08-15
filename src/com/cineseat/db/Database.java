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
 * 있으면 그 값이 우선한다. 자격 증명은 저장소에 커밋하지 않으며 기본값도 두지 않는다.
 * 설정이 없으면 root 같은 계정으로 몰래 접속을 시도하는 대신 무엇을 채워야 하는지 알려 준다.
 */
public final class Database {

    private static final Path CONFIG_FILE = Path.of("config", "config.properties");

    private static Settings settings;

    private record Settings(String url, String user, String password) {
    }

    private Database() {
    }

    /** 새 연결을 연다. 호출한 쪽에서 try-with-resources 로 닫아야 한다. */
    public static Connection getConnection() {
        Settings current = settings();
        try {
            return DriverManager.getConnection(current.url(), current.user(), current.password());
        } catch (SQLException e) {
            throw new DataAccessException("데이터베이스에 연결하지 못했습니다: " + current.url(), e);
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

    /** 연결 실패 안내에 쓰는 접속 대상. 비밀번호는 포함하지 않는다. */
    public static String describeTarget() {
        try {
            Settings current = settings();
            return current.user() + "@" + current.url();
        } catch (DataAccessException e) {
            return "(설정 없음)";
        }
    }

    /**
     * 설정을 처음 필요할 때 읽는다.
     *
     * <p>클래스 초기화 중에 예외를 던지면 {@code ExceptionInInitializerError} 로 바뀌어
     * 화면에서 원인을 안내할 수 없기 때문에, 읽는 시점을 호출 시점으로 미뤘다.
     */
    private static synchronized Settings settings() {
        if (settings == null) {
            Properties props = loadProperties();
            settings = new Settings(
                    require(props, "db.url", "CINESEAT_DB_URL"),
                    require(props, "db.username", "CINESEAT_DB_USERNAME"),
                    // 비밀번호는 비어 있을 수 있으므로 값이 없으면 빈 문자열로 둔다.
                    resolve(props, "db.password", "CINESEAT_DB_PASSWORD", ""));
        }
        return settings;
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException e) {
                throw new DataAccessException(
                        "설정 파일을 읽지 못했습니다: " + CONFIG_FILE.toAbsolutePath(), e);
            }
        }
        return props;
    }

    private static String require(Properties props, String key, String envKey) {
        String value = resolve(props, key, envKey, null);
        if (value == null || value.isBlank()) {
            throw new DataAccessException(
                    key + " 값이 없습니다. config/config.properties.example 을 "
                            + "config/config.properties 로 복사해 채우거나 환경 변수 "
                            + envKey + " 를 설정해 주세요.");
        }
        return value;
    }

    private static String resolve(Properties props, String key, String envKey, String fallback) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return props.getProperty(key, fallback);
    }
}
