package com.caimanproject.web.dto.response;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@Getter
@Setter
@ToString
public class ProblemDetailResponseDto extends ProblemDetail {

    private String channel;
    private String correlationId;
    private List<ProblemDetailPropertyErrorResponseDto> errors;

    @Builder
    public ProblemDetailResponseDto(
            final int status,
            final String title,
            final String detail,
            final URI instance,
            final String correlationId,
            final String channel,
            final List<ProblemDetailPropertyErrorResponseDto> errors) {

        this.correlationId = correlationId;
        this.channel = channel;
        this.errors = errors;

        final var problemDetail = ProblemDetail.forStatus(HttpStatus.resolve(status));
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(instance);
        super(problemDetail);
    }
}
