package com.example.simplescheduleapp.kafka.event.outbox;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.outbox.EventRecorder;
import com.example.simplescheduleapp.common.event.outbox.EventRecorderListener;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

@DisplayName("EventRecorderListener 은(는)")
public class EventRecorderListenerTest extends UnitTest {

    @InjectMocks
    private EventRecorderListener eventRecorderListener;

    @Mock
    private EventRecorder eventRecorder;

    @DisplayName("이벤트를 저장한다")
    @Test
    void recordEvent() {
        // given
        DomainEvent domainEvent = mock(DomainEvent.class);

        // when
        eventRecorderListener.recordEvent(domainEvent);

        // then
        verify(eventRecorder, times(1)).record(any());
    }
}
