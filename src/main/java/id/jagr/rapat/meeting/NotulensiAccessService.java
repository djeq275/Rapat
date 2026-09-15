package id.jagr.rapat.meeting;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import id.jagr.rapat.user.User;

import lombok.RequiredArgsConstructor;

/**
 * Per-meeting notulensi permission -- deliberately a relation
 * ({@link MeetingNotetaker}), not a global RBAC role. The Ketua Divisi of a
 * meeting's division (or Admin) can always write and manage notetakers for
 * it without being granted anything; an assigned notetaker can only write,
 * not assign others.
 */
@Service
@RequiredArgsConstructor
public class NotulensiAccessService {

    private final MeetingNotetakerRepository notetakerRepository;

    public boolean canWrite(User user, Meeting meeting) {
        return isManager(user, meeting)
                || notetakerRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId());
    }

    public void assertCanWrite(User user, Meeting meeting) {
        if (!canWrite(user, meeting)) {
            throw new AccessDeniedException("Anda tidak berhak mengubah notulensi rapat ini");
        }
    }

    public boolean canManageNotetakers(User user, Meeting meeting) {
        return isManager(user, meeting);
    }

    public void assertCanManageNotetakers(User user, Meeting meeting) {
        if (!canManageNotetakers(user, meeting)) {
            throw new AccessDeniedException("Anda tidak berhak menunjuk notulis untuk rapat ini");
        }
    }

    /**
     * A role with {@code canOrganizeMeetings} manages every meeting if it also
     * {@code canViewAllDivisions} (Admin's case today -- no division check at all), otherwise
     * only meetings of its own division (Ketua Divisi's case today -- division must match).
     */
    private boolean isManager(User user, Meeting meeting) {
        if (!user.getRole().isCanOrganizeMeetings()) {
            return false;
        }
        if (user.getRole().isCanViewAllDivisions()) {
            return true;
        }
        return user.getDivision() != null
                && user.getDivision().getId().equals(meeting.getDivision().getId());
    }
}
