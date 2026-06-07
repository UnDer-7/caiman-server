package com.caimanproject.contracts.config;

public interface CaimanServerProps {
    ApplicationProp server();
    OpenApiProp openApi();
    ProjectProp project();

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
