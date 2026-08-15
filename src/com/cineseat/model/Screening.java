package com.cineseat.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 특정 날짜에 특정 상영관에서 열리는 한 회차.
 *
 * <p>{@code screen} 테이블은 상영 기간(start_date ~ end_date)을 갖고 있으므로,
 * {@code showDate} 는 사용자가 고른 날짜로 채워진다.
 */
public record Screening(
        int screenId,
        int movieId,
        int placeId,
        String placeName,
        LocalDate showDate,
        LocalTime startTime,
        int totalSeats,
        int reservedSeats) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public int remainingSeats() {
        return totalSeats - reservedSeats;
    }

    public boolean soldOut() {
        return remainingSeats() <= 0;
    }

    public String startTimeLabel() {
        return startTime.format(TIME_FORMAT);
    }
}
