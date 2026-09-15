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
 * A role a {@link User} can have -- the 4 built-in rows (see {@link BuiltInRoleNames}) are
 * seeded by Liquibase and marked {@link #builtIn}, so they can never be renamed/deleted from
 * the admin UI (issue #46); Admin-created custom roles are ordinary non-built-in rows.
 *
 * <p>The 3 behavior flags below replace what used to be hardcoded checks against specific
 * role names (issue #48) -- they apply uniformly to built-in and custom roles alike. See
 * {@code db/changelog/014-create-app-role-table.xml} for the seed values that reproduce
 * today's Admin/Direktur/Ketua Divisi/Karyawan behavior exactly.
 */
@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
public class AppRole extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** True for the 4 seeded rows -- can't be renamed or deleted via the admin UI (issue #46). */
    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    /** Whether a User with this role must (Ketua Divisi/Karyawan) or must not (Admin/Direktur) have a division. */
    @Column(name = "requires_division", nullable = false)
    private boolean requiresDivision;

    /** Direktur's behavior today: every new meeting auto-invites every user with this flag set. */
    @Column(name = "auto_invite_to_all_meetings", nullable = false)
    private boolean autoInviteToAllMeetings;

    /** Direktur/Admin's behavior today: see meetings from every division, not just their own. */
    @Column(name = "can_view_all_divisions", nullable = false)
    private boolean canViewAllDivisions;

    /** Ketua Divisi's behavior today: can create meetings for, and manage notulensi of, their own division. */
    @Column(name = "can_organize_meetings", nullable = false)
    private boolean canOrganizeMeetings;

    public AppRole(String name, boolean builtIn, boolean requiresDivision, boolean autoInviteToAllMeetings,
                   boolean canViewAllDivisions, boolean canOrganizeMeetings) {
        this.name = name;
        this.builtIn = builtIn;
        this.requiresDivision = requiresDivision;
        this.autoInviteToAllMeetings = autoInviteToAllMeetings;
        this.canViewAllDivisions = canViewAllDivisions;
        this.canOrganizeMeetings = canOrganizeMeetings;
    }
}
