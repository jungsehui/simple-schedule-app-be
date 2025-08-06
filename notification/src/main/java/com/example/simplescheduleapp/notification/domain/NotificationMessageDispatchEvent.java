package com.example.simplescheduleapp.notification.domain;

import com.example.simplescheduleapp.common.event.DomainEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import static com.example.simplescheduleapp.common.SqlRestrictionClause.DELETED_AT_IS_NULL;

@SQLRestriction(DELETED_AT_IS_NULL)
@SQLDelete(sql = "UPDATE notification_message SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class NotificationMessageDispatchEvent extends DomainEvent {

    public NotificationMessageDispatchEvent(Long targetDomainId) {
        super(targetDomainId);
    }

    @Override
    public String getTopic() {
        return "";
    }
}
