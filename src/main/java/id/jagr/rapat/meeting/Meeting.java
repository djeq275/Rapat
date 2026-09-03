package id.jagr.rapat.meeting;

import java.time.Instant;
import java.time.LocalDateTime;

import id.jagr.rapat.common.AuditableEntity;
import id.jagr.rapat.division.Division;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meeting")
@Getter
@Setter
@NoArgsConstructor
public class Meeting extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "material_link")
    private String materialLink;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;

    /** Must be the Ketua Divisi of {@link #division} -- Admin does not create meetings. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "notulensi_link")
    private String notulensiLink;

    @Lob
    @Column(name = "notulensi_text", columnDefinition = "LONGTEXT")
    private String notulensiText;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_sync_status", nullable = false, length = 16)
    private CalendarSyncStatus calendarSyncStatus = CalendarSyncStatus.PENDING;

    @Lob
    @Column(name = "calendar_sync_error", columnDefinition = "LONGTEXT")
    private String calendarSyncError;

    @Column(name = "calendar_synced_at")
    private Instant calendarSyncedAt;

    public LocalDateTime getStartTimeLocal() {
        return startTime == null ? null : startTime.atZone(MeetingTimeZone.WIB).toLocalDateTime();
    }

    public LocalDateTime getEndTimeLocal() {
        return endTime == null ? null : endTime.atZone(MeetingTimeZone.WIB).toLocalDateTime();
    }
}
