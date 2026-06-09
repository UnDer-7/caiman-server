package com.caimanproject.web.config;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.web.annotation.CaimanEndpoint;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final CaimanServerProps caimanServerProps;

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            openApi.setInfo(buildInfo(openApi));
            openApi.setExternalDocs(buildExternalDocs(openApi));
        };
    }

    @Bean
    public OperationCustomizer globalHeadersCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            final Parameter correlationIdParam = new Parameter()
                    .in("header")
                    .name(RequestConstants.Headers.X_CORRELATION_ID)
                    .description(
                            "Unique identifier for tracking the request across services. Must be a valid UUID format.")
                    .required(true)
                    .schema(new UUIDSchema())
                    .example("550e8400-e29b-41d4-a716-446655440000");

            final Parameter channelParam = new Parameter()
                    .in("header")
                    .name(RequestConstants.Headers.X_CHANNEL)
                    .description("Channel/Client that originated the request (e.g., Web, Telegram, WhatsApp)")
                    .required(true)
                    .schema(new StringSchema())
                    .example("Web");

            operation.addParametersItem(correlationIdParam);
            operation.addParametersItem(channelParam);

            return operation;
        };
    }

    @Bean
    public OperationCustomizer globalApiResponsesCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (!handlerMethod.getBeanType().isAnnotationPresent(CaimanEndpoint.class)) {
                return operation;
            }

            customErrorResponses(operation);

            return operation;
        };
    }

    private void customErrorResponses(final Operation operation) {
        operation
                .getResponses()
                .addApiResponse(Integer.toString(HttpStatus.BAD_REQUEST.value()), buildBadRequestResponse());
//        operation
//                .getResponses()
//                .addApiResponse(
//                    Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value()), buildInternalServerErrorResponse());
    }

    private ApiResponse buildBadRequestResponse() {
        return new ApiResponse()
                .description("Invalid values")
                .content(new Content()
                        .addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponseDto"))
                                        .addExamples(
                                                "Required headers are missing or invalid",
                                                new Example()
                                                        .summary("Missing required headers")
                                                        .value("""
                                                            {
                                                                "code": "002",
                                                                "timestamp": "2026-02-15T17:32:10.590433349Z",
                                                                "message": "Invalid values",
                                                                "customMessage": "Missing headers. Headers: [X-Correlation-ID, X-Channel] are required",
                                                                "httpStatusCode": 400
                                                            }
                                                            """))
                                        .addExamples(
                                                "Any invalid input data will return a response similar to this",
                                                new Example()
                                                        .summary("Invalid input data")
                                                        .value("""
                                                            {
                                                                "code": "002",
                                                                "timestamp": "2026-02-04T17:29:21.398223803Z",
                                                                "message": "Invalid values",
                                                                "customMessage": "[ propertyPath: mealTypeId - errorMotive: must not be null - valueProvided: null ]",
                                                                "httpStatusCode": 400
                                                            }
                                                            """))));
    }

    private ApiResponse buildInternalServerErrorResponse() {
        return new ApiResponse()
                .description("Internal Server Error")
                .content(new Content()
                        .addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponseDto"))
                                        .addExamples(
                                                "Any unexpected internal error will return a response similar to this",
                                                new Example()
                                                        .summary("Unexpected internal error")
                                                        .value("""
                                                            {
                                                                "code": "001",
                                                                "timestamp": "2026-02-15T17:42:49.743256697Z",
                                                                "message": "An unexpected error occurred",
                                                                "httpStatusCode": 500
                                                            }
                                                            """))));
    }

    private ExternalDocumentation buildExternalDocs(final OpenAPI openApi) {
        final var externalDocs = Objects.requireNonNullElseGet(openApi.getExternalDocs(), ExternalDocumentation::new);

        final var documentation = caimanServerProps.openApi().application().documentation();
        externalDocs.setUrl(documentation.url());
        externalDocs.setDescription(documentation.description());

        return externalDocs;
    }

    private Info buildInfo(final OpenAPI openApi) {
        final var info = Objects.requireNonNullElseGet(openApi.getInfo(), Info::new);
        final var applicationProp = caimanServerProps.project();
        info.setTitle(applicationProp.name());
        info.description(applicationProp.description());
        info.version(applicationProp.version());

        final var contact = Objects.requireNonNullElseGet(openApi.getInfo().getContact(), Contact::new);
        final var contactProp = caimanServerProps.openApi().application().contact();
        contact.setName(contactProp.name());
        contact.setUrl(contactProp.url());
        contact.setEmail(contactProp.email());

        info.setContact(contact);

        return info;
    }
}
