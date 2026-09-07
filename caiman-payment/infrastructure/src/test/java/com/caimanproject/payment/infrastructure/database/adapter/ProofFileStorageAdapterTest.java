package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.payment.core.port.out.command.StoreProofFileCommand;
import com.caimanproject.test.annotation.UnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ProofFileStorageAdapterTest {

    @Mock
    CaimanServerProps caimanServerProps;

    @Mock
    CaimanServerProps.PaymentProp paymentProp;

    ProofFileStorageAdapter adapter;

    Path storageRoot;

    @BeforeEach
    void setUp() throws IOException {
        storageRoot = Files.createTempDirectory("proof-storage-test");
        lenient().when(caimanServerProps.payment()).thenReturn(paymentProp);
        lenient().when(paymentProp.proofStorageFolderPath()).thenReturn(storageRoot.toString());
        adapter = new ProofFileStorageAdapter(caimanServerProps);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var files = Files.list(storageRoot)) {
            files.forEach(path -> path.toFile().delete());
        }
        Files.deleteIfExists(storageRoot);
    }

    @Test
    void should_sanitize_debtor_and_plan_name_and_use_content_type_extension() {
        final var command = new StoreProofFileCommand(
                "bytes".getBytes(), "image/jpeg", "Mateus Silva/../etc", "Shared Netflix!!", 3);

        final var relativePath = adapter.store(command);

        assertThat(relativePath).matches("Mateus-Silva-etc_Shared-Netflix_3_\\d{8}_\\d{6}_UTC_[0-9a-f-]{36}\\.jpg");
        assertThat(Files.exists(storageRoot.resolve(relativePath))).isTrue();
    }

    @Test
    void should_use_pdf_extension_for_application_pdf_content_type() {
        final var command = new StoreProofFileCommand("bytes".getBytes(), "application/pdf", "John", "Plan", 0);

        final var relativePath = adapter.store(command);

        assertThat(relativePath).endsWith(".pdf");
    }

    @Test
    void should_cap_sanitized_name_parts_length() {
        final var longName = "a".repeat(100);
        final var command = new StoreProofFileCommand("bytes".getBytes(), "image/png", longName, longName, 1);

        final var relativePath = adapter.store(command);
        final var firstPart = relativePath.split("_")[0];

        assertThat(firstPart).hasSize(40);
    }

    @Test
    void should_throw_for_unsupported_content_type() {
        final var command = new StoreProofFileCommand("bytes".getBytes(), "text/plain", "John", "Plan", 0);

        assertThatThrownBy(() -> adapter.store(command)).isInstanceOf(IllegalArgumentException.class);
    }
}
