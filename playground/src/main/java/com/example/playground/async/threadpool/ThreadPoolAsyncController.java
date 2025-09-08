package com.example.playground.async.threadpool;

import com.example.playground.async.event.TestTxEventService;
import com.example.playground.async.singlethread.SingleThreadBlockEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ThreadPoolAsyncController {

    private final AtomicLong blockAtomicId = new AtomicLong(0);
    private final AtomicLong nonBlockAtomicId = new AtomicLong(0);
    private final TestTxEventService txEventService;

    @GetMapping("/test/async/thread-pool/block")
    public ResponseEntity<Void> callBlock() {
        long id = blockAtomicId.incrementAndGet();
        log.info("[block thread pool] id: {}", id);
        txEventService.publish(new ThreadPoolBlockEvent(id));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/test/async/thread-pool/non-block")
    public ResponseEntity<Void> callNonBlock() {
        long id = nonBlockAtomicId.incrementAndGet();
        log.info("[non block thread pool] id: {}", id);
        txEventService.publish(new ThreadPoolNonBlockEvent(id));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
