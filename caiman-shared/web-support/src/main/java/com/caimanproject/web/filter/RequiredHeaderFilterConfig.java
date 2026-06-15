package com.caimanproject.web.filter;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.web.dto.response.ErrorResponseDto;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Order(2)
@Configuration
public class RequiredHeaderFilterConfig extends OncePerRequestFilter {
    private static final String WILDCARD_PATH = "/**/*";

    private final AntPathMatcher pathMatcher;
    private final Optional<SwaggerUiConfigProperties> swaggerUiConfigProperties;
    private final Optional<SpringDocConfigProperties> springDocConfigProperties;
    private final ObjectMapper objectMapper;
    private final Set<String> ignoredPaths;

    public RequiredHeaderFilterConfig(
            final AntPathMatcher pathMatcher,
            final Optional<SwaggerUiConfigProperties> swaggerUiConfigProperties,
            final Optional<SpringDocConfigProperties> springDocConfigProperties,
            final ObjectMapper objectMapper,
            @Value("${management.endpoints.web.base-path:/manage}") final String managementBasePath) {

        this.pathMatcher = pathMatcher;

        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.springDocConfigProperties = springDocConfigProperties;
        this.objectMapper = objectMapper;

        final var customIgnoredPath = List.of("/favicon.ico", managementBasePath, managementBasePath + "/**");

        this.ignoredPaths = Stream.of(getApiDocsPaths(), getSwaggerUiPaths(), customIgnoredPath)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {

        final var requestUrl = request.getRequestURI();
        return ignoredPaths.stream().anyMatch(ignoredPath -> pathMatcher.match(ignoredPath, requestUrl));
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {

        final boolean isValid = validateRequiredHeaders(request, response);
        if (isValid) {
            filterChain.doFilter(request, response);
        }
    }

    private static boolean isValidUuid(final String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private boolean validateRequiredHeaders(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final String correlationId = request.getHeader(RequestConstants.Headers.X_CORRELATION_ID);
        final String channel = request.getHeader(RequestConstants.Headers.X_CHANNEL);

        final List<String> missingHeaders = new ArrayList<>();

        if (correlationId == null || correlationId.isBlank()) {
            missingHeaders.add(RequestConstants.Headers.X_CORRELATION_ID);
        }

        if (channel == null || channel.isBlank()) {
            missingHeaders.add(RequestConstants.Headers.X_CHANNEL);
        }

        if (!missingHeaders.isEmpty()) {
            sendMissingHeadersErrorResponse(response, missingHeaders);
            return false;
        }

        if (!isValidUuid(correlationId)) {
            sendInvalidUuidErrorResponse(response, correlationId);
            return false;
        }

        return true;
    }

    private void sendInvalidUuidErrorResponse(final HttpServletResponse response, final String invalidValue)
            throws IOException {
        final var errorResponse = WebSupportExceptionCode.INVALID_VALUES.createException(
                "Header '%s' must be a valid UUID format. Received: '%s'"
                        .formatted(RequestConstants.Headers.X_CORRELATION_ID, invalidValue));

        errorResponse.executeLogging();

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(buildErrorResponse(errorResponse)));
    }

    private void sendMissingHeadersErrorResponse(final HttpServletResponse response, final List<String> missingHeaders)
            throws IOException {
        final var errorResponse = WebSupportExceptionCode.INVALID_VALUES.createException(
                "Missing headers. Headers: %s are required".formatted(missingHeaders));

        errorResponse.executeLogging();

        response.setStatus(errorResponse.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(buildErrorResponse(errorResponse)));
    }

    private List<String> getApiDocsPaths() {
        return springDocConfigProperties
                .map(props -> {
                    final String path = props.getApiDocs().getPath();
                    return List.of(path, path + WILDCARD_PATH);
                })
                .orElse(List.of());
    }

    private List<String> getSwaggerUiPaths() {
        return swaggerUiConfigProperties
                .map(props -> {
                    final String path = props.getPath();
                    return List.of(path, "/swagger-ui" + WILDCARD_PATH);
                })
                .orElse(List.of());
    }

    private static ErrorResponseDto buildErrorResponse(final CaimanException invalidValues) {
        final Function<LogField, String> getFromMDC = field -> Optional.of(field)
                .map(LogField::label)
                .map(MDC::get)
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        final String requestId = getFromMDC.apply(LogField.REQUEST_ID);
        final String correlationId = getFromMDC.apply(LogField.CORRELATION_ID);
        final String channel = getFromMDC.apply(LogField.CHANNEL);

        return ErrorResponseDto.builder()
                .code(invalidValues.getExceptionCode().getFullCode())
                .timestamp(invalidValues.getTimestamp())
                .message(invalidValues.getExceptionCode().getMessage())
                .detail(invalidValues.getDetail().orElse(null))
                .httpStatusCode(invalidValues.getHttpStatusCode())
                .requestId(requestId)
                .correlationId(correlationId)
                .channel(channel)
                .build();
    }
}
