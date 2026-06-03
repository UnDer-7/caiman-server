package com.caimanproject.debtor.infrastructure.database.entity;

import com.caimanproject.debtor.core.ContactType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "debtor_contact",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_dc_debtor_type_value",
        columnNames = {"debtor_id", "contact_type", "contact_value"}
    )
)
public class DebtorContactEntity {

    @Id
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

    @Column(name = "priority", nullable = false)
    private int priority = 1;

    protected DebtorContactEntity() {}

    public DebtorContactEntity(String id, DebtorEntity debtor, ContactType contactType,
                               String contactValue, int priority) {
        this.id = id;
        this.debtor = debtor;
        this.contactType = contactType;
        this.contactValue = contactValue;
        this.priority = priority;
    }

    public String getId() { return id; }
    public DebtorEntity getDebtor() { return debtor; }
    public ContactType getContactType() { return contactType; }
    public String getContactValue() { return contactValue; }
    public void setContactValue(String contactValue) { this.contactValue = contactValue; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
