package com.caimanproject.app.actuator;

import com.caimanproject.app.test.IntegrationTestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorIT extends IntegrationTestController {

    @Nested
    @DisplayName("GET /manage/health")
    class Health {

        @Test
        @DisplayName("should return 200 without required headers")
        void should_return_200_without_required_headers() {
            webTestClient
                .get()
                .uri("/manage/health")
                .exchange()
                .expectStatus()
                .isOk();
        }

        @Test
        @DisplayName("should return status UP")
        void should_return_status_up() {
            webTestClient
                .get()
                .uri("/manage/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(JsonNode.class)
                .value(json -> {
                    assertThat(json.has("status")).isTrue();
                    assertThat(json.get("status").asString()).isEqualTo("UP");
                });
        }

        @Test
        @DisplayName("should return db component status UP")
        void should_return_db_component_status_up() {
            webTestClient
                .get()
                .uri("/manage/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(JsonNode.class)
                .value(json -> {
                    assertThat(json.has("components")).isTrue();
                    final var db = json.get("components").get("db");
                    assertThat(db).isNotNull();
                    assertThat(db.get("status").asString()).isEqualTo("UP");
                });
        }
    }
}
