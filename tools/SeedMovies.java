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
 * <p>두 가지 방식을 지원한다.
 *
 * <pre>
 *   java tools/SeedMovies.java                    키 없이 공개 박스오피스 페이지에서 가져온다
 *   java tools/SeedMovies.java --key=발급받은키    오픈 API 로 가져온다 (상영시간·관람등급까지)
 *   java tools/SeedMovies.java --date=20260815 --count=8 --out=db/seed-movies.sql
 * </pre>
 *
 * <p>키가 있으면 상영시간과 관람등급까지 정확히 받아 오므로 그쪽을 권한다.
 * 키는 https://www.kobis.or.kr/kobisopenapi/ 에서 무료로 발급받을 수 있고,
 * {@code KOBIS_API_KEY} 환경 변수로도 넘길 수 있다.
 *
 * <p>키가 없으면 공개 박스오피스 페이지에서 실제 상영작 제목을 가져오되, 그 페이지가 주지 않는
 * 상영시간과 관람등급은 기본값으로 채운다. 화면에서 직접 만든 값임을 알 수 있도록 실행할 때
 * 어떤 값이 기본값인지 함께 출력한다.
 */
public class SeedMovies {

    private static final String BOX_OFFICE_URL =
            "https://www.kobis.or.kr/kobisopenapi/webservice/rest/boxoffice/searchDailyBoxOfficeList.xml";
    private static final String MOVIE_INFO_URL =
            "https://www.kobis.or.kr/kobisopenapi/webservice/rest/movie/searchMovieInfo.xml";

    /** 키 없이 접근할 수 있는 공개 박스오피스 페이지. */
    private static final String BOX_OFFICE_PAGE =
            "https://www.kobis.or.kr/kobis/business/stat/boxs/findDailyBoxOfficeList.do";

    /** 표의 제목 셀: mstView('movie','영화코드') ... title="영화 제목" */
    private static final java.util.regex.Pattern BOX_OFFICE_ROW = java.util.regex.Pattern.compile(
            "mstView\\('movie','(\\d+)'\\);return false;\"\\s*title=\"([^\"]*)\"");

    /** 공개 페이지가 상영시간을 주지 않을 때 쓰는 값. */
    private static final int DEFAULT_RUNNING_TIME = 110;

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

        // 당일 집계는 아직 없으므로 기본값은 어제로 둔다.
        String date = argValue(args, "--date", LocalDate.now().minusDays(1).format(TARGET_DATE));
        int count = Integer.parseInt(argValue(args, "--count", "6"));
        Path out = Path.of(argValue(args, "--out", "db/seed-movies.sql"));

        boolean useApi = key != null && !key.isBlank();
        System.out.println(useApi
                ? "KOBIS 오픈 API 로 " + date + " 기준 상영작을 가져옵니다."
                : """
                        키가 없어 공개 박스오피스 페이지에서 가져옵니다.
                        제목은 실제 상영작이지만 상영시간과 관람등급은 기본값으로 채웁니다.
                        정확한 값을 원하면 https://www.kobis.or.kr/kobisopenapi/ 에서
                        무료 키를 발급받아 --key=... 로 넘겨 주세요.
                        """);

        SeedMovies tool = new SeedMovies(key);
        List<Movie> movies;
        try {
            movies = useApi ? tool.fetchMovies(date, count) : tool.fetchFromWeb(date, count);
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

    // ---------------------------------------------------------------- 키 없이 가져오기

    /** 공개 박스오피스 페이지에서 순위와 제목을 뽑아낸다. */
    List<Movie> fetchFromWeb(String targetDate, int count) throws Exception {
        String day = LocalDate.parse(targetDate, TARGET_DATE).toString();
        String form = "loadEnd=0&searchType=search"
                + "&sSearchFrom=" + day + "&sSearchTo=" + day
                + "&sMultiMovieYn=&sRepNationCd=&sWideAreaCd=";

        HttpRequest request = HttpRequest.newBuilder(URI.create(BOX_OFFICE_PAGE))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (CineSeat seed tool)")
                .header("Referer", BOX_OFFICE_PAGE)
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("KOBIS 페이지 응답 오류: HTTP " + response.statusCode());
        }

        List<Movie> movies = new ArrayList<>();
        java.util.regex.Matcher matcher = BOX_OFFICE_ROW.matcher(response.body());
        while (matcher.find() && movies.size() < count) {
            String code = matcher.group(1);
            String title = unescapeHtml(matcher.group(2));
            if (title.isBlank()) {
                continue;
            }
            movies.add(new Movie(code, title, DEFAULT_RUNNING_TIME, 0, price(DEFAULT_RUNNING_TIME)));
            System.out.printf("  %d. %s  (상영시간 %d분 · 전체관람가 — 둘 다 기본값)%n",
                    movies.size(), title, DEFAULT_RUNNING_TIME);
        }

        if (movies.isEmpty()) {
            throw new IllegalStateException("""
                    박스오피스 표를 읽지 못했습니다.
                    KOBIS 페이지 구조가 바뀌었을 수 있습니다. --key=... 로 오픈 API 를 쓰거나
                    --date=YYYYMMDD 로 다른 날짜를 지정해 보세요.""");
        }
        return movies;
    }

    /** 제목에 섞여 들어오는 최소한의 HTML 엔티티만 되돌린다. */
    static String unescapeHtml(String value) {
        return value.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .trim();
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
