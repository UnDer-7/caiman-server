package com.caimanproject.payment.infrastructure.database.config;

import com.caimanproject.contracts.config.CaimanServerProps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProofStorageInitializer implements InitializingBean {

    private final CaimanServerProps caimanServerProps;

    @Override
    public void afterPropertiesSet() throws IOException {
        final var path = Path.of(caimanServerProps.payment().proofStorageFolderPath());

        Files.createDirectories(path);

        if (!Files.isReadable(path)) {
            throw new IllegalStateException(
                    "Payment proof storage folder '%s' is not readable. Check the directory's permissions or CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH."
                            .formatted(path));
        }

        if (!Files.isWritable(path)) {
            throw new IllegalStateException(
                    "Payment proof storage folder '%s' is not writable. Check the directory's permissions or CAIMAN_SERVER_PAYMENT_PROOF_STORAGE_FOLDER_PATH."
                            .formatted(path));
        }
    }
}
