package com.caimanproject.payment.infrastructure.database.config;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.test.annotation.UnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PaymentProofStorageInitializerTest {

    @Mock
    CaimanServerProps caimanServerProps;

    @Mock
    CaimanServerProps.PaymentProp paymentProp;

    Path tempDir;

    @BeforeEach
    void setUp() {
        when(caimanServerProps.payment()).thenReturn(paymentProp);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("rwxrwxrwx"));
            Files.deleteIfExists(tempDir);
        }
        if (parentDir != null) {
            Files.deleteIfExists(parentDir);
        }
    }

    Path parentDir;

    @Test
    void should_create_storage_directory_when_missing() throws IOException {
        parentDir = Files.createTempDirectory("proof-init-test");
        tempDir = parentDir.resolve("proofs-subfolder");
        when(paymentProp.proofStorageFolderPath()).thenReturn(tempDir.toString());

        new PaymentProofStorageInitializer(caimanServerProps).afterPropertiesSet();

        assertThat(Files.isDirectory(tempDir)).isTrue();
    }

    @Test
    void should_throw_when_storage_directory_is_not_writable() throws IOException {
        tempDir = Files.createTempDirectory("proof-init-readonly-test");
        when(paymentProp.proofStorageFolderPath()).thenReturn(tempDir.toString());
        Files.setPosixFilePermissions(
                tempDir, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));

        assertThatThrownBy(() -> new PaymentProofStorageInitializer(caimanServerProps).afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not writable");
    }
}
