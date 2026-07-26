package com.newland.erp.platform.application.integration;

public interface PlatformFeatureFlagPort {
  boolean isEnabled(String key);
}
