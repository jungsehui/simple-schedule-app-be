package com.example.simplescheduleapp.common.event.producer;

import com.example.simplescheduleapp.common.event.DomainEvent;

public interface EventProducer {

    void produce(DomainEvent domainEvent);
}
