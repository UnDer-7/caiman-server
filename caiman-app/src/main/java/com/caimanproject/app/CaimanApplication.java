package com.caimanproject.app;

import com.caimanproject.app.aot.CaimanRuntimeHints;
import com.caimanproject.app.initializer.TimezoneInitializer;
import com.caimanproject.app.property.CaimanServerPropsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(CaimanRuntimeHints.class)
@EnableConfigurationProperties(CaimanServerPropsConfig.class)
@SpringBootApplication(
        exclude = {DataSourceAutoConfiguration.class},
        scanBasePackages = "com.caimanproject")
public class CaimanApplication {

    private static final String STRUCTURED_LOGGING_PROFILE = "structured-logging";
    private static final String FORMAT_ENV = "CAIMAN_SERVER_LOGGING_FORMAT";

    private CaimanApplication() {}

    public static void main(String[] args) {
        final var application = new SpringApplication(CaimanApplication.class);
        application.addInitializers(new TimezoneInitializer());
        if ("STRUCTURED".equalsIgnoreCase(System.getenv(FORMAT_ENV))) {
            application.setAdditionalProfiles(STRUCTURED_LOGGING_PROFILE);
        }
        application.run(args);
    }
}
