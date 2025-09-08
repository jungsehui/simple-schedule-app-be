package com.example.playground.async.threadpool;

import com.example.playground.async.event.TestDomainEvent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("THREAD_POOL_NON_BLOCK_EVENT")
@NoArgsConstructor
@Getter
@Entity
public class ThreadPoolNonBlockEvent extends TestDomainEvent {

    public ThreadPoolNonBlockEvent(Long requestId) {
        super(requestId);
    }
}
