package com.caimanproject.billing.core.port.in;

import com.caimanproject.billing.core.domain.model.ChargePlan;
import com.caimanproject.billing.core.port.in.command.CreateChargePlanCommand;

public interface CreateChargePlanUseCase {

    ChargePlan execute(CreateChargePlanCommand chargePlanCommand);

}
