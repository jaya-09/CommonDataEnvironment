package com.cde.qlm;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication @EnableCaching
public class QlmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(QlmServiceApplication.class, args);
    }
}
