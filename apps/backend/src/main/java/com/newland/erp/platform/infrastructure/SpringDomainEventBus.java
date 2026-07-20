package com.newland.erp.platform.infrastructure;

import com.newland.erp.platform.application.DomainEventBus;
import com.newland.erp.platform.domain.PlatformDomainEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class SpringDomainEventBus implements DomainEventBus {
    private final ApplicationEventPublisher publisher;

    public SpringDomainEventBus(final ApplicationEventPublisher eventPublisher) {
        this.publisher = eventPublisher;
    }

    @Override
    public void publish(final PlatformDomainEvent event) {
        publisher.publishEvent(event);
    }
}
