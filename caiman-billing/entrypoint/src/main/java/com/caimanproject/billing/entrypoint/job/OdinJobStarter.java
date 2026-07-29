package com.caimanproject.billing.entrypoint.job;

import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdinJobStarter {

    private final RunOdinUseCase runOdinUseCase;

    @Recurring(id = "odin_job", cron = "*/6 * * * * *")
    public void run() {
        runOdinUseCase.execute();
    }
}
