package com.example.simplescheduleapp.notification.domain;

import com.example.simplescheduleapp.common.entity.SoftDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE notification_message SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class NotificationMessage extends SoftDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderMemberId;
    private Long targetMemberId;
    private String messageBody;

    public NotificationMessage(
            Long senderMemberId,
            Long targetMemberId,
            String messageBody
    ) {
        this.senderMemberId = senderMemberId;
        this.targetMemberId = targetMemberId;
        this.messageBody = messageBody;
    }
}
