package com.caimanproject.app.property;

import com.caimanproject.app.config.DatabaseType;
import com.caimanproject.contracts.config.CaimanServerProps;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Maintenance: every nested record defined here must be explicitly registered in
 * {@link com.caimanproject.app.aot.CaimanRuntimeHints}.
 * <p>
 * Adding, renaming, or removing a nested record requires a matching update in
 * {@code CaimanRuntimeHints#registerHints}. Without it, Hibernate Validator's
 * {@code JPATraversableResolver} will throw {@code MissingReflectionRegistrationError}
 * at native-image runtime when it accesses configuration property fields via reflection.
 */
@Validated
@ConfigurationProperties(prefix = "caiman-server", ignoreUnknownFields = false)
public record CaimanServerPropsConfig(
    @NotNull @Valid LoggingPropImpl logging,
    @NotNull @Valid ApplicationPropImpl server,
    @NotNull @Valid OpenApiPropImp openApi,
    @NotNull @Valid ProjectPropImpl project,
    @NotNull @Valid DatabasePropImpl database
) implements CaimanServerProps {

    public record LoggingPropImpl(
        @NotBlank @Pattern(regexp = "TRACE|DEBUG|INFO|WARN|ERROR", message = "Must be one of: TRACE, DEBUG, INFO, WARN, ERROR")
        String level,

        @NotBlank String folderPath,

        @NotBlank @Pattern(regexp = "UNSTRUCTURED|STRUCTURED", message = "Must be one of: UNSTRUCTURED, STRUCTURED")
        String format
    ) implements CaimanServerProps.LoggingProp {}

    public record DatabasePropImpl(
        @NotNull DatabaseType type,
        String url,
        String username,
        String password,
        String sqliteFile
    ) implements CaimanServerProps.DatabaseProp {

        public DatabasePropImpl {
            url = (url != null && url.isBlank()) ? null : url;
            username = (username != null && username.isBlank()) ? null : username;
            password = (password != null && password.isBlank()) ? null : password;
            sqliteFile = (sqliteFile != null && sqliteFile.isBlank()) ? null : sqliteFile;
        }

        @AssertTrue(message = "DATABASE_TYPE=POSTGRES requires DATABASE_JDBC_URL, DATABASE_USERNAME, DATABASE_PASSWORD environment variables")
        boolean isPostgresConfigValid() {
            return type != DatabaseType.POSTGRES
                || (url != null && username != null && password != null);
        }

        @AssertTrue(message = "DATABASE_TYPE=SQLITE requires DATABASE_SQLITE_FILE environment variable")
        boolean isSqliteConfigValid() {
            return type != DatabaseType.SQLITE
                || sqliteFile != null;
        }
    }

    public record ProjectPropImpl(
        @NotBlank String name,
        @NotBlank String version,
        @NotBlank String description
    ) implements CaimanServerProps.ProjectProp {}

    public record ApplicationPropImpl(
        @Pattern(regexp = "^$|^/[a-zA-Z0-9]([a-zA-Z0-9._~-]|/[a-zA-Z0-9])*+$", message = """
                Invalid endpoints prefix. Must be empty/null or a valid URL path starting with '/' (e.g. '/api', '/server/v1'). \
                Cannot be just '/', cannot end with '/', and must contain only alphanumeric characters, '.', '_', '~', or '-'
                """)
        String endpointsPrefix,

        @NotNull
        Integer port
    ) implements CaimanServerProps.ApplicationProp{ }

    public record OpenApiPropImp(
        @NotNull @Valid OpenApiGenericPropImpl apiDocs,
        @NotNull @Valid OpenApiGenericPropImpl swaggerUi,
        @NotNull @Valid OpenApiApplicationPropImpl application
    ) implements CaimanServerProps.OpenApiProp {}

    public record OpenApiGenericPropImpl(
        @NotBlank String path,
        @NotNull Boolean enabled
    ) implements CaimanServerProps.OpenApiGenericProp {}

    public record OpenApiApplicationPropImpl(
        @NotNull @Valid OpenApiApplicationContactPropImpl contact,
        @NotNull @Valid OpenApiApplicationDocumentationPropImpl documentation
    ) implements CaimanServerProps.OpenApiApplicationProp {}

    public record OpenApiApplicationContactPropImpl(
        @NotBlank String name,
        @NotBlank @URL String url,
        @NotBlank @Email String email
    ) implements CaimanServerProps.OpenApiApplicationContactProp {}

    public record OpenApiApplicationDocumentationPropImpl(
        @NotBlank @URL String url,
        @NotBlank String description
    ) implements CaimanServerProps.OpenApiApplicationDocumentationProp {}
}
