package com.caimanproject.app;

import com.caimanproject.app.initializer.TimezoneInitializer;
import com.caimanproject.app.property.CaimanServerPropsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(CaimanServerPropsConfig.class)
@SpringBootApplication(scanBasePackages = "com.caimanproject")
public class CaimanApplication {

    private CaimanApplication() {
    }

    public static void main(String[] args) {
        final var application = new SpringApplication(CaimanApplication.class);
        application.addInitializers(new TimezoneInitializer());
        application.run(args);
    }
}
