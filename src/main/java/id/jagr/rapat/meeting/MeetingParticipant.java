package id.jagr.rapat.meeting;

import id.jagr.rapat.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "meeting_participant",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_participant", columnNames = {"meeting_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "added_reason", nullable = false, length = 16)
    private ParticipantAddedReason addedReason;

    public MeetingParticipant(Meeting meeting, User user, ParticipantAddedReason addedReason) {
        this.meeting = meeting;
        this.user = user;
        this.addedReason = addedReason;
    }
}
