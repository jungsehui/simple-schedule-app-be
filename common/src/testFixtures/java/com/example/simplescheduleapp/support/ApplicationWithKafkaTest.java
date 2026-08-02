package com.example.simplescheduleapp.support;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Boot 4의 spring-kafka EmbeddedKafka는 KRaft 모드 — 고정 포트 하드코딩은 내부 리스너 이름 규약(EXTERNAL)과
// 충돌한다. 포트를 고정하지 않고(랜덤) 주입된 EmbeddedKafkaBroker의 실제 주소를 사용한다. 앱의 프로듀서/컨슈머는
// application-common-test.yml의 ${spring.embedded.kafka.brokers}로 같은 브로커에 접속한다. (ADR-0003 Stage 3)
@EmbeddedKafka(partitions = 1)
@SpringBootTest
public class ApplicationWithKafkaTest extends CommonExceptionTest {

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafkaBroker;

    protected ConsumerRecords<String, String> waitingConsumeTopicSync(String topic) {
        try {
            return waitingConsumeTopicAsync(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<ConsumerRecords<String, String>> waitingConsumeTopicAsync(String topic) {
        // Consumer 설정 — 주입된 임베디드 브로커의 실제 부트스트랩 주소 사용
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
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
