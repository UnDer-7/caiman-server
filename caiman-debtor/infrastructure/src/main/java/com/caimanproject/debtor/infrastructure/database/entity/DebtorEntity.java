package com.caimanproject.debtor.infrastructure.database.entity;

import com.caimanproject.jpa.AuditEmbeddable;
import com.caimanproject.jpa.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
@Table(name = "debtor")
public class DebtorEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Embedded
    private AuditEmbeddable audit;

    @Builder.Default
    @OneToMany(mappedBy = "debtor", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DebtorContactEntity> contacts = new ArrayList<>();

    public void addContact(final DebtorContactEntity contact) {
        if (contact != null) {
            contact.setDebtor(this);
            contacts.add(contact);
        }
    }

    public void addContacts(final Collection<DebtorContactEntity> contacts) {
        if (contacts != null && !contacts.isEmpty()) {
            contacts.forEach(this::addContact);
        }
    }
}
