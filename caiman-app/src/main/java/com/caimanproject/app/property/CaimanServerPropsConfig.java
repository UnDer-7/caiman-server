package com.caimanproject.app.property;

import com.caimanproject.contracts.config.CaimanServerProps;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "caiman-server", ignoreUnknownFields = false)
public record CaimanServerPropsConfig(
    ApplicationPropImpl application
) implements CaimanServerProps {

    record ApplicationPropImpl(
        @Pattern(regexp = "^$|^/[a-zA-Z0-9]([a-zA-Z0-9._~-]|/[a-zA-Z0-9])*+$", message = """
                Invalid endpoints prefix. Must be empty/null or a valid URL path starting with '/' (e.g. '/api', '/server/v1'). \
                Cannot be just '/', cannot end with '/', and must contain only alphanumeric characters, '.', '_', '~', or '-'
                """)
        String endpointsPrefix
    ) implements CaimanServerProps.ApplicationProp{
    }
}
