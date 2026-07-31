package com.caimanproject.billing.entrypoint.job;

import com.caimanproject.billing.core.port.in.RunOdinUseCase;
import com.caimanproject.contracts.exception.LogField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdinJobStarter {

    private final RunOdinUseCase runOdinUseCase;

    @Recurring(id = "odin_job", cron = "*/30 * * * * *")
    public void run() {
        log.info(
                LogField.Placeholders.ONE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "Odin job started"));
        runOdinUseCase.execute();
    }
}
