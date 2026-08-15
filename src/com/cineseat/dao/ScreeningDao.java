package com.cineseat.dao;

import com.cineseat.db.DataAccessException;
import com.cineseat.db.Database;
import com.cineseat.model.Screening;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 상영 일정 조회. */
public class ScreeningDao {

    /**
     * 예매 가능한 모든 날짜. 상영 기간(start_date ~ end_date)을 하루 단위로 펼쳐서 돌려주므로
     * 달력 화면이 날짜를 직접 하드코딩할 필요가 없다.
     */
    public Set<LocalDate> availableDates() {
        String sql = "SELECT start_date, end_date FROM screen";
        Set<LocalDate> dates = new HashSet<>();
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end = rs.getDate("end_date").toLocalDate();
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    dates.add(d);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("상영 일정을 불러오지 못했습니다.", e);
        }
        return dates;
    }

    /** 특정 영화의 특정 날짜 회차 목록. 이미 예매된 좌석 수까지 함께 계산한다. */
    public List<Screening> findByMovieAndDate(int movieId, LocalDate date) {
        String sql = """
                SELECT s.id, s.movie_id, s.place_id, p.name AS place_name,
                       s.start_time, s.total_seats,
                       COALESCE((SELECT SUM(r.reserve_cnt)
                                 FROM reserve r
                                 WHERE r.movie_id = s.movie_id
                                   AND r.place_id = s.place_id
                                   AND r.reserve_date = ?
                                   AND r.reserve_time = s.start_time
                                   AND r.delete_fg = 'N'), 0) AS reserved_seats
                FROM screen s
                JOIN place p ON p.id = s.place_id
                WHERE s.movie_id = ? AND ? BETWEEN s.start_date AND s.end_date
                ORDER BY s.start_time, p.name
                """;
        List<Screening> screenings = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setInt(2, movieId);
            ps.setDate(3, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    screenings.add(new Screening(
                            rs.getInt("id"),
                            rs.getInt("movie_id"),
                            rs.getInt("place_id"),
                            rs.getString("place_name"),
                            date,
                            rs.getTime("start_time").toLocalTime(),
                            rs.getInt("total_seats"),
                            rs.getInt("reserved_seats")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("상영 회차를 불러오지 못했습니다.", e);
        }
        return screenings;
    }
}
