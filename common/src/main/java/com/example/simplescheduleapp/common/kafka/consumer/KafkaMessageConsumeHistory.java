package com.example.simplescheduleapp.common.kafka.consumer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE kafka_message_consume_history SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "kafka_message_consume_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class KafkaMessageConsumeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Column(nullable = false)
    private String topic;

    public KafkaMessageConsumeHistory(String uuid, String topic) {
        this.uuid = uuid;
        this.topic = topic;
    }
}
