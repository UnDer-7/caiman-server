package com.caimanproject.web.mapper;

import com.caimanproject.contracts.exception.CaimanException;
import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.validation.ValidationErrorSource;
import com.caimanproject.contracts.validation.ValidationErrorSourceBody;
import com.caimanproject.contracts.validation.ValidationErrorSourceGeneric;
import com.caimanproject.contracts.validation.ValidationErrorSourceHeader;
import com.caimanproject.contracts.validation.ValidationErrorSourceParameter;
import com.caimanproject.contracts.validation.ValidationErrorSourcePathParameter;
import com.caimanproject.web.dto.response.ProblemDetailPropertyErrorResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceBodyResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceGenericResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceHeaderResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceParameterResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourcePathParameterResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailResponseDto;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CaimanExceptionMapper {

    public ProblemDetailResponseDto toProblemDetailResponse(final CaimanException exception) {
        final Function<LogField, String> getFromMDC = field -> Optional.of(field)
                .map(LogField::label)
                .map(MDC::get)
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        final String requestId = getFromMDC.apply(LogField.REQUEST_ID);
        final String correlationId = getFromMDC.apply(LogField.CORRELATION_ID);
        final String channel = getFromMDC.apply(LogField.CHANNEL);

        final var errorsDto = exception.getErrors().stream()
                .map(error -> ProblemDetailPropertyErrorResponseDto.builder()
                        .code(error.getCode().getFullCode())
                        .message(error.getCode().getMessage())
                        .detail(error.getDetail().orElse(null))
                        .source(error.getSource()
                                .map(CaimanExceptionMapper::buildPropertySourceResponse)
                                .orElse(null))
                        .build())
                .toList();

        return ProblemDetailResponseDto.builder()
                .status(exception.getHttpStatusCode())
                .title(exception.getTitle())
                .detail(exception.getDetail())
                .instance(URI.create("/requests/" + requestId))
                .correlationId(correlationId)
                .channel(channel)
                .errors(exception.getHttpStatusCode() == 500 ? null : errorsDto)
                .build();
    }

    private static ProblemDetailPropertySourceResponseDto buildPropertySourceResponse(
            final ValidationErrorSource source) {
        final String invalidValue = source.invalidValue();
        return switch (source) {
            case ValidationErrorSourceBody body ->
                new ProblemDetailPropertySourceBodyResponseDto(invalidValue, body.body());
            case ValidationErrorSourceParameter parameter ->
                new ProblemDetailPropertySourceParameterResponseDto(invalidValue, parameter.queryParam());
            case ValidationErrorSourceHeader header ->
                new ProblemDetailPropertySourceHeaderResponseDto(invalidValue, header.header());
            case ValidationErrorSourcePathParameter pathParameter ->
                new ProblemDetailPropertySourcePathParameterResponseDto(invalidValue, pathParameter.pathParameter());
            case ValidationErrorSourceGeneric generic ->
                new ProblemDetailPropertySourceGenericResponseDto(invalidValue, generic.fieldName());
        };
    }
}
