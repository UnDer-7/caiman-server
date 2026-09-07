package com.caimanproject.payment.entrypoint.controller;

import com.caimanproject.app.test.IntegrationTestController;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

class PublicProofControllerIT extends IntegrationTestController {

    static final String BASE_URL = "/public/proofs";

    UUID debtorId;
    UUID chargePlanId;
    UUID chargePlanMemberId;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE debtor, charge_plan CASCADE");

        debtorId = UUID.randomUUID();
        chargePlanId = UUID.randomUUID();
        chargePlanMemberId = UUID.randomUUID();
        final var now = Timestamp.from(Instant.now());

        jdbcTemplate.update("""
                INSERT INTO debtor (id, name, notifications_enabled, is_active, created_at, updated_at)
                VALUES (?,?,?,?,?,?)
                """, debtorId.toString(), "John Doe", true, true, now, now);

        jdbcTemplate.update(
                """
                INSERT INTO charge_plan (id, name, type, status, proof_validation_mode, total_amount,
                    due_tolerance_days, cycle_unit, cycle_interval, cycle_anchor_date, notifications_enabled,
                    notification_time, notification_timezone, starts_at, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                chargePlanId.toString(),
                "Shared Plan",
                "SPLIT",
                "ACTIVE",
                "MANUAL",
                new BigDecimal("100.00"),
                3,
                "MONTHLY",
                1,
                Date.valueOf(LocalDate.now()),
                true,
                Time.valueOf(LocalTime.of(9, 0)),
                "UTC",
                now,
                now,
                now);

        jdbcTemplate.update(
                """
                INSERT INTO charge_plan_member (id, charge_plan_id, debtor_id, status, credit_balance,
                    joined_at, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                chargePlanMemberId.toString(),
                chargePlanId.toString(),
                debtorId.toString(),
                "ACTIVE",
                BigDecimal.ZERO,
                now,
                now,
                now);
    }

    private UUID createInvoice(final String status, final BigDecimal amountDue, final BigDecimal amountPaid) {
        return createInvoice(status, amountDue, amountPaid, chargePlanId, chargePlanMemberId);
    }

    private UUID createInvoice(
            final String status,
            final BigDecimal amountDue,
            final BigDecimal amountPaid,
            final UUID planId,
            final UUID memberId) {
        final var invoiceId = UUID.randomUUID();
        final var token = UUID.randomUUID();
        final var now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
                """
                INSERT INTO invoice (id, charge_plan_id, charge_plan_member_id, cycle_index, generation_date,
                    amount_due, amount_paid, status, due_date, upload_token, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                invoiceId.toString(),
                planId.toString(),
                memberId.toString(),
                1L,
                Date.valueOf(LocalDate.now()),
                amountDue,
                amountPaid,
                status,
                now,
                token.toString(),
                now,
                now);

        return token;
    }

    private UUID[] createChargePlanWithMode(final String proofValidationMode) {
        final var planId = UUID.randomUUID();
        final var memberId = UUID.randomUUID();
        final var now = Timestamp.from(Instant.now());

        jdbcTemplate.update(
                """
                INSERT INTO charge_plan (id, name, type, status, proof_validation_mode, total_amount,
                    due_tolerance_days, cycle_unit, cycle_interval, cycle_anchor_date, notifications_enabled,
                    notification_time, notification_timezone, starts_at, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                planId.toString(),
                "Plan " + proofValidationMode,
                "SPLIT",
                "ACTIVE",
                proofValidationMode,
                new BigDecimal("100.00"),
                3,
                "MONTHLY",
                1,
                Date.valueOf(LocalDate.now()),
                true,
                Time.valueOf(LocalTime.of(9, 0)),
                "UTC",
                now,
                now,
                now);

        jdbcTemplate.update(
                """
                INSERT INTO charge_plan_member (id, charge_plan_id, debtor_id, status, credit_balance,
                    joined_at, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                memberId.toString(),
                planId.toString(),
                debtorId.toString(),
                "ACTIVE",
                BigDecimal.ZERO,
                now,
                now,
                now);

        return new UUID[] {planId, memberId};
    }

    @Nested
    @DisplayName("GET /public/proofs - getProofPage")
    class GetProofPage {

        @Test
        @DisplayName("should return 200 with invoice data and upload form when the invoice is open")
        void should_return_page_with_form_when_invoice_is_open() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            webTestClient
                    .get()
                    .uri(BASE_URL + "?token=" + token)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectHeader()
                    .contentTypeCompatibleWith(MediaType.TEXT_HTML)
                    .expectBody(String.class)
                    .value(body -> {
                        assertThat(body).contains("Shared Plan");
                        assertThat(body).contains("John Doe");
                        assertThat(body).contains("100.00");
                        assertThat(body).contains("id=\"proof-form\"");
                    });
        }

        @Test
        @DisplayName("should return 200 without the form and with a status message when invoice is already paid")
        void should_return_page_without_form_when_invoice_is_paid() {
            final var token = createInvoice("PAID", new BigDecimal("100.00"), new BigDecimal("100.00"));

            webTestClient
                    .get()
                    .uri(BASE_URL + "?token=" + token)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(String.class)
                    .value(body -> {
                        assertThat(body).doesNotContain("id=\"proof-form\"");
                        assertThat(body).contains("This invoice has already been paid.");
                    });
        }

        @Test
        @DisplayName("should return 404 with the not-found page for an unknown token")
        void should_return_not_found_page_for_unknown_token() {
            webTestClient
                    .get()
                    .uri(BASE_URL + "?token=" + UUID.randomUUID())
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .value(body -> assertThat(body).contains("Link not found"));
        }

        @Test
        @DisplayName("should return 404 with the not-found page for a malformed token")
        void should_return_not_found_page_for_malformed_token() {
            webTestClient
                    .get()
                    .uri(BASE_URL + "?token=not-a-uuid")
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .value(body -> assertThat(body).contains("Link not found"));
        }

        @Test
        @DisplayName("should succeed without X-Correlation-ID/X-Channel headers")
        void should_succeed_without_required_headers() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            webTestClient
                    .get()
                    .uri(BASE_URL + "?token=" + token)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }
    }

    @Nested
    @DisplayName("POST /public/proofs - uploadProof")
    class UploadProof {

        @Test
        @DisplayName("should return 202 with proofId and pending-review message for a well-formed MANUAL request")
        void should_return_202_for_well_formed_request() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(202)
                    .expectBody()
                    .jsonPath("$.proofId")
                    .exists()
                    .jsonPath("$.message")
                    .isEqualTo("Your payment proof has been received and is being reviewed. "
                            + "You will be notified of the result.");

            final var proofCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM payment_proof WHERE status = 'PENDING_MANUAL_REVIEW'", Integer.class);
            assertThat(proofCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should return 422 when a proof is already pending review for the invoice")
        void should_return_422_when_active_proof_already_exists() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            final var firstUpload = new MultipartBodyBuilder();
            firstUpload
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            firstUpload.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(firstUpload.build()))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(202);

            final var secondUpload = new MultipartBodyBuilder();
            secondUpload
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof2.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            secondUpload.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(secondUpload.build()))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(422)
                    .expectBody()
                    .jsonPath("$.errors[0].code")
                    .isEqualTo("PAYMENT_002");
        }

        @Test
        @DisplayName("should return 422 when the invoice is already paid")
        void should_return_422_when_invoice_is_not_payable() {
            final var token = createInvoice("PAID", new BigDecimal("100.00"), new BigDecimal("100.00"));

            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .isEqualTo(422)
                    .expectBody()
                    .jsonPath("$.errors[0].code")
                    .isEqualTo("PAYMENT_003");
        }

        @Test
        @DisplayName("should return 500 when the charge plan uses an unimplemented validation mode (AI_AUTO)")
        void should_return_500_for_ai_auto_mode() {
            final var planAndMember = createChargePlanWithMode("AI_AUTO");
            final var token = createInvoice(
                    "SENT", new BigDecimal("100.00"), BigDecimal.ZERO, planAndMember[0], planAndMember[1]);

            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .is5xxServerError();
        }

        @Test
        @DisplayName("should return 404 with a standard JSON error body for an unknown token")
        void should_return_404_for_unknown_token() {
            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "TOTAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + UUID.randomUUID())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .expectBody()
                    .jsonPath("$.title")
                    .isEqualTo("Not Found");
        }

        @Test
        @DisplayName("should return 400 when paymentType is PARTIAL and declaredAmount is missing")
        void should_return_400_when_partial_without_declared_amount() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "PARTIAL");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .isBadRequest()
                    .expectBody()
                    .jsonPath("$.title")
                    .isEqualTo("Field validation failed")
                    .jsonPath("$.errors[0].source.queryParameter")
                    .isEqualTo("declaredAmount");
        }

        @Test
        @DisplayName("should return 400 when paymentType is PARTIAL and declaredAmount is zero")
        void should_return_400_when_partial_with_zero_declared_amount() {
            final var token = createInvoice("SENT", new BigDecimal("100.00"), BigDecimal.ZERO);

            final var multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder
                    .part("file", new ByteArrayResource("fake-image-bytes".getBytes()) {
                        @Override
                        public String getFilename() {
                            return "proof.png";
                        }
                    })
                    .contentType(MediaType.IMAGE_PNG);
            multipartBodyBuilder.part("paymentType", "PARTIAL");
            multipartBodyBuilder.part("declaredAmount", "0");

            webTestClient
                    .post()
                    .uri(BASE_URL + "?token=" + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .exchange()
                    .expectStatus()
                    .isBadRequest()
                    .expectBody()
                    .jsonPath("$.errors[0].source.queryParameter")
                    .isEqualTo("declaredAmount");
        }
    }
}
