package com.caimanproject.web.filter;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.EntrypointException;
import com.caimanproject.contracts.util.Constants;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceHeader;
import com.caimanproject.contracts.validation.ValidationResult;
import com.caimanproject.web.constant.OpenApiConstants;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import com.caimanproject.web.mapper.CaimanExceptionMapper;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Order(2)
@Configuration
public class RequiredHeaderFilterConfig extends OncePerRequestFilter {
    private static final String WILDCARD_PATH = "/**/*";

    private final CaimanExceptionMapper caimanExceptionMapper;

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
            @Value("${management.endpoints.web.base-path:/manage}") final String managementBasePath,
            final CaimanExceptionMapper caimanExceptionMapper) {

        this.pathMatcher = pathMatcher;

        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.springDocConfigProperties = springDocConfigProperties;
        this.objectMapper = objectMapper;
        this.caimanExceptionMapper = caimanExceptionMapper;

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

        final ValidationResult validationResult = validateRequiredHeaders(request);
        if (validationResult.isValid()) {
            filterChain.doFilter(request, response);
        } else {
            final var exception = new EntrypointException(validationResult.errors());
            buildErrorResponse(response, exception);
        }
    }

    private static boolean isValidUuid(final String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private ValidationResult validateRequiredHeaders(final HttpServletRequest request) {
        final String correlationId = request.getHeader(RequestConstants.Headers.X_CORRELATION_ID);
        final String channel = request.getHeader(RequestConstants.Headers.X_CHANNEL);

        final var validations = new ArrayList<ValidationError>();

        if (correlationId == null || correlationId.isBlank()) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("Required field is null/blank")
                    .source(new ValidationErrorSourceHeader(RequestConstants.Headers.X_CORRELATION_ID, correlationId))
                    .build());
        }

        if (channel == null || channel.isBlank()) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("Required field is null/blank")
                    .source(new ValidationErrorSourceHeader(RequestConstants.Headers.X_CHANNEL, channel))
                    .build());
        }

        if (!isValidUuid(correlationId)) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("Required uuid must be a valid format: %s (example: %s)"
                            .formatted(Constants.UUID_FORMAT, OpenApiConstants.Examples.UUID))
                    .source(new ValidationErrorSourceHeader(RequestConstants.Headers.X_CORRELATION_ID, correlationId))
                    .build());
        }

        return ValidationResult.of(validations);
    }

    private void buildErrorResponse(final HttpServletResponse response, final CaimanException exception)
            throws IOException {

        exception.executeLogging();
        final var responseBody = caimanExceptionMapper.toProblemDetailResponse(exception);

        response.setStatus(exception.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
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
}
