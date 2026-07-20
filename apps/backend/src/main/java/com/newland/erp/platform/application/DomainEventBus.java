package com.newland.erp.platform.application;

import com.newland.erp.platform.domain.PlatformDomainEvent;

public interface DomainEventBus {
    void publish(PlatformDomainEvent event);
}
