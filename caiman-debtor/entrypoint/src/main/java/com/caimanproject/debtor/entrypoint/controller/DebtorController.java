package com.caimanproject.debtor.entrypoint.controller;

import com.caimanproject.contracts.exception.LogField;
import com.caimanproject.contracts.util.LogMask;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.core.port.in.CreateDebtorUseCase;
import com.caimanproject.debtor.entrypoint.controller.spec.DebtorControllerSpec;
import com.caimanproject.debtor.entrypoint.mapper.DebtorWebMapper;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.web.annotation.CaimanEndpoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@RequiredArgsConstructor
@CaimanEndpoint("/v1/debtors")
public class DebtorController implements DebtorControllerSpec {

    private final DebtorWebMapper debtorWebMapper;
    private final CreateDebtorUseCase createDebtorUseCase;

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public DebtorResponseDto createDebtor(@RequestBody final CreateDebtorRequestDto payload) {
        log.info(
                LogField.Placeholders.FIVE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "debtor create request received"),
                StructuredArguments.kv(LogField.DEBTOR_NAME.label(), payload.name()),
                StructuredArguments.kv(LogField.DEBTOR_NOTIFICATIONS_ENABLED.label(), payload.notificationsEnabled()),
                StructuredArguments.kv(
                        LogField.CONTACTS_COUNT.label(), payload.contacts().size()),
                StructuredArguments.kv(LogField.CONTACT_DETAILS.label(), getContactDetails(payload.contacts())));

        final var createCommand = debtorWebMapper.toCommand(payload);
        final var debtor = createDebtorUseCase.execute(createCommand);
        final var response = debtorWebMapper.toDto(debtor);

        log.info(
                LogField.Placeholders.FIVE.getPlaceholder(),
                StructuredArguments.kv(LogField.MSG.label(), "debtor create response"),
                StructuredArguments.kv(LogField.DEBTOR_ID.label(), response.id()),
                StructuredArguments.kv(LogField.DEBTOR_NAME.label(), response.name()),
                StructuredArguments.kv(LogField.DEBTOR_NOTIFICATIONS_ENABLED.label(), response.notificationsEnabled()),
                StructuredArguments.kv(
                        LogField.CONTACTS_COUNT.label(), response.contacts().size()));

        return response;
    }

    private static List<String> getContactDetails(final List<CreateDebtorContactRequestDto> contacts) {
        return contacts.stream()
                .map(c -> {
                    final ContactType contactType = c.contactType();
                    final var contactValue =
                            switch (contactType) {
                                case EMAIL -> LogMask.email(c.contactValue());
                            };
                    return "(contactType: %s - contactValue: %s - priority: %s)"
                            .formatted(contactType, contactValue, c.priority());
                })
                .toList();
    }
}
