package com.caimanproject.web.aot;

import com.caimanproject.web.dto.response.ProblemDetailPropertyErrorResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceBodyResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceGenericResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceHeaderResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceParameterResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourcePathParameterResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailPropertySourceResponseDto;
import com.caimanproject.web.dto.response.ProblemDetailResponseDto;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({
    ProblemDetailPropertyErrorResponseDto.class,
    ProblemDetailPropertySourceBodyResponseDto.class,
    ProblemDetailPropertySourceGenericResponseDto.class,
    ProblemDetailPropertySourceHeaderResponseDto.class,
    ProblemDetailPropertySourceParameterResponseDto.class,
    ProblemDetailPropertySourcePathParameterResponseDto.class,
    ProblemDetailPropertySourceResponseDto.class,
    ProblemDetailResponseDto.class
})
public class WebSupportAotConfig {}
