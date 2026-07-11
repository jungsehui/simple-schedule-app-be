package com.example.simplescheduleapp.notification.domain;

import com.example.simplescheduleapp.common.domain.SoftDeletedDomain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_DATE_IS_NULL;

@SQLRestriction(DELETED_DATE_IS_NULL)
@SQLDelete(sql = "UPDATE failed_notification SET deleted_date = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "failed_notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class FailedNotification extends SoftDeletedDomain {

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

    public FailedNotification(
            Long senderId,
            Long targetId,
            String title,
            String body,
            NotificationType type,
            String failReason
    ) {
        this.uuid = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.targetId = targetId;
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
