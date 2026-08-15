package com.cineseat.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 예매 내역 한 건. 영화 제목과 상영관 이름은 조회 시 조인해서 채운다. */
public record Reservation(
        int id,
        String username,
        int movieId,
        String movieTitle,
        int placeId,
        String placeName,
        LocalDate reserveDate,
        LocalTime reserveTime,
        int seatCount,
        String seats,
        int price,
        LocalDateTime createdAt) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public List<String> seatList() {
        return List.of(seats.split(","));
    }

    public String scheduleLabel() {
        return reserveDate.format(DATE_FORMAT) + "  " + reserveTime.format(TIME_FORMAT);
    }

    public String createdAtLabel() {
        return createdAt == null ? "-" : createdAt.format(CREATED_FORMAT);
    }

    public String priceLabel() {
        return String.format("%,d원", price);
    }
}
