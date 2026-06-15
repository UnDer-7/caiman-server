package com.caimanproject.contracts.config;

public interface CaimanServerProps {
    LoggingProp logging();

    ApplicationProp server();

    OpenApiProp openApi();

    ProjectProp project();

    DatabaseProp database();

    interface LoggingProp {
        String level();

        String folderPath();

        String format();
    }

    interface DatabaseProp {
        String url();

        String username();

        String password();
    }

    interface ProjectProp {
        String name();

        String version();

        String description();
    }

    interface ApplicationProp {
        String endpointsPrefix();

        Integer port();
    }

    interface OpenApiProp {
        OpenApiGenericProp apiDocs();

        OpenApiGenericProp swaggerUi();

        OpenApiApplicationProp application();
    }

    interface OpenApiGenericProp {
        String path();

        Boolean enabled();
    }

    interface OpenApiApplicationProp {
        OpenApiApplicationContactProp contact();

        OpenApiApplicationDocumentationProp documentation();
    }

    interface OpenApiApplicationContactProp {
        String name();

        String url();

        String email();
    }

    interface OpenApiApplicationDocumentationProp {
        String url();

        String description();
    }
}
