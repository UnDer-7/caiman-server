package com.caimanproject.app.config;

import com.caimanproject.app.test.IntegrationTestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

class ApiDocumentationIT extends IntegrationTestController {

    static final String SWAGGER_BASE_URL = "/swagger-ui/index.html";

    @Value("${springdoc.api-docs.path}")
    String apiDocsBaseUrl;

    @Value("${spring.application.name}")
    String applicationName;

    @Nested
    @DisplayName("GET /swagger-ui/index.html")
    class SwaggerUi {

        @Test
        @DisplayName("should return Swagger UI HTML page with status 200")
        void should_return_swagger_ui_html_page() {
            webTestClient
                    .get()
                    .uri(SWAGGER_BASE_URL)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .contentType(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(html -> {
                        assertThat(html).contains("<!DOCTYPE html>");
                        assertThat(html).contains("<title>Swagger UI</title>");
                        assertThat(html).contains("<div id=\"swagger-ui\"></div>");
                        assertThat(html).contains("swagger-ui-bundle.js");
                        assertThat(html).contains("swagger-ui-standalone-preset.js");
                        assertThat(html).contains("swagger-initializer.js");
                    });
        }
    }

    @Nested
    @DisplayName("GET /v3/api-docs")
    class ApiDocs {

        @Test
        @DisplayName("should return OpenAPI JSON with status 200")
        void should_return_openapi_json() {
            webTestClient
                    .get()
                    .uri(apiDocsBaseUrl)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .contentType(MediaType.APPLICATION_JSON)
                    .expectBody(JsonNode.class)
                    .value(json -> {
                        assertThat(json.has("openapi")).isTrue();
                        assertThat(json.get("openapi").asString()).startsWith("3.");

                        assertThat(json.has("info")).isTrue();
                        assertThat(json.get("info").get("title").asString()).isEqualTo(applicationName);

                        assertThat(json.has("paths")).isTrue();
                        assertThat(json.get("paths")).isNotEmpty();

                        assertThat(json.has("components")).isTrue();
                        assertThat(json.get("components").has("schemas")).isTrue();
                    });
        }

        @Test
        @DisplayName("should contain external documentation")
        void should_contain_external_documentation() {
            webTestClient
                    .get()
                    .uri(apiDocsBaseUrl)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JsonNode.class)
                    .value(json -> {
                        assertThat(json.has("externalDocs")).isTrue();
                        assertThat(json.get("externalDocs").has("url")).isTrue();
                        assertThat(json.get("externalDocs").has("description")).isTrue();
                    });
        }

        @Test
        @DisplayName("should contain contact information")
        void should_contain_contact_information() {
            webTestClient
                    .get()
                    .uri(apiDocsBaseUrl)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JsonNode.class)
                    .value(json -> {
                        final var contact = json.get("info").get("contact");
                        assertThat(contact).isNotNull();
                        assertThat(contact.has("name")).isTrue();
                        assertThat(contact.has("url")).isTrue();
                        assertThat(contact.has("email")).isTrue();
                    });
        }
    }
}
