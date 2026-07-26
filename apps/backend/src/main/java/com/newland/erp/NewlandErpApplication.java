package com.newland.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulith
@SpringBootApplication
@EnableScheduling
public final class NewlandErpApplication {
    public static void main(final String[] args) {
        SpringApplication.run(NewlandErpApplication.class, args);
    }
}
