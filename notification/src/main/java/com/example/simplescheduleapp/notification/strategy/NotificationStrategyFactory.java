package com.example.simplescheduleapp.notification.strategy;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.kafka.LectureEventType;
import com.example.simplescheduleapp.notification.exception.NotificationTypeExceptionCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationStrategyFactory {

    private final Map<LectureEventType, NotificationStrategy> notificationStrategies;

    public NotificationStrategyFactory(List<NotificationStrategy> strategies) {
        this.notificationStrategies = new EnumMap<>(LectureEventType.class);

        for (NotificationStrategy strategy : strategies) {
            notificationStrategies.put(strategy.getSupportType(), strategy);
        }
    }

    public NotificationStrategy getStrategy(LectureEventType type) {
        NotificationStrategy strategy = notificationStrategies.get(type);

        if (strategy == null) {
            throw new ApplicationException(NotificationTypeExceptionCode.NOTIFICATION_TYPE_NOT_FOUND);
        }

        return strategy;
    }
}
