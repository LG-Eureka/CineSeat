-- CineSeat 스키마 및 예시 데이터
--   mysql -u root -p < db/schema.sql
--
-- 상영 일정은 오늘 날짜를 기준으로 만들어지므로, 언제 실행해도 달력에서 바로 예매해 볼 수 있다.

CREATE DATABASE IF NOT EXISTS moviedb
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE moviedb;

DROP TABLE IF EXISTS reserve;
DROP TABLE IF EXISTS screen;
DROP TABLE IF EXISTS place;
DROP TABLE IF EXISTS movie;
DROP TABLE IF EXISTS users;

-- 회원
CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    age      INT NOT NULL
);

-- 영화. age_limit 0 은 전체 관람가
CREATE TABLE movie (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(50) NOT NULL,
    price        INT NOT NULL,
    age_limit    INT NOT NULL DEFAULT 0,
    running_time INT NOT NULL
);

-- 상영관
CREATE TABLE place (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    addr VARCHAR(100)
);

-- 상영 일정. 한 행이 "이 기간 동안 매일 이 시각에 하는 한 회차" 를 뜻한다.
-- 여러 시간대를 한 칸에 몰아 넣지 않기 때문에 회차를 그대로 조회할 수 있다.
CREATE TABLE screen (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    movie_id    INT NOT NULL,
    place_id    INT NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    start_time  TIME NOT NULL,
    total_seats INT NOT NULL DEFAULT 40,
    FOREIGN KEY (movie_id) REFERENCES movie(id),
    FOREIGN KEY (place_id) REFERENCES place(id),
    UNIQUE KEY uk_screen (movie_id, place_id, start_date, start_time)
);

-- 예매. 취소는 행을 지우지 않고 delete_fg 로 표시한다.
CREATE TABLE reserve (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50) NOT NULL,
    movie_id     INT NOT NULL,
    place_id     INT NOT NULL,
    reserve_date DATE NOT NULL,
    reserve_time TIME NOT NULL,
    reserve_cnt  INT NOT NULL,
    seat         VARCHAR(50) NOT NULL,
    price        INT NOT NULL,
    ins_dt       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_fg    CHAR(1) NOT NULL DEFAULT 'N',
    del_dt       TIMESTAMP NULL,
    FOREIGN KEY (username) REFERENCES users(username),
    FOREIGN KEY (movie_id) REFERENCES movie(id),
    FOREIGN KEY (place_id) REFERENCES place(id),
    KEY idx_reserve_screening (movie_id, place_id, reserve_date, reserve_time)
);

-- ---------------------------------------------------------------- 예시 데이터

INSERT INTO users (username, password, age) VALUES
    ('admin', 'admin1234', 30),
    ('user1', 'test1234', 25),
    ('test',  'test1234', 35);

INSERT INTO movie (title, price, age_limit, running_time) VALUES
    ('듄: 파트 2',  15000, 12, 166),
    ('웡카',        12000,  0, 116),
    ('데드맨',      12000, 15, 108),
    ('건국전쟁',    12000, 15, 139);

INSERT INTO place (name, addr) VALUES
    ('광주 충장로', '광주광역시 동구 충장로'),
    ('광주 상무',   '광주광역시 서구 상무중앙로'),
    ('광주 용봉',   '광주광역시 북구 용봉동');

-- 오늘부터 3주 동안의 상영 일정
INSERT INTO screen (movie_id, place_id, start_date, end_date, start_time, total_seats) VALUES
    (1, 1, CURDATE(), CURDATE() + INTERVAL 20 DAY, '11:00:00', 40),
    (1, 1, CURDATE(), CURDATE() + INTERVAL 20 DAY, '14:30:00', 40),
    (1, 1, CURDATE(), CURDATE() + INTERVAL 20 DAY, '20:00:00', 40),
    (1, 2, CURDATE(), CURDATE() + INTERVAL 20 DAY, '12:00:00', 32),
    (1, 3, CURDATE(), CURDATE() + INTERVAL 20 DAY, '16:00:00', 40),
    (2, 1, CURDATE(), CURDATE() + INTERVAL 14 DAY, '15:00:00', 40),
    (2, 2, CURDATE(), CURDATE() + INTERVAL 14 DAY, '17:30:00', 32),
    (2, 3, CURDATE(), CURDATE() + INTERVAL 14 DAY, '19:00:00', 40),
    (3, 2, CURDATE(), CURDATE() + INTERVAL 10 DAY, '13:00:00', 32),
    (3, 3, CURDATE(), CURDATE() + INTERVAL 10 DAY, '21:00:00', 40),
    (4, 1, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 24 DAY, '18:00:00', 40),
    (4, 3, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 24 DAY, '10:30:00', 40);

-- 좌석이 일부 차 있는 상태를 보여 주기 위한 예시 예매
INSERT INTO reserve (username, movie_id, place_id, reserve_date, reserve_time, reserve_cnt, seat, price) VALUES
    ('user1', 1, 1, CURDATE(), '11:00:00', 4, 'C3,C4,C5,C6', 60000),
    ('user1', 2, 1, CURDATE() + INTERVAL 1 DAY, '15:00:00', 2, 'B1,B2', 24000),
    ('test',  1, 1, CURDATE(), '11:00:00', 3, 'A1,A2,A3',   45000),
    ('test',  3, 2, CURDATE() + INTERVAL 2 DAY, '13:00:00', 1, 'D5', 12000);
