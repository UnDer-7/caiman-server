package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.Invoice;
import java.time.Instant;
import java.util.UUID;

public interface NotifyInvoiceCreationGateway {

    void notify(
            Invoice invoice,
            UUID debtorId,
            String chargePlanName,
            boolean invoiceCreatedNotificationEnabled,
            Instant scheduledFor,
            int maxAttempts);
}
