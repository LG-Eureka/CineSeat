package com.cineseat.model;

/** 상영 영화. {@code ageLimit} 0 은 전체 관람가를 뜻한다. */
public record Movie(int id, String title, int price, int ageLimit, int runningTime) {

    public String ageLimitLabel() {
        return ageLimit == 0 ? "전체" : ageLimit + "+";
    }

    public String runningTimeLabel() {
        return runningTime + "분";
    }
}
