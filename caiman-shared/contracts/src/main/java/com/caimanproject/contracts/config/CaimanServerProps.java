package com.caimanproject.contracts.config;

public interface CaimanServerProps {
    ApplicationProp application();
    OpenApiProp openApi();

    interface ApplicationProp {
        String endpointsPrefix();
    }

    interface OpenApiProp {
        OpenApiGenericProp apiDocs();
        OpenApiGenericProp swaggerUi();
    }

    interface OpenApiGenericProp {
        String path();
        Boolean enabled();
    }
}
