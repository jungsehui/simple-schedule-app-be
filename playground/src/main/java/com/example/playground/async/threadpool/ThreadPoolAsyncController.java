package com.example.playground.async.singlethread;

import com.example.playground.async.event.TestTxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@RestController
public class SingleThreadAsyncController {

    private final AtomicLong atomicId = new AtomicLong(0);
    private final TestTxEventService txEventService;

    @GetMapping("/test/async/single-thread")
    public ResponseEntity<Void> call() {
        long id = atomicId.incrementAndGet();
        log.info("[single thread] start id: {}", id);
        txEventService.publish(new SingleThreadBlockEvent(id));
        log.info("[single thread] end id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
