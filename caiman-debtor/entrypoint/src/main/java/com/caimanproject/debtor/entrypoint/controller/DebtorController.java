package com.caimanproject.debtor.entrypoint.controller;

import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.entrypoint.controller.spec.DebtorControllerSpec;
import com.caimanproject.debtor.entrypoint.mapper.DebtorWebMapper;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
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
@CaimanEndpoint("/v1/debtors")
public class DebtorController implements DebtorControllerSpec {

    private final DebtorWebMapper debtorWebMapper;
    private final CreateDebtorUseCase createDebtorUseCase;

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public DebtorResponseDto createDebtor(@RequestBody final CreateDebtorRequestDto payload) {
        final var debtor = debtorWebMapper.toModel(payload);
        final var debtorSaved = createDebtorUseCase.execute(debtor);
        return debtorWebMapper.toDto(debtorSaved);
    }
}
