package com.example.playground.async.singlethread;

import com.example.playground.async.event.TestDomainEvent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("SINGLE_THREAD_BLOCK_EVENT")
@NoArgsConstructor
@Getter
@Entity
public class SingleThreadBlockEvent extends TestDomainEvent {

    public SingleThreadBlockEvent(Long requestId) {
        super(requestId);
    }
}
