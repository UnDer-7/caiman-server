package com.caimanproject.app.test;

import com.caimanproject.app.CaimanApplication;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = CaimanApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestController {

    static final PostgreSQLContainer POSTGRES_CONTAINER =
        new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("caiman-server.database.type", () -> "POSTGRES");
        registry.add("caiman-server.database.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("caiman-server.database.username", POSTGRES_CONTAINER::getUsername);
        registry.add("caiman-server.database.password", POSTGRES_CONTAINER::getPassword);
    }

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
