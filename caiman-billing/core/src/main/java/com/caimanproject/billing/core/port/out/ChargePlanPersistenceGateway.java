package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.ChargePlan;

public interface ChargePlanPersistenceGateway {

    ChargePlan save(final ChargePlan chargePlan);
}
