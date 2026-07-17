package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

/**
 * {@code Schedule} 도메인의 JPA 영속 모델 — JOINED 상속 계층의 루트 (ADR-0004).
 *
 * <p>감사·소프트삭제·상속 매핑·낙관적 락 등 영속 관심사를 도메인 대신 여기서 담당한다.
 */
@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE schedule SET deleted_date = CURRENT_TIMESTAMP WHERE schedule_id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class ScheduleEntity extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "memo")
    private String memo;

    /**
     * 3차 방어선: 낙관적 락. JPA 제약상 {@code @Version}은 JOINED 계층의 루트에만 둘 수 있으므로
     * 여기에 두어 모든 자식(Lecture/SpecialLecture/Consultation)이 함께 보호받는다.
     *
     * <p>도메인({@code Schedule.version})과 <b>반드시 왕복</b>되어야 detached 엔티티 merge 시
     * {@code WHERE version = ?}가 적용된다 — 매퍼의 version 전달이 이 방어선의 전제다.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ScheduleEntity(Long id, Long version, String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
    }
}
