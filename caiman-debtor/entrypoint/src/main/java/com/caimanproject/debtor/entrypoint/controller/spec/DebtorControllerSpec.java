package com.caimanproject.debtor.entrypoint.controller.spec;

import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Tag(name = "Debtor")
public interface DebtorControllerSpec {

    DebtorResponseDto createDebtor(@Valid @NotNull CreateDebtorRequestDto payload);

}

