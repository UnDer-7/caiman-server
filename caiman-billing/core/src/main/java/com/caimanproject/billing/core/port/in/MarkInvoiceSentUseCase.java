package com.caimanproject.billing.core.port.in;

import java.util.UUID;

public interface MarkInvoiceSentUseCase {

    void execute(UUID invoiceId, String triggerType);
}
