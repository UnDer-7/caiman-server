package com.caimanproject.payment.entrypoint.controller;

import com.caimanproject.contracts.config.CaimanServerProps;
import com.caimanproject.contracts.exception.EntrypointException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceParameter;
import com.caimanproject.contracts.validation.ValidationResult;
import com.caimanproject.payment.core.domain.types.BusinessExceptionCode;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import com.caimanproject.payment.core.domain.types.PaymentType;
import com.caimanproject.payment.core.port.in.GetProofPageUseCase;
import com.caimanproject.payment.core.port.in.UploadProofUseCase;
import com.caimanproject.payment.core.port.in.command.GetProofPageCommand;
import com.caimanproject.payment.core.port.in.command.UploadProofCommand;
import com.caimanproject.payment.entrypoint.mapper.ProofPageWebMapper;
import com.caimanproject.payment.entrypoint.payload.response.ProofUploadResponseDto;
import com.caimanproject.web.annotation.CaimanController;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@RequiredArgsConstructor
@CaimanController("/public/proofs")
public class PublicProofController {

    private static final Set<String> ALLOWED_PROOF_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final GetProofPageUseCase getProofPageUseCase;
    private final UploadProofUseCase uploadProofUseCase;
    private final ProofPageWebMapper proofPageWebMapper;
    private final CaimanServerProps caimanServerProps;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getProofPage(@RequestParam("token") final String token) {
        try {
            final var view = getProofPageUseCase.execute(new GetProofPageCommand(parseTokenOrThrow(token)));
            final var modelAndView = new ModelAndView("proof-page", "page", proofPageWebMapper.toDto(view));
            final var endpointPrefix =
                    Objects.requireNonNullElse(caimanServerProps.server().endpointsPrefix(), "");

            modelAndView.addObject("token", token);
            modelAndView.addObject("endpointsPrefix", endpointPrefix);

            return modelAndView;
        } catch (final NotFoundException notFoundException) {
            return new ModelAndView("proof-not-found", HttpStatus.NOT_FOUND);
        } catch (final Exception unexpected) {
            log.error(
                    LogField.Placeholders.TWO.getPlaceholder(),
                    StructuredArguments.kv(LogField.MSG.label(), "unexpected error rendering the public proof page"),
                    StructuredArguments.kv(LogField.EXCEPTION_MESSAGE.label(), unexpected.getMessage()),
                    unexpected);
            return new ModelAndView("proof-error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ProofUploadResponseDto uploadProof(
            @RequestParam("token") final String token,
            @RequestParam("file") final MultipartFile file,
            @RequestParam("paymentType") final PaymentType paymentType,
            @RequestParam(value = "declaredAmount", required = false) final BigDecimal declaredAmount)
            throws IOException {

        final UUID parsedToken = parseTokenOrThrow(token);
        validateUpload(file, paymentType, declaredAmount);

        final var result = uploadProofUseCase.execute(UploadProofCommand.builder()
                .token(parsedToken)
                .fileContent(file.getBytes())
                .fileContentType(file.getContentType())
                .originalFilename(file.getOriginalFilename())
                .paymentType(paymentType)
                .declaredAmount(declaredAmount)
                .build());

        return ProofUploadResponseDto.builder()
                .proofId(result.proofId())
                .message(messageFor(result.status()))
                .build();
    }

    private static String messageFor(final PaymentProofStatus status) {
        return switch (status) {
            case PENDING_ANALYSIS, PENDING_MANUAL_REVIEW ->
                "Your payment proof has been received and is being " + "reviewed. You will be notified of the result.";
            case APPROVED -> "Your payment has been approved. Thank you!";
            case REJECTED ->
                "Your payment proof was rejected. Please contact the system administrator for more " + "information.";
        };
    }

    private UUID parseTokenOrThrow(final String token) {
        try {
            return UUID.fromString(token);
        } catch (final IllegalArgumentException invalidFormat) {
            throw notFoundException();
        }
    }

    private NotFoundException notFoundException() {
        return new NotFoundException(Collections.singletonList(ValidationError.builder()
                .code(BusinessExceptionCode.INVOICE_NOT_FOUND)
                .build()));
    }

    private static void validateUpload(
            final MultipartFile file, final PaymentType paymentType, final BigDecimal declaredAmount) {
        final var validations = new ArrayList<ValidationError>();

        if (file == null || file.isEmpty()) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("file is required")
                    .source(ValidationErrorSourceParameter.builder()
                            .queryParam("file")
                            .invalidValue(null)
                            .build())
                    .build());
        } else if (file.getContentType() == null || !ALLOWED_PROOF_CONTENT_TYPES.contains(file.getContentType())) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("file content type must be one of " + ALLOWED_PROOF_CONTENT_TYPES)
                    .source(ValidationErrorSourceParameter.builder()
                            .queryParam("file")
                            .invalidValue(file.getContentType())
                            .build())
                    .build());
        }

        if (paymentType == PaymentType.PARTIAL
                && (declaredAmount == null || declaredAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("declaredAmount is required and must be greater than 0 when paymentType is PARTIAL")
                    .source(ValidationErrorSourceParameter.builder()
                            .queryParam("declaredAmount")
                            .invalidValue(declaredAmount == null ? null : declaredAmount.toString())
                            .build())
                    .build());
        }

        ValidationResult.of(validations).throwIfInvalid(EntrypointException::new);
    }
}
