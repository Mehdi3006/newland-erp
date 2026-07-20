package com.newland.erp.platform.infrastructure;

import com.newland.erp.platform.application.FileStoragePort;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public final class InMemoryFileStorageAdapter implements FileStoragePort {
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String put(final String storageKey, final byte[] content) {
        storage.put(storageKey, Arrays.copyOf(content, content.length));
        return storageKey;
    }

    @Override
    public byte[] get(final String storageKey) {
        final byte[] bytes = storage.get(storageKey);
        return bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
    }
}
