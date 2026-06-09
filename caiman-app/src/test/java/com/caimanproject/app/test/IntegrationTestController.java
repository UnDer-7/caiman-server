package com.caimanproject.app.test;

import com.caimanproject.app.CaimanApplication;
import com.caimanproject.app.test.config.TestcontainersConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = CaimanApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {TestcontainersConfig.class})
public abstract class IntegrationTestController {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
