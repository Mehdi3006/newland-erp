package com.newland.erp.platform.application;

public interface FileStoragePort {
    String put(String storageKey, byte[] content);

    byte[] get(String storageKey);
}
