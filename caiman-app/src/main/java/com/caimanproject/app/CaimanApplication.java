package com.caimanproject.app;

import com.caimanproject.app.property.CaimanServerPropsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(CaimanServerPropsConfig.class)
@SpringBootApplication(scanBasePackages = "com.caimanproject")
public class CaimanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaimanApplication.class, args);
    }
}
