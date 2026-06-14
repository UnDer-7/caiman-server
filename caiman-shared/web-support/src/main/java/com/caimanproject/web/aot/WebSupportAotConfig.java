package com.caimanproject.web.aot;

import com.caimanproject.web.dto.response.ErrorResponseDto;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({
    ErrorResponseDto.class
})
public class WebSupportAotConfig {

}
