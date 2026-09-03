package id.jagr.rapat.meeting;

import java.time.Instant;

import id.jagr.rapat.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A per-meeting grant of notulensi write access -- deliberately a relation,
 * not a global RBAC role. The Ketua Divisi of a meeting's division always has
 * write access regardless of whether they hold a row here.
 */
@Entity
@Table(name = "meeting_notetaker",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_notetaker", columnNames = {"meeting_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MeetingNotetaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    public MeetingNotetaker(Meeting meeting, User user, User grantedBy, Instant grantedAt) {
        this.meeting = meeting;
        this.user = user;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
    }
}
