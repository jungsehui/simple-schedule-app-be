package com.example.simplescheduleapp.common.messaging;

public interface MessagePublisher {

    void publish(String channel, String message);
}
