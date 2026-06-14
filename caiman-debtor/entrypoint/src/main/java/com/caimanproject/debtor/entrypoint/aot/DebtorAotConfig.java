package com.caimanproject.debtor.entrypoint.aot;

import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.AuditResponseDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorContactResponseDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.debtor.entrypoint.validation.ContactValueValidator;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({
    // DTOs — Jackson serialization/deserialization
    CreateDebtorContactRequestDto.class,
    CreateDebtorRequestDto.class,
    AuditResponseDto.class,
    DebtorContactResponseDto.class,
    DebtorResponseDto.class,
    // ConstraintValidators — SpringConstraintValidatorFactory instantiates via reflection (no-arg constructor).
    ContactValueValidator.class
})
public class DebtorAotConfig {
}
