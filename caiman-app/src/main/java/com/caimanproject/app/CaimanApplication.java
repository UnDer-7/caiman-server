package com.caimanproject.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.caimanproject")
public class CaimanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaimanApplication.class, args);
    }
}
