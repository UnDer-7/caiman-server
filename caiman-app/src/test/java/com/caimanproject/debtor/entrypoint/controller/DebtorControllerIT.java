package com.caimanproject.debtor.entrypoint.controller;

import com.caimanproject.app.test.IntegrationTestController;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;
import com.caimanproject.web.dto.response.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"java:S5961", "Integration tests may have many assertions per method"})
class DebtorControllerIT extends IntegrationTestController {

    static final String BASE_URL = "/v1/debtors";

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE debtor CASCADE");
    }

    @Nested
    @DisplayName("POST /v1/debtors - createDebtor")
    class CreateDebtor {

        @Test
        @DisplayName("should create debtor and return all fields correctly")
        void should_create_debtor() {
            final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

            webTestClient
                .post()
                .uri(BASE_URL)
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11")
                .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(DebtorResponseDto.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.name()).isEqualTo(request.name());
                    assertThat(response.notes()).isEqualTo(request.notes());
                    assertThat(response.notificationsEnabled()).isEqualTo(request.notificationsEnabled());
                    assertThat(response.active()).isTrue();

                    assertThat(response.audit()).isNotNull();
                    assertThat(response.audit().createdAt()).isNotNull();
                    assertThat(response.audit().updatedAt()).isNotNull();

                    assertThat(response.contacts())
                        .isNotEmpty()
                        .hasSize(request.contacts().size());

                    final var requestContact = request.contacts().getFirst();
                    final var responseContact = response.contacts().getFirst();

                    assertThat(responseContact.id()).isNotNull();
                    assertThat(responseContact.contactType()).isEqualTo(requestContact.contactType());
                    assertThat(responseContact.contactValue()).isEqualTo(requestContact.contactValue());
                    assertThat(responseContact.priority()).isEqualTo(requestContact.priority());

                    assertThat(responseContact.audit()).isNotNull();
                    assertThat(responseContact.audit().createdAt()).isNotNull();
                    assertThat(responseContact.audit().updatedAt()).isNotNull();
                });
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void should_return_400_when_name_is_blank() {
            final var request = DtoBuilder.buildCreateDebtorRequestDto()
                .name("")
                .build();

            webTestClient
                .post()
                .uri(BASE_URL)
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11")
                .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponseDto.class)
                .value(response -> {
                    assertThat(response.code()).isEqualTo("WEB_SUPPORT_002");
                    assertThat(response.timestamp()).isNotNull();
                    assertThat(response.message()).isEqualTo("Some invalid values were sent");
                    assertThat(response.detail())
                        .contains("propertyPath: name")
                        .contains("errorMotive: must not be blank");
                    assertThat(response.httpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                });
        }

        @Test
        @DisplayName("should return 422 when contacts have duplicate contact value")
        void should_return_422_when_contacts_have_duplicate_value() {
            final var duplicateContactValue = "johndoe@example.com";
            final var request = DtoBuilder.buildCreateDebtorRequestDto()
                .contacts(List.of(
                    CreateDebtorContactRequestDto.builder()
                        .contactType(ContactType.EMAIL)
                        .contactValue(duplicateContactValue)
                        .priority(1)
                        .build(),
                    CreateDebtorContactRequestDto.builder()
                        .contactType(ContactType.EMAIL)
                        .contactValue(duplicateContactValue)
                        .priority(2)
                        .build()
                ))
                .build();

            webTestClient
                .post()
                .uri(BASE_URL)
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11")
                .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .expectBody(ErrorResponseDto.class)
                .value(response -> {
                    assertThat(response.code()).isEqualTo("DEBTOR_001");
                    assertThat(response.timestamp()).isNotNull();
                    assertThat(response.message()).isEqualTo("Informed contact list has duplicate contact value");
                    assertThat(response.httpStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
                });
        }

        @Test
        @DisplayName("should return 422 when contacts have duplicate priority")
        void should_return_422_when_contacts_have_duplicate_priority() {
            final int duplicatePriority = 1;
            final var request = DtoBuilder.buildCreateDebtorRequestDto()
                .contacts(List.of(
                    CreateDebtorContactRequestDto.builder()
                        .contactType(ContactType.EMAIL)
                        .contactValue("johndoe@example.com")
                        .priority(duplicatePriority)
                        .build(),
                    CreateDebtorContactRequestDto.builder()
                        .contactType(ContactType.EMAIL)
                        .contactValue("janedoe@example.com")
                        .priority(duplicatePriority)
                        .build()
                ))
                .build();

            webTestClient
                .post()
                .uri(BASE_URL)
                .header(RequestConstants.Headers.X_CORRELATION_ID, "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11")
                .header(RequestConstants.Headers.X_CHANNEL, "integration-test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .expectBody(ErrorResponseDto.class)
                .value(response -> {
                    assertThat(response.code()).isEqualTo("DEBTOR_002");
                    assertThat(response.timestamp()).isNotNull();
                    assertThat(response.message()).isEqualTo("Informed contact list has duplicate contact priority");
                    assertThat(response.httpStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
                });
        }

        @Test
        @DisplayName("should return 400 when required headers are missing")
        void should_return_400_when_required_headers_are_missing() {
            final var request = DtoBuilder.buildCreateDebtorRequestDto().build();

            webTestClient
                .post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorResponseDto.class)
                .value(response -> {
                    assertThat(response.code()).isEqualTo("WEB_SUPPORT_002");
                    assertThat(response.timestamp()).isNotNull();
                    assertThat(response.message()).isEqualTo("Some invalid values were sent");
                    assertThat(response.detail())
                        .contains("Missing headers")
                        .contains(RequestConstants.Headers.X_CORRELATION_ID)
                        .contains(RequestConstants.Headers.X_CHANNEL);
                    assertThat(response.httpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                });
        }
    }
}
