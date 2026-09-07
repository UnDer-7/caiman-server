package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.Invoice;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceSearchGateway {

    Optional<Long> findMaxCycleIndex(UUID chargePlanId);

    boolean existsGeneratedOn(UUID chargePlanId, LocalDate generationDate);

    boolean existsAny(UUID chargePlanId);

    Optional<Invoice> findById(UUID id);
}
