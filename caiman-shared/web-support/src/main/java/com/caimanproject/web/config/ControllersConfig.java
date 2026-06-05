package com.caimanproject.web.config;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.web.annotation.CaimanEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ControllersConfig implements WebMvcConfigurer {

    private final CaimanServerProps serverProps;

    @Override
    public void configurePathMatch(final PathMatchConfigurer configurer) {
        configureControllerPrefix(configurer);
    }

    private void configureControllerPrefix(final PathMatchConfigurer configurer) {
        final var endpointsPrefix = serverProps.application().endpointsPrefix();
        if (endpointsPrefix != null && !endpointsPrefix.isBlank()) {
            configurer.addPathPrefix(endpointsPrefix, HandlerTypePredicate.forAnnotation(CaimanEndpoint.class));
        }
    }
}
