package com.caimanproject.billing.core.port.out;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import java.util.List;

public interface ChargePlanSearchGateway {

    List<ChargePlan> getAllActives();
}
