package com.example.playground.async.virtualthread;

import com.example.playground.async.event.TestDomainEvent;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DiscriminatorValue("VIRTUAL_THREAD_NON_BLOCK_EVENT")
@NoArgsConstructor
@Getter
@Entity
public class VirtualThreadNonBlockEvent extends TestDomainEvent {

    public VirtualThreadNonBlockEvent(Long requestId) {
        super(requestId);
    }
}
