package com.example.simplescheduleapp.sse.presentation;

import com.example.simplescheduleapp.sse.event.RedisSseMessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SseController {

    private final RedisSseMessageSubscriber redisSseMessageSubscriber;

    @GetMapping(value = "/connect/sse/{memberId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connectSse(@PathVariable Long memberId) {
        return ResponseEntity.ok(redisSseMessageSubscriber.connect(memberId));
    }
}
