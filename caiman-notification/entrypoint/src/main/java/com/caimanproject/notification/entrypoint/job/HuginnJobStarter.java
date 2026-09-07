package com.caimanproject.notification.entrypoint.job;

import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.notification.core.port.in.RunHuginnUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HuginnJobStarter {

    private final RunHuginnUseCase runHuginnUseCase;

    // todo: Configurar para nao rodar caso ja tenha um rodando
    @Recurring(id = "huginn_job", cron = "*/20 * * * * *")
    public void run() {
        log.info(
                LogField.Placeholders.ONE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "Huginn job started"));

        // todo: ver como lidar com erros e o retry do job

        runHuginnUseCase.execute();
    }
}
