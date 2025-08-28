package com.example.simplescheduleapp.notification.domain;

import com.example.simplescheduleapp.common.entity.BaseEntity;
import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE failed_notification SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "failed_notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class FailedNotification extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;
    private Long senderMemberId;
    private Long targetMemberId;
    private String title;
    private String body;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String failReason;
    private int retryCount;

    public FailedNotification(
            Long senderMemberId,
            Long targetMemberId,
            String title,
            String body,
            NotificationType type,
            String failReason
    ) {
        this.uuid = UUID.randomUUID().toString();
        this.senderMemberId = senderMemberId;
        this.targetMemberId = targetMemberId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.failReason = failReason;
        this.retryCount = 1;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
