package cupid.common.kafka.consumer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaMessageProcessHistoryRepository extends JpaRepository<KafkaMessageConsumeHistory, Long> {

    Optional<KafkaMessageConsumeHistory> findByUuid(String uuid);
}
