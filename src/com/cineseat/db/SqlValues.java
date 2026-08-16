package com.cineseat.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜와 시각을 데이터베이스 문자열로 바꾸고 되돌린다.
 *
 * <p>SQLite 에는 날짜/시각 전용 타입이 없어 문자열로 저장한다. 형식이 한 글자라도 어긋나면
 * 비교가 조용히 빗나가기 때문에, 변환을 이 클래스 한 곳에만 둔다.
 *
 * <p>특히 {@link LocalTime#toString()} 은 초가 0이면 {@code "13:00"} 처럼 초를 빼고 만들어서
 * 저장된 {@code "13:00:00"} 과 맞지 않는다. 그래서 항상 {@link #TIME_FORMAT} 을 거친다.
 */
public final class SqlValues {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SqlValues() {
    }

    // ---------------------------------------------------------------- 저장할 때

    public static String text(LocalDate date) {
        return date.toString();
    }

    public static String text(LocalTime time) {
        return time.format(TIME_FORMAT);
    }

    // ---------------------------------------------------------------- 읽을 때

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        return LocalDate.parse(rs.getString(column));
    }

    public static LocalTime time(ResultSet rs, String column) throws SQLException {
        return LocalTime.parse(rs.getString(column));
    }

    /** {@code ins_dt} 처럼 "yyyy-MM-dd HH:mm:ss" 로 저장된 값. 비어 있으면 null. */
    public static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), DATE_TIME_FORMAT);
    }
}
