package com.caimanproject.payment.infrastructure.database.adapter;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.payment.core.port.out.ProofFileStorageGateway;
import com.caimanproject.payment.core.port.out.command.StoreProofFileCommand;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProofFileStorageAdapter implements ProofFileStorageGateway {

    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9-]+");
    private static final int MAX_NAME_PART_LENGTH = 40;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf");

    private final CaimanServerProps caimanServerProps;

    /** {@inheritDoc} */
    @Override
    public String store(final StoreProofFileCommand command) {
        final var filename = buildFilename(command);
        final var storageRoot = Path.of(caimanServerProps.payment().proofStorageFolderPath());
        final var targetPath = storageRoot.resolve(filename);

        try {
            Files.write(targetPath, command.fileContent());
        } catch (final IOException ioException) {
            throw new UncheckedIOException(
                    "Failed to write payment proof file to '%s'".formatted(targetPath), ioException);
        }

        return filename;
    }

    private static String buildFilename(final StoreProofFileCommand command) {
        final var debtorName = sanitize(command.debtorName());
        final var chargePlanName = sanitize(command.chargePlanName());
        final var timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        final var extension = resolveExtension(command.fileContentType());

        return "%s_%s_%d_%s_UTC_%s.%s"
                .formatted(debtorName, chargePlanName, command.cycleIndex(), timestamp, UUID.randomUUID(), extension);
    }

    private static String sanitize(final String value) {
        final var sanitized = DISALLOWED_CHARS
                .matcher(value == null ? "" : value)
                .replaceAll("-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return sanitized.substring(0, Math.min(sanitized.length(), MAX_NAME_PART_LENGTH));
    }

    private static String resolveExtension(final String contentType) {
        final var extension =
                EXTENSION_BY_CONTENT_TYPE.get(contentType == null ? null : contentType.toLowerCase(Locale.ROOT));
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported proof content type: " + contentType);
        }
        return extension;
    }
}
