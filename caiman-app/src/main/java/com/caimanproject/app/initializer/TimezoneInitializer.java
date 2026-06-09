package com.caimanproject.app.initializer;

import com.caimanproject.contracts.exception.LogField;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Initializes the application timezone to UTC before the ApplicationContext is fully created.
 *
 * <p>This initializer enforces UTC as the JVM default timezone to ensure consistent date/time handling across all
 * layers of the application (API, database, logs).
 *
 * <p><strong>Why UTC:</strong>
 *
 * <ul>
 *   <li>Prevents timezone conversion bugs and DST (Daylight Saving Time) issues
 *   <li>Client applications handle timezone conversion (backend stays timezone-agnostic)
 *   <li>Simplifies database queries and data portability
 *   <li>No ambiguity when users travel or change timezones
 * </ul>
 *
 * <p><strong>How it works:</strong>
 *
 * <ul>
 *   <li>Executes BEFORE the ApplicationContext is created, ensuring all beans see UTC
 *   <li>Works with both JVM and GraalVM Native Image without modifications
 *   <li>All API endpoints receive/send dates in ISO 8601 UTC format (e.g., "2025-01-15T14:30:00Z")
 *   <li>Clients (web, mobile, bots) convert UTC to local timezone for display
 * </ul>
 *
 * @see TimeZone#setDefault(TimeZone)
 */
@Slf4j
public class TimezoneInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        final TimeZone utcTimeZone = TimeZone.getTimeZone(ZoneOffset.UTC);
        TimeZone.setDefault(utcTimeZone);

        log.info(
                LogField.Placeholders.TWO.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "Application timezone initialized"),
                StructuredArguments.kv(LogField.TIMEZONE_ID.label(), utcTimeZone.getID()));
    }
}
