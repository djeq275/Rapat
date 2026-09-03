package id.jagr.rapat.division;

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

@Entity
@Table(name = "division")
@Getter
@Setter
@NoArgsConstructor
public class Division extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Plain FK column (no {@code @ManyToOne}) on purpose: User already references
     * Division, so a JPA relationship in this direction would create a
     * division-user module cycle.
     */
    @Column(name = "ketua_divisi_user_id")
    private Long ketuaDivisiUserId;

    public Division(String name) {
        this.name = name;
    }
}
