package com.example.simplescheduleapp.common.redis.counter;

public interface AtomicCounter {

    void set(String key, long value);

    Long decrement(String key);

    Long increment(String key);
}
