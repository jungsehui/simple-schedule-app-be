package com.example.simplescheduleapp.schedule.domain;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE schedule SET deleted_date = CURRENT_TIMESTAMP WHERE schedule_id = ?")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@Table(name = "schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class Schedule extends SoftDeletedDomain {

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
     * 3차 방어선: 낙관적 락(Optimistic Lock)을 위한 버전 필드.
     * <p>
     * JPA 제약상 {@code @Version}은 엔티티 계층의 <b>루트(root)</b>에만 존재할 수 있다.
     * Schedule이 {@code @Inheritance(JOINED)} 계층의 root entity이므로,
     * 여기에 두어 모든 자식 엔티티(Lecture, SpecialLecture, Consultation)가 함께 낙관적 락 보호를 받는다.
     * <p>
     * JPA가 UPDATE 시 {@code WHERE version = ?}을 자동으로 추가한다.
     * 다른 트랜잭션이 먼저 commit했다면 UPDATE 결과가 0행 → {@link jakarta.persistence.OptimisticLockException} 발생.
     * <p>
     * 정상 시(2차 분산 락이 직렬화 보장): 버전 체크 비용만 추가, 충돌 없음.
     * 비정상 시(분산 락 만료/네트워크 파티션): 동시 수정을 감지하여 안전 실패.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Schedule(String title, LocalDateTime startTime, LocalDateTime endTime, String memo) {
        validatePastTime(startTime, endTime);
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.memo = memo;
        this.version = 0L;
    }

    protected void updateSchedule(Schedule schedule) {
        this.title = schedule.title;
        this.startTime = schedule.startTime;
        this.endTime = schedule.endTime;
        this.memo = schedule.memo;
    }

    private void validatePastTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new ApplicationException(LectureExceptionCode.INVALID_LECTURE_TIME_PAST);
        }
    }
}
