package com.caimanproject.contracts.config;

public interface CaimanServerProps {
    ApplicationProp application();

    interface ApplicationProp {
        String endpointsPrefix();
    }
}
