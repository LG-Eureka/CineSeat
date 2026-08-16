import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * KOBIS(영화진흥위원회) 오픈 API 에서 실제 상영 중인 영화를 받아 시드 SQL 을 만든다.
 *
 * <p>애플리케이션은 이 파일을 쓰지 않는다. 데이터를 한 번 받아 SQL 로 떨궈 두는 도구이므로
 * 앱 자체에는 네트워크나 JSON 의존성이 생기지 않는다.
 *
 * <pre>
 *   java tools/SeedMovies.java --key=발급받은키
 *   java tools/SeedMovies.java --key=... --date=20260815 --count=8 --out=db/seed-movies.sql
 * </pre>
 *
 * <p>키는 https://www.kobis.or.kr/kobisopenapi/ 에서 무료로 발급받을 수 있고,
 * {@code KOBIS_API_KEY} 환경 변수로도 넘길 수 있다.
 */
public class SeedMovies {

    private static final String BOX_OFFICE_URL =
            "https://www.kobis.or.kr/kobisopenapi/webservice/rest/boxoffice/searchDailyBoxOfficeList.xml";
    private static final String MOVIE_INFO_URL =
            "https://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.xml";

    private static final DateTimeFormatter TARGET_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 상영관별 회차 시간표. 영화 순서에 따라 돌아가며 배정한다. */
    private static final String[][] TIME_TABLES = {
            {"10:30:00", "13:20:00", "16:10:00", "19:00:00", "21:50:00"},
            {"11:00:00", "14:00:00", "17:00:00", "20:00:00"},
            {"12:30:00", "15:30:00", "18:30:00"},
    };

    private static final int[] PLACE_SEATS = {40, 32, 40};

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String apiKey;

    record Movie(String code, String title, int runningTime, int ageLimit, int price) {
    }

    public static void main(String[] args) throws Exception {
        String key = argValue(args, "--key", System.getenv("KOBIS_API_KEY"));
        if (key == null || key.isBlank()) {
            System.err.println("""
                    KOBIS API 키가 필요합니다.

                      java tools/SeedMovies.java --key=발급받은키

                    키는 https://www.kobis.or.kr/kobisopenapi/ 에서 무료로 발급받을 수 있습니다.
                    KOBIS_API_KEY 환경 변수로 넘겨도 됩니다.""");
            System.exit(1);
        }

        // 당일 집계는 아직 없으므로 기본값은 어제로 둔다.
        String date = argValue(args, "--date", LocalDate.now().minusDays(1).format(TARGET_DATE));
        int count = Integer.parseInt(argValue(args, "--count", "6"));
        Path out = Path.of(argValue(args, "--out", "db/seed-movies.sql"));

        SeedMovies tool = new SeedMovies(key);
        List<Movie> movies;
        try {
            movies = tool.fetchMovies(date, count);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        if (movies.isEmpty()) {
            System.err.println("가져온 영화가 없습니다. 날짜(--date=" + date + ")를 확인해 주세요.");
            System.exit(1);
        }

        String sql = buildSql(movies, date);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, sql, StandardCharsets.UTF_8);

        System.out.println();
        System.out.println(out + " 생성 완료 (영화 " + movies.size() + "편)");
        System.out.println("적용:  mysql -u root -p < " + out);
    }

    SeedMovies(String apiKey) {
        this.apiKey = apiKey;
    }

    // ---------------------------------------------------------------- 조회

    List<Movie> fetchMovies(String targetDate, int count) throws Exception {
        Document boxOffice = get(BOX_OFFICE_URL + "?key=" + apiKey + "&targetDt=" + targetDate);
        NodeList entries = boxOffice.getElementsByTagName("dailyBoxOfficeList");

        List<Movie> movies = new ArrayList<>();
        for (int i = 0; i < entries.getLength() && movies.size() < count; i++) {
            Element entry = (Element) entries.item(i);
            String code = text(entry, "movieCd");
            String title = text(entry, "movieNm");
            if (code.isEmpty() || title.isEmpty()) {
                continue;
            }

            Document detail = get(MOVIE_INFO_URL + "?key=" + apiKey + "&movieCd=" + code);
            NodeList infoNodes = detail.getElementsByTagName("movieInfo");
            int runningTime = 0;
            String grade = "";
            if (infoNodes.getLength() > 0) {
                Element info = (Element) infoNodes.item(0);
                runningTime = parseInt(text(info, "showTm"));
                grade = text(info, "watchGradeNm");
            }
            if (runningTime <= 0) {
                // 상영시간이 비어 있는 경우가 있어 평균값으로 채운다.
                runningTime = 110;
            }

            movies.add(new Movie(code, title, runningTime, ageLimit(grade), price(runningTime)));
            System.out.printf("  %d. %s (%d분, %s)%n",
                    movies.size(), title, runningTime, grade.isEmpty() ? "등급 미상" : grade);
        }
        return movies;
    }

    private Document get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<byte[]> response =
                http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("KOBIS 응답 오류: HTTP " + response.statusCode());
        }
        return parse(response.body());
    }

    /** 응답 XML 을 읽고, 오류 응답이면 예외로 바꾼다. */
    static Document parse(byte[] body) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(body));

        // 키가 잘못되면 HTTP 200 과 함께 faultResult 가 돌아온다.
        NodeList faults = document.getElementsByTagName("faultInfo");
        if (faults.getLength() > 0) {
            Element fault = (Element) faults.item(0);
            throw new IllegalStateException(
                    "KOBIS 오류 " + text(fault, "errorCode") + ": " + text(fault, "message"));
        }
        return document;
    }

    // ---------------------------------------------------------------- 변환

    /** 관람 등급 문구를 나이 제한 숫자로 바꾼다. */
    static int ageLimit(String watchGradeName) {
        if (watchGradeName.contains("전체")) {
            return 0;
        }
        if (watchGradeName.contains("12")) {
            return 12;
        }
        if (watchGradeName.contains("15")) {
            return 15;
        }
        if (watchGradeName.contains("청소년관람불가") || watchGradeName.contains("제한")) {
            return 19;
        }
        return 0;
    }

    /** KOBIS 는 가격을 주지 않으므로 상영시간을 기준으로 정한다. */
    static int price(int runningTime) {
        return runningTime >= 150 ? 15000 : 12000;
    }

    // ---------------------------------------------------------------- SQL 생성

    static String buildSql(List<Movie> movies, String targetDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("""
                -- KOBIS 일별 박스오피스에서 받아 생성한 시드 데이터입니다.
                -- tools/SeedMovies.java 가 만들어 낸 파일이므로 직접 고치지 마세요.
                --
                -- 주의: 영화·상영일정·예매 데이터를 모두 지우고 새로 넣습니다.
                --       기존 예매 내역을 남기려면 먼저 백업해 주세요.
                """);
        sql.append("-- 기준일: ").append(targetDate).append('\n');
        sql.append("-- 생성일: ").append(LocalDate.now()).append("\n\n");

        sql.append("USE moviedb;\n\n");
        sql.append("DELETE FROM reserve;\n");
        sql.append("DELETE FROM screen;\n");
        sql.append("DELETE FROM movie;\n");
        sql.append("ALTER TABLE movie AUTO_INCREMENT = 1;\n");
        sql.append("ALTER TABLE screen AUTO_INCREMENT = 1;\n\n");

        sql.append("INSERT INTO movie (id, title, price, age_limit, running_time) VALUES\n");
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            sql.append("    (").append(i + 1).append(", '").append(escape(movie.title()))
                    .append("', ").append(movie.price())
                    .append(", ").append(movie.ageLimit())
                    .append(", ").append(movie.runningTime()).append(')')
                    .append(i == movies.size() - 1 ? ";\n\n" : ",\n");
        }

        sql.append("INSERT INTO screen (movie_id, place_id, start_date, end_date, start_time, total_seats) VALUES\n");
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < movies.size(); i++) {
            int movieId = i + 1;
            // 순위가 높은 영화일수록 더 많은 상영관에 건다.
            int placeCount = i < 2 ? 3 : i < 4 ? 2 : 1;
            int runDays = i < 2 ? 20 : i < 4 ? 14 : 10;
            for (int place = 1; place <= placeCount; place++) {
                String[] times = TIME_TABLES[(i + place - 1) % TIME_TABLES.length];
                for (String time : times) {
                    rows.add("    (%d, %d, CURDATE(), CURDATE() + INTERVAL %d DAY, '%s', %d)"
                            .formatted(movieId, place, runDays, time, PLACE_SEATS[place - 1]));
                }
            }
        }
        sql.append(String.join(",\n", rows)).append(";\n");
        return sql.toString();
    }

    static String escape(String value) {
        return value.replace("'", "''");
    }

    // ---------------------------------------------------------------- 도우미

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node.getTextContent() == null ? "" : node.getTextContent().trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String argValue(String[] args, String name, String fallback) {
        for (String arg : args) {
            if (arg.startsWith(name + "=")) {
                return arg.substring(name.length() + 1);
            }
        }
        return fallback;
    }
}
