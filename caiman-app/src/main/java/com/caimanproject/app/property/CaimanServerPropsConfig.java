package com.caimanproject.app.property;

import com.caimanproject.contracts.config.CaimanServerProps;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "caiman-server", ignoreUnknownFields = false)
public record CaimanServerPropsConfig(
    @NotNull @Valid ApplicationPropImpl server,
    @NotNull @Valid OpenApiPropImp openApi,
    @NotNull @Valid ProjectPropImpl project
) implements CaimanServerProps {

    record ProjectPropImpl(
        @NotBlank String name,
        @NotBlank String version,
        @NotBlank String description
    ) implements CaimanServerProps.ProjectProp {}

    record ApplicationPropImpl(
        @Pattern(regexp = "^$|^/[a-zA-Z0-9]([a-zA-Z0-9._~-]|/[a-zA-Z0-9])*+$", message = """
                Invalid endpoints prefix. Must be empty/null or a valid URL path starting with '/' (e.g. '/api', '/server/v1'). \
                Cannot be just '/', cannot end with '/', and must contain only alphanumeric characters, '.', '_', '~', or '-'
                """)
        String endpointsPrefix,

        @NotNull
        Integer port
    ) implements CaimanServerProps.ApplicationProp{ }

    record OpenApiPropImp(
        @NotNull @Valid OpenApiGenericPropImpl apiDocs,
        @NotNull @Valid OpenApiGenericPropImpl swaggerUi,
        @NotNull @Valid OpenApiApplicationPropImpl application
    ) implements CaimanServerProps.OpenApiProp {}

    record OpenApiGenericPropImpl(
        @NotBlank String path,
        @NotNull Boolean enabled
    ) implements CaimanServerProps.OpenApiGenericProp {}

    record OpenApiApplicationPropImpl(
        @NotNull @Valid OpenApiApplicationContactPropImpl contact,
        @NotNull @Valid OpenApiApplicationDocumentationPropImpl documentation
    ) implements CaimanServerProps.OpenApiApplicationProp {}

    record OpenApiApplicationContactPropImpl(
        @NotBlank String name,
        @NotBlank @URL String url,
        @NotBlank @Email String email
    ) implements CaimanServerProps.OpenApiApplicationContactProp {}

    record OpenApiApplicationDocumentationPropImpl(
        @NotBlank @URL String url,
        @NotBlank String description
    ) implements CaimanServerProps.OpenApiApplicationDocumentationProp {}
}
