package com.example.simplescheduleapp.kafka.event.outbox;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.outbox.EventRecordListener;
import com.example.simplescheduleapp.common.outbox.EventRecorder;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

@DisplayName("EventRecordListener 은(는)")
public class EventRecordListenerTest extends UnitTest {

    @InjectMocks
    private EventRecordListener eventRecordListener;

    @Mock
    private EventRecorder eventRecorder;

    @DisplayName("이벤트를 저장한다")
    @Test
    void recordEvent() {
        // given
        DomainEvent domainEvent = mock(DomainEvent.class);

        // when
        eventRecordListener.recordEvent(domainEvent);

        // then
        verify(eventRecorder, times(1)).record(any());
    }
}
