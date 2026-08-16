package com.cineseat.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * SQLite 데이터베이스 연결을 만들어 준다.
 *
 * <p>파일 하나로 동작하므로 따로 서버를 띄우거나 계정을 만들 필요가 없다. 기본값은
 * {@code db/cineseat.db} 이고, 다른 경로를 쓰고 싶으면 {@code config/config.properties} 의
 * {@code db.url} 이나 환경 변수 {@code CINESEAT_DB_URL} 로 바꿀 수 있다.
 */
public final class Database {

    /** 설정이 없을 때 쓰는 데이터베이스 파일. */
    public static final String DEFAULT_URL = "jdbc:sqlite:db/cineseat.db";

    private static final Path CONFIG_FILE = Path.of("config", "config.properties");

    private static String url;

    private Database() {
    }

    /** 새 연결을 연다. 호출한 쪽에서 try-with-resources 로 닫아야 한다. */
    public static Connection getConnection() {
        String target = url();
        try {
            Connection conn = DriverManager.getConnection(target);
            applyPragmas(conn);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException("데이터베이스를 열지 못했습니다: " + target, e);
        }
    }

    /**
     * 연결마다 적용해야 하는 설정.
     *
     * <p>SQLite 는 외래키 검사가 기본으로 꺼져 있어 연결할 때마다 켜 줘야 하고,
     * 다른 곳에서 쓰고 있을 때 곧바로 실패하지 않도록 대기 시간을 준다.
     */
    private static void applyPragmas(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
    }

    /** 테이블이 준비되어 있는지 확인한다. */
    public static void verifySchema() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT 1 FROM movie LIMIT 1").close();
        } catch (SQLException e) {
            throw new DataAccessException("""
                    테이블을 찾지 못했습니다. 아래 명령으로 데이터베이스를 만들어 주세요.

                        sqlite3 %s < db/schema.sql

                    ./run.sh 로 실행하면 없을 때 자동으로 만들어 줍니다."""
                    .formatted(filePath()), e);
        }
    }

    /** 연결 실패 안내에 쓰는 데이터베이스 파일 경로. */
    public static String filePath() {
        String target = url();
        return target.startsWith("jdbc:sqlite:") ? target.substring("jdbc:sqlite:".length()) : target;
    }

    private static synchronized String url() {
        if (url == null) {
            Properties props = loadProperties();
            String fromEnv = System.getenv("CINESEAT_DB_URL");
            url = fromEnv != null && !fromEnv.isBlank()
                    ? fromEnv
                    : props.getProperty("db.url", DEFAULT_URL);
        }
        return url;
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
}
