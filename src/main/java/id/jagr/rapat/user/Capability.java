package id.jagr.rapat.user;

import id.jagr.rapat.common.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A page/menu-level admin capability an {@link AppRole} can be granted (issue #46) -- seeded by
 * Liquibase, one row per existing {@code @PreAuthorize("hasRole('ADMIN')")} admin screen (see
 * issue #43's inventory). Deliberately not Admin-editable: this catalog only grows when new
 * admin features ship in code, unlike a role's name or its capability assignments.
 */
@Entity
@Table(name = "capability")
@Getter
@Setter
@NoArgsConstructor
public class Capability extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String label;
}
