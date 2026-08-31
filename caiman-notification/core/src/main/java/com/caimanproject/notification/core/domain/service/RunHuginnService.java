package com.caimanproject.notification.core.domain.service;

import com.caimanproject.notification.core.port.in.RunHuginnUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunHuginnService implements RunHuginnUseCase {

    @Override
    public void execute() {
        log.info("Run Huginn service started --- TESTING");
    }

}
