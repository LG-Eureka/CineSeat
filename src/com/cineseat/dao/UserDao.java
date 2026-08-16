package com.cineseat.dao;

import com.cineseat.db.DataAccessException;
import com.cineseat.db.Database;
import com.cineseat.db.SqlErrors;
import com.cineseat.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** 회원 가입 / 로그인 / 회원 조회. */
public class UserDao {

    /** 아이디가 이미 쓰이고 있으면 false 를 돌려준다. */
    public boolean register(String username, String password, int age) {
        String sql = "INSERT INTO users (username, password, age) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt(3, age);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (SqlErrors.isUniqueViolation(e)) {
                return false;
            }
            throw new DataAccessException("회원 가입에 실패했습니다.", e);
        }
    }

    public boolean exists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("회원 조회에 실패했습니다.", e);
        }
    }

    /** 아이디와 비밀번호가 맞으면 회원 정보를, 아니면 빈 값을 돌려준다. */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, age FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("로그인 처리에 실패했습니다.", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, age FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("회원 정보를 불러오지 못했습니다.", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getInt("age"));
    }
}
