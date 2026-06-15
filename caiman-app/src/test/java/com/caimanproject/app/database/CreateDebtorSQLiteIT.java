package com.caimanproject.app.database;

import com.caimanproject.app.CaimanApplication;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.test.annotation.IntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@IntegrationTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = CaimanApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("createDebtor — SQLite integration")
class CreateDebtorSQLiteIT {

    private static final Path SQLITE_DB_PATH =
            Path.of(System.getProperty("java.io.tmpdir"), "caiman-test-" + UUID.randomUUID() + ".db");

    @DynamicPropertySource
    static void registerSQLiteProperties(DynamicPropertyRegistry registry) {
        log.info("Registering SQLite properties. SQLite file: {}", SQLITE_DB_PATH);

        registry.add("caiman-server.database.type", () -> "SQLITE");
        registry.add("caiman-server.database.sqlite-file", SQLITE_DB_PATH::toString);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM debtor_contact");
        jdbcTemplate.execute("DELETE FROM debtor");
    }

    @AfterAll
    void deleteSqliteFile() throws IOException {
        final boolean wasDeleted = Files.deleteIfExists(SQLITE_DB_PATH);
        if (wasDeleted) {
            log.info("Successfully deleted SQLite file: {}", SQLITE_DB_PATH);
        } else {
            log.info("Unable to delete SQLite file: {}", SQLITE_DB_PATH);
        }
    }

    @Test
    @DisplayName("should create debtor successfully via SQLite")
    void should_create_debtor_sqlite() {
        final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

        webTestClient
                .post()
                .uri("/v1/debtors")
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd12")
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
