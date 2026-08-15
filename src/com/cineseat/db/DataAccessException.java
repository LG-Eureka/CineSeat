package com.cineseat.db;

/** DAO 계층에서 발생한 SQL 오류를 화면 계층으로 전달하기 위한 예외. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
