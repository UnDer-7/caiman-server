package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.Invoice;
import java.time.Instant;

public interface NotifyInvoiceCreationGateway {

    void notify(Invoice invoice, String chargePlanName, Instant scheduledFor, int maxAttempts);

}
