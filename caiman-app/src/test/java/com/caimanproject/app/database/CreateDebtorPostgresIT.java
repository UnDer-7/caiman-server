package com.caimanproject.app.database;

import com.caimanproject.app.test.IntegrationTestController;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("createDebtor — PostgreSQL integration")
class CreateDebtorPostgresIT extends IntegrationTestController {

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE debtor_contact CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE debtor CASCADE");
    }

    @Test
    @DisplayName("should create debtor successfully via PostgreSQL")
    void should_create_debtor_postgres() {
        final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

        webTestClient
                .post()
                .uri("/v1/debtors")
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11")
                .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(DebtorResponseDto.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.name()).isEqualTo(request.name());
                    assertThat(response.active()).isTrue();
                    assertThat(response.audit().createdAt()).isNotNull();
                });
    }
}
