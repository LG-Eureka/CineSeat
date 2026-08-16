import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL 파일을 실행해 데이터베이스를 만든다.
 *
 * <pre>
 *   java -cp "lib/*:tools" InitDb                       db/schema.sql 로 db/cineseat.db 생성
 *   java -cp "lib/*:tools" InitDb db/seed-movies.sql    다른 SQL 파일 적용
 * </pre>
 *
 * <p>sqlite3 명령이 없는 환경에서도 데이터베이스를 만들 수 있도록 두었다.
 * {@code run.sh} 가 데이터베이스 파일이 없을 때 자동으로 호출한다.
 */
public class InitDb {

    private static final String DEFAULT_DB = "db/cineseat.db";
    private static final String DEFAULT_SCRIPT = "db/schema.sql";

    public static void main(String[] args) throws Exception {
        Path script = Path.of(args.length > 0 ? args[0] : DEFAULT_SCRIPT);
        String dbPath = args.length > 1 ? args[1] : System.getenv()
                .getOrDefault("CINESEAT_DB_FILE", DEFAULT_DB);

        if (!Files.exists(script)) {
            System.err.println("SQL 파일을 찾지 못했습니다: " + script.toAbsolutePath());
            System.exit(1);
        }
        Files.createDirectories(Path.of(dbPath).toAbsolutePath().getParent());

        String sql = Files.readString(script, StandardCharsets.UTF_8);
        List<String> statements = split(sql);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement()) {
            for (String statement : statements) {
                try {
                    st.execute(statement);
                } catch (SQLException e) {
                    throw new IllegalStateException(
                            "실행하지 못한 구문: " + summarize(statement) + "\n  " + e.getMessage(), e);
                }
            }
        }

        System.out.println(dbPath + " 준비 완료 (" + script + ", 구문 " + statements.size() + "개)");
    }

    /**
     * 세미콜론 기준으로 구문을 나눈다.
     *
     * <p>주석과 문자열 안의 세미콜론은 구분자로 보지 않는다. 예시 데이터에 한글이 들어 있고
     * 작은따옴표도 쓰이기 때문에 단순 split 으로는 잘못 잘린다.
     */
    static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean inComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                    current.append(c);
                }
                continue;
            }
            if (!inString && c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                inComment = true;
                i++;
                continue;
            }
            if (c == '\'') {
                // '' 는 문자열 안의 작은따옴표이므로 상태를 바꾸지 않는다.
                if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                inString = !inString;
            }
            if (c == ';' && !inString) {
                addIfNotBlank(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    private static String summarize(String statement) {
        String flat = statement.replaceAll("\\s+", " ").trim();
        return flat.length() <= 70 ? flat : flat.substring(0, 70) + "...";
    }
}
