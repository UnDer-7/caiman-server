package com.caimanproject.payment.infrastructure.database.entity;

import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import com.caimanproject.payment.core.domain.types.PaymentProofStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "payment_proof",
        indexes = {
            @Index(name = "idx_proof_upload_token", columnList = "upload_token"),
            @Index(name = "idx_proof_invoice", columnList = "invoice_id")
        })
public class PaymentProofEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "file_content_type", length = 100, nullable = false)
    private String fileContentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "upload_token", length = 500, nullable = false)
    private String uploadToken;

    @Column(name = "ai_extracted_value", precision = 15, scale = 2)
    private BigDecimal aiExtractedValue;

    @Column(name = "final_value", precision = 15, scale = 2)
    private BigDecimal finalValue;

    @Builder.Default
    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview = false;

    @Column(name = "ai_raw_response", columnDefinition = "text")
    private String aiRawResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private PaymentProofStatus status;

    @Embedded
    private AuditEmbeddable audit;
}
