package com.newland.erp.platform.infrastructure;

import com.newland.erp.platform.application.CachePort;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class InMemoryCacheAdapter implements CachePort {
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void put(final String key, final String value, final Duration ttl) {
        entries.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> get(final String key) {
        final Entry entry = entries.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void evict(final String key) {
        entries.remove(key);
    }

    private record Entry(String value, Instant expiresAt) {
    }
}
