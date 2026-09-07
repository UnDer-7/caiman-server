package com.caimanproject.contracts.gateway.invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceGateway {

    /**
     * @param token the invoice's upload_token
     * @return a snapshot of the invoice (and its plan/debtor context), empty if no invoice has this token
     */
    Optional<InvoiceSnapshotDto> findByUploadToken(UUID token);
}
