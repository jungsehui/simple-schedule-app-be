package com.example.playground.async.virtualthread;

import com.example.playground.async.event.TestTxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@RestController
public class VirtualThreadAsyncController {

    private final AtomicLong blockAtomicId = new AtomicLong(0);
    private final AtomicLong nonBlockAtomicId = new AtomicLong(0);
    private final TestTxEventService txEventService;

    @GetMapping("/test/async/virtual-thread/block")
    public ResponseEntity<Void> callBlock() {
        long id = blockAtomicId.incrementAndGet();
        log.info("[block virtual thread] id: {}", id);
        txEventService.publish(new VirtualThreadBlockEvent(id));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/test/async/virtual-thread/non-block")
    public ResponseEntity<Void> callNonBlock() {
        long id = nonBlockAtomicId.incrementAndGet();
        log.info("[non block virtual thread] id: {}", id);
        txEventService.publish(new VirtualThreadNonBlockEvent(id));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
