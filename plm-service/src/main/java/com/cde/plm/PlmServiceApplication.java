package com.cde.plm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication @EnableCaching
public class PlmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlmServiceApplication.class, args);
    }
}
