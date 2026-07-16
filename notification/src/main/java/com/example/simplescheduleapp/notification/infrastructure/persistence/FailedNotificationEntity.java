package com.example.simplescheduleapp.notification.infrastructure.persistence;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import com.example.simplescheduleapp.notification.domain.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

/**
 * {@code FailedNotification} 도메인의 JPA 영속 모델 (ADR-0004).
 * <p>테이블·컬럼·소프트삭제 SQL·감사 타임스탬프 등 저장 관심사만 담당하고,
 * 도메인 불변식/행위는 순수 {@code FailedNotification}이 보유한다. 변환은 매퍼가 한다.
 */
@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE failed_notification SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "failed_notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class FailedNotificationEntity extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;
    private Long senderId;
    private Long targetId;
    private String title;
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String failReason;
    private int retryCount;

    public FailedNotificationEntity(
            Long id,
            String uuid,
            Long senderId,
            Long targetId,
            String title,
            String body,
            NotificationType type,
            String failReason,
            int retryCount
    ) {
        this.id = id;
        this.uuid = uuid;
        this.senderId = senderId;
        this.targetId = targetId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.failReason = failReason;
        this.retryCount = retryCount;
    }
}
