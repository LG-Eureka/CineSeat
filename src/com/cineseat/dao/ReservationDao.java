package com.cineseat.dao;

import com.cineseat.db.DataAccessException;
import com.cineseat.db.Database;
import com.cineseat.db.SqlValues;
import com.cineseat.model.Reservation;
import com.cineseat.model.Screening;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 예매 생성 / 조회 / 취소. */
public class ReservationDao {

    /** 예매 시도 결과. */
    public sealed interface Outcome {
        /** 예매 성공. */
        record Success() implements Outcome {
        }

        /** 고른 좌석 중 일부를 그사이 다른 사람이 예매한 경우. */
        record SeatsTaken(Set<String> seats) implements Outcome {
        }
    }

    private static final String SELECT_RESERVED_SEATS = """
            SELECT seat FROM reserve
            WHERE movie_id = ? AND place_id = ? AND reserve_date = ? AND reserve_time = ?
              AND delete_fg = 'N'
            """;

    /** 해당 회차에 이미 팔린 좌석 번호들. */
    public Set<String> reservedSeats(Screening screening) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RESERVED_SEATS)) {
            bindScreening(ps, screening);
            try (ResultSet rs = ps.executeQuery()) {
                return collectSeats(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("좌석 정보를 불러오지 못했습니다.", e);
        }
    }

    /**
     * 좌석을 예매한다.
     *
     * <p>좌석 확인과 저장을 한 트랜잭션 안에서 처리해, 좌석 목록을 본 뒤 예매 버튼을 누르기까지의
     * 사이에 다른 사람이 같은 좌석을 가져가는 경우를 막는다.
     *
     * <p>SQLite 에는 {@code SELECT ... FOR UPDATE} 가 없다. 대신 {@code BEGIN IMMEDIATE} 로
     * 쓰기 잠금을 먼저 잡아 두면 확인과 저장 사이에 다른 연결이 끼어들지 못한다.
     */
    public Outcome reserve(String username, Screening screening, List<String> seats, int price) {
        try (Connection conn = Database.getConnection()) {
            begin(conn);
            try {
                Set<String> taken;
                try (PreparedStatement ps = conn.prepareStatement(SELECT_RESERVED_SEATS)) {
                    bindScreening(ps, screening);
                    try (ResultSet rs = ps.executeQuery()) {
                        taken = collectSeats(rs);
                    }
                }

                Set<String> conflicts = new LinkedHashSet<>(seats);
                conflicts.retainAll(taken);
                if (!conflicts.isEmpty()) {
                    end(conn, "ROLLBACK");
                    return new Outcome.SeatsTaken(conflicts);
                }

                String insert = """
                        INSERT INTO reserve
                            (username, movie_id, place_id, reserve_date, reserve_time, reserve_cnt, seat, price)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, username);
                    ps.setInt(2, screening.movieId());
                    ps.setInt(3, screening.placeId());
                    ps.setString(4, SqlValues.text(screening.showDate()));
                    ps.setString(5, SqlValues.text(screening.startTime()));
                    ps.setInt(6, seats.size());
                    ps.setString(7, String.join(",", seats));
                    ps.setInt(8, price);
                    ps.executeUpdate();
                }

                end(conn, "COMMIT");
                return new Outcome.Success();
            } catch (SQLException e) {
                endQuietly(conn, "ROLLBACK");
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("예매를 저장하지 못했습니다.", e);
        }
    }

    /**
     * 쓰기 잠금을 곧바로 잡는 트랜잭션을 시작한다.
     *
     * <p>JDBC 의 {@code setAutoCommit(false)} 는 잠금을 미루는 {@code BEGIN DEFERRED} 로
     * 시작하기 때문에, 확인과 저장 사이에 다른 연결이 끼어들 수 있다.
     */
    private void begin(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("BEGIN IMMEDIATE");
        }
    }

    private void end(Connection conn, String command) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(command);
        }
    }

    private void endQuietly(Connection conn, String command) {
        try {
            end(conn, command);
        } catch (SQLException ignored) {
            // 원래 발생한 예외를 가리지 않도록 무시한다.
        }
    }

    /** 취소되지 않은 내 예매 목록. 최근 상영일 순으로 돌려준다. */
    public List<Reservation> findByUsername(String username) {
        String sql = """
                SELECT r.id, r.username, r.movie_id, m.title AS movie_title,
                       r.place_id, p.name AS place_name,
                       r.reserve_date, r.reserve_time, r.reserve_cnt, r.seat, r.price, r.ins_dt
                FROM reserve r
                JOIN movie m ON m.id = r.movie_id
                JOIN place p ON p.id = r.place_id
                WHERE r.username = ? AND r.delete_fg = 'N'
                ORDER BY r.reserve_date DESC, r.reserve_time DESC, r.id DESC
                """;
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getInt("movie_id"),
                            rs.getString("movie_title"),
                            rs.getInt("place_id"),
                            rs.getString("place_name"),
                            SqlValues.date(rs, "reserve_date"),
                            SqlValues.time(rs, "reserve_time"),
                            rs.getInt("reserve_cnt"),
                            rs.getString("seat"),
                            rs.getInt("price"),
                            SqlValues.dateTime(rs, "ins_dt")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("예매 내역을 불러오지 못했습니다.", e);
        }
        return reservations;
    }

    /** 예매를 취소 처리한다. 남의 예매는 취소되지 않도록 아이디를 함께 확인한다. */
    public boolean cancel(int reservationId, String username) {
        String sql = "UPDATE reserve SET delete_fg = 'Y', del_dt = datetime('now', 'localtime') "
                + "WHERE id = ? AND username = ? AND delete_fg = 'N'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("예매를 취소하지 못했습니다.", e);
        }
    }

    private void bindScreening(PreparedStatement ps, Screening screening) throws SQLException {
        ps.setInt(1, screening.movieId());
        ps.setInt(2, screening.placeId());
        ps.setString(3, SqlValues.text(screening.showDate()));
        ps.setString(4, SqlValues.text(screening.startTime()));
    }

    /** 예매 한 건의 좌석은 "A1,A2" 처럼 저장되므로 펼쳐서 모은다. */
    private Set<String> collectSeats(ResultSet rs) throws SQLException {
        Set<String> seats = new LinkedHashSet<>();
        while (rs.next()) {
            for (String seat : rs.getString("seat").split(",")) {
                seats.add(seat.trim());
            }
        }
        return seats;
    }

}
