package com.caimanproject.web.filter;

import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.util.RequestConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Order(1)
@Configuration
@RequiredArgsConstructor
public class RequestLoggingFilterConfig extends OncePerRequestFilter {

    @Override
    public void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        putHeadersInMdc(request);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static void putHeadersInMdc(final HttpServletRequest request) {

        final String correlationId = request.getHeader(RequestConstants.Headers.X_CORRELATION_ID);
        final String channel = request.getHeader(RequestConstants.Headers.X_CHANNEL);

        MDC.put(LogField.REQUEST_ID.label(), UUID.randomUUID().toString());
        MDC.put(LogField.HTTP_REQUEST.label(), "%s %s".formatted(request.getMethod(), request.getRequestURI()));
        MDC.put(LogField.CORRELATION_ID.label(), correlationId);
        MDC.put(LogField.CHANNEL.label(), channel);
    }
}
