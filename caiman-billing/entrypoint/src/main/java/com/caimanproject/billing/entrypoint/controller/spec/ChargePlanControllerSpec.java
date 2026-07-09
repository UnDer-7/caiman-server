package com.caimanproject.billing.entrypoint.controller.spec;

import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanResponseDto;
import com.caimanproject.web.annotation.composition.body.NotNullBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ChargePlanControllerSpec {

    ChargePlanResponseDto createChargePlan(@Valid @NotNullBody CreateChargePlanRequestDto payload);
}
