package com.cineseat.dao;

import com.cineseat.db.DataAccessException;
import com.cineseat.db.Database;
import com.cineseat.db.SqlValues;
import com.cineseat.model.Movie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 영화 조회. */
public class MovieDao {

    /** 해당 날짜에 상영 일정이 잡혀 있는 영화 목록. */
    public List<Movie> findByDate(LocalDate date) {
        String sql = """
                SELECT DISTINCT m.id, m.title, m.price, m.age_limit, m.running_time
                FROM movie m
                JOIN screen s ON s.movie_id = m.id
                WHERE ? BETWEEN s.start_date AND s.end_date
                ORDER BY m.title
                """;
        List<Movie> movies = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SqlValues.text(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(new Movie(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("price"),
                            rs.getInt("age_limit"),
                            rs.getInt("running_time")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("영화 목록을 불러오지 못했습니다.", e);
        }
        return movies;
    }
}
