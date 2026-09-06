package com.caimanproject.payment.entrypoint.controller;

import com.caimanproject.contracts.exception.EntrypointException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.exception.NotFoundException;
import com.caimanproject.contracts.gateway.invoice.InvoiceGateway;
import com.caimanproject.contracts.validation.ValidationError;
import com.caimanproject.contracts.validation.ValidationErrorSourceParameter;
import com.caimanproject.contracts.validation.ValidationResult;
import com.caimanproject.payment.core.domain.types.BusinessExceptionCode;
import com.caimanproject.payment.core.domain.types.PaymentType;
import com.caimanproject.payment.core.port.in.GetProofPageUseCase;
import com.caimanproject.payment.core.port.in.command.GetProofPageCommand;
import com.caimanproject.payment.entrypoint.mapper.ProofPageWebMapper;
import com.caimanproject.payment.entrypoint.payload.response.ProofUploadResponseDto;
import com.caimanproject.web.exception.WebSupportExceptionCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/public/proofs")
@RequiredArgsConstructor
public class PublicProofController {

    private static final Set<String> ALLOWED_PROOF_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final GetProofPageUseCase getProofPageUseCase;
    private final InvoiceGateway invoiceGateway;
    private final ProofPageWebMapper proofPageWebMapper;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getProofPage(@RequestParam("token") final String token) {
        try {
            final var view = getProofPageUseCase.execute(new GetProofPageCommand(parseTokenOrThrow(token)));
            final var modelAndView = new ModelAndView("proof-page", "page", proofPageWebMapper.toDto(view));
            modelAndView.addObject("token", token);
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
            @RequestParam(value = "declaredAmount", required = false) final BigDecimal declaredAmount) {

        final UUID parsedToken = parseTokenOrThrow(token);
        validateUpload(file, paymentType, declaredAmount);

        final var invoice = invoiceGateway.findByUploadToken(parsedToken).orElseThrow(this::notFoundException);

        log.info(
                LogField.Placeholders.SEVEN.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "payment proof upload received"),
                StructuredArguments.kv(LogField.INVOICE_ID.label(), invoice.id()),
                StructuredArguments.kv(LogField.PAYMENT_TYPE.label(), paymentType),
                StructuredArguments.kv(LogField.DECLARED_AMOUNT.label(), declaredAmount),
                StructuredArguments.kv(LogField.PROOF_FILE_NAME.label(), file.getOriginalFilename()),
                StructuredArguments.kv(LogField.PROOF_CONTENT_TYPE.label(), file.getContentType()),
                StructuredArguments.kv(LogField.PROOF_FILE_SIZE_BYTES.label(), file.getSize()));

        return new ProofUploadResponseDto("Received. Proof handling is not implemented yet.");
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
                    .source(new ValidationErrorSourceParameter("file", null))
                    .build());
        } else if (file.getContentType() == null || !ALLOWED_PROOF_CONTENT_TYPES.contains(file.getContentType())) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("file content type must be one of " + ALLOWED_PROOF_CONTENT_TYPES)
                    .source(new ValidationErrorSourceParameter("file", file.getContentType()))
                    .build());
        }

        if (paymentType == PaymentType.PARTIAL
                && (declaredAmount == null || declaredAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            validations.add(ValidationError.builder()
                    .code(WebSupportExceptionCode.INVALID_VALUES)
                    .detail("declaredAmount is required and must be greater than 0 when paymentType is PARTIAL")
                    .source(new ValidationErrorSourceParameter(
                            "declaredAmount", declaredAmount == null ? null : declaredAmount.toString()))
                    .build());
        }

        ValidationResult.of(validations).throwIfInvalid(EntrypointException::new);
    }
}
