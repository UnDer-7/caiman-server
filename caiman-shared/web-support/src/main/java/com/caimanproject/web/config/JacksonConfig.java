package com.caimanproject.web.config;

import com.caimanproject.contracts.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            final DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);
            final DateTimeFormatter timeFormatter =
                    DateTimeFormatter.ofPattern(Constants.TIME_FORMAT);

            final SimpleModule module = new SimpleModule();
            module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

            builder.addModule(module);
        };
    }
}
