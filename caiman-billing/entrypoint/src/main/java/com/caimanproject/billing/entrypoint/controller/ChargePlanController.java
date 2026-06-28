package com.caimanproject.billing.entrypoint.controller;

import com.caimanproject.billing.core.port.in.CreateChargePlanUseCase;
import com.caimanproject.billing.entrypoint.controller.spec.ChargePlanControllerSpec;
import com.caimanproject.billing.entrypoint.mapper.ChargePlanWebMapper;
import com.caimanproject.billing.entrypoint.payload.request.CreateChargePlanRequestDto;
import com.caimanproject.billing.entrypoint.payload.response.ChargePlanResponseDto;
import com.caimanproject.web.annotation.CaimanEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@RequiredArgsConstructor
@CaimanEndpoint("/v1/charge-plans")
public class ChargePlanController implements ChargePlanControllerSpec {

    private final ChargePlanWebMapper chargePlanWebMapper;
    private final CreateChargePlanUseCase createChargePlanUseCase;

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ChargePlanResponseDto createChargePlan(@RequestBody final CreateChargePlanRequestDto payload) {
        final var createCommand = chargePlanWebMapper.toCommand(payload);
        final var chargePlan = createChargePlanUseCase.execute(createCommand);
        return chargePlanWebMapper.toDto(chargePlan);
    }

}
