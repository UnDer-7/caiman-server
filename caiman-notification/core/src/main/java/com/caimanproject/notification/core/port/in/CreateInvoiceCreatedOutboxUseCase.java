package com.caimanproject.notification.core.port.in;

import com.caimanproject.notification.core.port.in.command.CreateInvoiceCreatedOutboxCommand;

public interface CreateInvoiceCreatedOutboxUseCase {

    void execute(CreateInvoiceCreatedOutboxCommand command);
}
