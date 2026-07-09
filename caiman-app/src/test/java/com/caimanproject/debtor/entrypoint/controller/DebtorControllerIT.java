package com.caimanproject.debtor.entrypoint.controller;

import com.caimanproject.app.test.IntegrationTestController;
import com.caimanproject.app.test.builder.DtoBuilder;
import com.caimanproject.contracts.util.RequestConstants;
import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.debtor.entrypoint.payload.request.CreateDebtorContactRequestDto;
import com.caimanproject.debtor.entrypoint.payload.response.DebtorResponseDto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

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
            final var request =
                    DtoBuilder.buildCreateDebtorRequestDto().name("").build();

            final var correlationId = "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11";
            final var channel = "integration-test";
            webTestClient
                    .post()
                    .uri(BASE_URL)
                    .header(RequestConstants.Headers.X_CORRELATION_ID, correlationId)
                    .header(RequestConstants.Headers.X_CHANNEL, channel)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                    .jsonPath("$.title").isEqualTo("Field validation failed")
                    .jsonPath("$.detail").isEqualTo("One or more request fields are invalid. See errors for details.")
                    .jsonPath("$.instance").exists()
                    .jsonPath("$.correlationId").isEqualTo(correlationId)
                    .jsonPath("$.channel").isEqualTo(channel)
                    .jsonPath("$.errors").value(List.class, errors -> assertThat(errors).hasSize(1))
                    .jsonPath("$.errors[0].code").isEqualTo("WEB_SUPPORT_002")
                    .jsonPath("$.errors[0].message").isEqualTo("Some invalid values were sent")
                    .jsonPath("$.errors[0].detail").isEqualTo("must not be blank")
                    .jsonPath("$.errors[0].source.body").isEqualTo("$.name");
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
                                    .build()))
                    .build();

            final var correlationId = "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11";
            final var channel = "integration-test";
            webTestClient
                    .post()
                    .uri(BASE_URL)
                    .header(RequestConstants.Headers.X_CORRELATION_ID, correlationId)
                    .header(RequestConstants.Headers.X_CHANNEL, channel)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                    .jsonPath("$.title").isEqualTo("Business rule violation")
                    .jsonPath("$.detail").isEqualTo("One or more business rules were violated. See errors for details.")
                    .jsonPath("$.instance").exists()
                    .jsonPath("$.correlationId").isEqualTo(correlationId)
                    .jsonPath("$.channel").isEqualTo(channel)
                    .jsonPath("$.errors").value(List.class, errors -> assertThat(errors).hasSize(1))
                    .jsonPath("$.errors[0].code").isEqualTo("DEBTOR_BUSINESS_001")
                    .jsonPath("$.errors[0].message").isEqualTo("Informed contact list has duplicate contact value")
                    .jsonPath("$.errors[0].detail").isEqualTo("contactType: EMAIL - priority: 1")
                    .jsonPath("$.errors[0].source.body").isEqualTo("$.contacts[*].contactValue")
                    .jsonPath("$.errors[0].source.invalidValue").isEqualTo(duplicateContactValue);
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
                                    .build()))
                    .build();

            final var correlationId = "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11";
            final var channel = "integration-test";
            webTestClient
                    .post()
                    .uri(BASE_URL)
                    .header(RequestConstants.Headers.X_CORRELATION_ID, correlationId)
                    .header(RequestConstants.Headers.X_CHANNEL, channel)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                    .jsonPath("$.title").isEqualTo("Business rule violation")
                    .jsonPath("$.detail").isEqualTo("One or more business rules were violated. See errors for details.")
                    .jsonPath("$.instance").exists()
                    .jsonPath("$.correlationId").isEqualTo(correlationId)
                    .jsonPath("$.channel").isEqualTo(channel)
                    .jsonPath("$.errors").value(List.class, errors -> assertThat(errors).hasSize(1))
                    .jsonPath("$.errors[0].code").isEqualTo("DEBTOR_BUSINESS_002")
                    .jsonPath("$.errors[0].message").isEqualTo("Informed contact list has duplicate contact priority")
                    .jsonPath("$.errors[0].detail").isEqualTo("contactType: EMAIL - contactValue: johndoe@example.com")
                    .jsonPath("$.errors[0].source.body").isEqualTo("$.contacts[*].priority")
                    .jsonPath("$.errors[0].source.invalidValue").isEqualTo(String.valueOf(duplicatePriority));
        }

        @ParameterizedTest
        @DisplayName("should return 400 when contact value is not a valid email")
        @ValueSource(strings = {"not-a-valid-email", "user@", "@example.com", "user@.com", "user@example"})
        void should_return_400_when_contact_value_is_invalid_email(final String invalidEmail) {
            final var request = DtoBuilder.buildCreateDebtorRequestDto()
                    .contacts(List.of(CreateDebtorContactRequestDto.builder()
                            .contactType(ContactType.EMAIL)
                            .contactValue(invalidEmail)
                            .priority(1)
                            .build()))
                    .build();

            final var correlationId = "bf5ef8a2-5af2-4adf-8b58-d186fe01cd11";
            final var channel = "integration-test";
            webTestClient
                    .post()
                    .uri(BASE_URL)
                    .header(RequestConstants.Headers.X_CORRELATION_ID, correlationId)
                    .header(RequestConstants.Headers.X_CHANNEL, channel)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus()
                    .isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                    .jsonPath("$.title").isEqualTo("Field validation failed")
                    .jsonPath("$.detail").isEqualTo("One or more request fields are invalid. See errors for details.")
                    .jsonPath("$.instance").exists()
                    .jsonPath("$.correlationId").isEqualTo(correlationId)
                    .jsonPath("$.channel").isEqualTo(channel)
                    .jsonPath("$.errors").value(List.class, errors -> assertThat(errors).hasSize(1))
                    .jsonPath("$.errors[0].code").isEqualTo("WEB_SUPPORT_002")
                    .jsonPath("$.errors[0].message").isEqualTo("Some invalid values were sent")
                    .jsonPath("$.errors[0].detail").isEqualTo("must be a valid EMAIL address")
                    .jsonPath("$.errors[0].source.body").isEqualTo("$.contacts[0].contactValue");
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
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
                    .jsonPath("$.title").isEqualTo("Field validation failed")
                    .jsonPath("$.detail").isEqualTo("One or more request fields are invalid. See errors for details.")
                    .jsonPath("$.instance").exists()
                    .jsonPath("$.correlationId").doesNotExist()
                    .jsonPath("$.channel").doesNotExist()
                    .jsonPath("$.errors").value(List.class, errors -> assertThat(errors).hasSize(2))
                    .jsonPath("$.errors[0].code").isEqualTo("WEB_SUPPORT_002")
                    .jsonPath("$.errors[0].message").isEqualTo("Some invalid values were sent")
                    .jsonPath("$.errors[0].detail").isEqualTo("Required field is null/blank")
                    .jsonPath("$.errors[1].code").isEqualTo("WEB_SUPPORT_002")
                    .jsonPath("$.errors[1].message").isEqualTo("Some invalid values were sent")
                    .jsonPath("$.errors[1].detail").isEqualTo("Required field is null/blank")
                    .jsonPath("$.errors[*].source.header")
                    .value(List.class, headers -> assertThat(headers)
                            .containsExactlyInAnyOrder(
                                    RequestConstants.Headers.X_CORRELATION_ID, RequestConstants.Headers.X_CHANNEL));
        }
    }
}
