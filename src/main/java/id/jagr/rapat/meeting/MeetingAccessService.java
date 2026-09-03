package id.jagr.rapat.meeting;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import id.jagr.rapat.division.Division;
import id.jagr.rapat.user.Role;
import id.jagr.rapat.user.User;

import lombok.RequiredArgsConstructor;

/**
 * Karyawan see only their own division's meetings; Direktur/Admin see all.
 * {@link #assertCanView} must be called on every direct meeting-id access
 * (not just list rendering) -- otherwise a Karyawan can reach another
 * division's meeting by guessing/typing its id.
 */
@Service
@RequiredArgsConstructor
public class MeetingAccessService {

    private final MeetingRepository meetingRepository;

    public List<Meeting> visibleMeetings(User viewer) {
        if (isCompanyWide(viewer)) {
            return meetingRepository.findAllByOrderByStartTimeDesc();
        }
        return meetingRepository.findByDivisionIdOrderByStartTimeDesc(requireDivision(viewer).getId());
    }

    public void assertCanView(User viewer, Meeting meeting) {
        if (isCompanyWide(viewer)) {
            return;
        }
        Division viewerDivision = requireDivision(viewer);
        if (!viewerDivision.getId().equals(meeting.getDivision().getId())) {
            throw new AccessDeniedException("Anda tidak berhak melihat rapat ini");
        }
    }

    private boolean isCompanyWide(User viewer) {
        return viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.DIREKTUR;
    }

    private Division requireDivision(User viewer) {
        if (viewer.getDivision() == null) {
            throw new AccessDeniedException("Anda tidak terikat ke divisi manapun");
        }
        return viewer.getDivision();
    }
}
