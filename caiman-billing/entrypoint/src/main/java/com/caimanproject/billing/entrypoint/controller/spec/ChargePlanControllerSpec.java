package com.caimanproject.billing.entrypoint.controller.spec;

import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ChargePlanControllerSpec {

    ChargePlanResponseDto createChargePlan(@Valid @NotNull CreateChargePlanRequestDto payload);
}
