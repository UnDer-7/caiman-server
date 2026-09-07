package com.caimanproject.debtor.infrastructure.database.entity;

import com.caimanproject.debtor.core.domain.types.ContactType;
import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "debtor_contact",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_dc_debtor_type_value",
                    columnNames = {"debtor_id", "contact_type", "contact_value"}),
            @UniqueConstraint(
                    name = "uq_dc_debtor_type_priority",
                    columnNames = {"debtor_id", "contact_type", "priority"})
        })
public class DebtorContactEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debtor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dc_debtor"))
    private DebtorEntity debtor;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", length = 50, nullable = false)
    private ContactType contactType;

    @Column(name = "contact_value", length = 500, nullable = false)
    private String contactValue;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private int priority = 1;

    @Embedded
    private AuditEmbeddable audit;
}
