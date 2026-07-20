package com.newland.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@Modulith
@SpringBootApplication
public final class NewlandErpApplication {
    public static void main(final String[] args) {
        SpringApplication.run(NewlandErpApplication.class, args);
    }
}
