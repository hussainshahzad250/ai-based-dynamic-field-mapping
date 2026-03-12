package com.fintech.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AIMappingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AIMappingServiceApplication.class, args);
    }
}
