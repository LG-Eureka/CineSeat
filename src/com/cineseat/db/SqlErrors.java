package com.cineseat.db;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * SQL 예외의 원인을 판별한다.
 *
 * <p>같은 제약 위반이라도 드라이버마다 알려 주는 방식이 달라서 한 곳에 모아 둔다.
 * SQLite 드라이버는 전용 예외 타입도, SQLState 도 주지 않고 메시지로만 알려 준다.
 */
public final class SqlErrors {

    private SqlErrors() {
    }

    /** 유일 제약(중복 값) 위반인지 확인한다. */
    public static boolean isUniqueViolation(SQLException e) {
        if (e instanceof SQLIntegrityConstraintViolationException) {
            return true;
        }
        // 표준 SQLState 23xxx = 무결성 제약 위반
        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) {
            return true;
        }
        // SQLite: SQLState 가 null 이라 메시지로 판별한다.
        String message = e.getMessage();
        return message != null && message.contains("UNIQUE constraint failed");
    }
}
