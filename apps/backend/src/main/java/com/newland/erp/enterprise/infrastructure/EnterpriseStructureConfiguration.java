package com.newland.erp.enterprise.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public final class EnterpriseStructureConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
