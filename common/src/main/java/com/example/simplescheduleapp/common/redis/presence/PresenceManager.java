package com.example.simplescheduleapp.common.redis.presence;

import java.time.Duration;

public interface PresenceManager {

    void markOnline(String key, Duration ttl);

    void markOffline(String key);

    boolean isOnline(String key);

    void refreshTtl(String key, Duration ttl);
}
