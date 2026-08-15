package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.Invoice;

public interface NotifyInvoiceCreationGateway {

    void notify(Invoice invoice);


}
