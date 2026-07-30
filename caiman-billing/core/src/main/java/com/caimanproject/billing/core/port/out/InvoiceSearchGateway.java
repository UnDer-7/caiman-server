package com.caimanproject.billing.core.port.out;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceSearchGateway {

    Optional<Long> findMaxCycleIndex(UUID chargePlanId);
}
