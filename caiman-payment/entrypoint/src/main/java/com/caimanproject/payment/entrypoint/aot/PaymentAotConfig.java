package com.caimanproject.payment.entrypoint.aot;

import com.caimanproject.payment.entrypoint.payload.response.ProofPageResponseDto;
import com.caimanproject.payment.entrypoint.payload.response.ProofUploadResponseDto;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({
    // DTOs — Jackson serialization (ProofUploadResponseDto) and Thymeleaf/SpEL property
    // access (ProofPageResponseDto) under native image.
    ProofPageResponseDto.class,
    ProofUploadResponseDto.class
})
public class PaymentAotConfig {}
