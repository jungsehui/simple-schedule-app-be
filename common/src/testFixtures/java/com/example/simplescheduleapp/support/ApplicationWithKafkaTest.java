package com.example.simplescheduleapp.support;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.simplescheduleapp.support.ApplicationWithKafkaTest.PORT;

@EmbeddedKafka(
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:" + PORT
        },
        ports = {PORT}
)
@SpringBootTest
public class ApplicationWithKafkaTest extends CommonExceptionTest {

    // 49092 포트로 실행중인 작업이 없어야 함
    public static final int PORT = 49092;

    protected ConsumerRecords<String, String> waitingConsumeTopicSync(String topic) {
        try {
            return waitingConsumeTopicAsync(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<ConsumerRecords<String, String>> waitingConsumeTopicAsync(String topic) {
        // Consumer 설정
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        CompletableFuture<ConsumerRecords<String, String>> future = CompletableFuture.supplyAsync(() -> {
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                consumer.subscribe(Collections.singletonList(topic));
                ConsumerRecords<String, String> records;
                long start = System.currentTimeMillis();
                do {
                    records = consumer.poll(Duration.ofMillis(300));
                } while (records.isEmpty() && System.currentTimeMillis() - start < 10_000); // 최대 10초 대기
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
                return records;
            }
        });
        return future;
    }
}
