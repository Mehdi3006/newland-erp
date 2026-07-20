package com.newland.erp.enterprise.application;

public interface AuthorizationPort {
    void require(String permission);
}
