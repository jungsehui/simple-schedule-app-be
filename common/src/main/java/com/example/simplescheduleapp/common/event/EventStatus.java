package com.example.simplescheduleapp.common.event;

public enum EventStatus {

    INIT,            // 초기 생성
    PRODUCE_SUCCESS, // 발행 성공
    PRODUCE_FAIL,    // 발행 실패
    DEAD,            // 최대 재시도 초과
    ;
}
