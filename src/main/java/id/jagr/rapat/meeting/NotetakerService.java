package id.jagr.rapat.meeting;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.jagr.rapat.user.User;
import id.jagr.rapat.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotetakerService {

    private final MeetingParticipantRepository participantRepository;
    private final MeetingNotetakerRepository notetakerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MeetingNotetaker> findNotetakers(Long meetingId) {
        return notetakerRepository.findByMeetingId(meetingId);
    }

    /** Participants not yet a notetaker for this meeting -- who a manager could still assign. */
    @Transactional(readOnly = true)
    public List<User> findEligibleCandidates(Long meetingId) {
        Set<Long> alreadyNotetakers = notetakerRepository.findByMeetingId(meetingId).stream()
                .map(n -> n.getUser().getId())
                .collect(Collectors.toSet());
        return participantRepository.findByMeetingId(meetingId).stream()
                .map(MeetingParticipant::getUser)
                .filter(user -> !alreadyNotetakers.contains(user.getId()))
                .toList();
    }

    @Transactional
    public void assign(Meeting meeting, Long userId, Long grantedByUserId) {
        if (notetakerRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            return;
        }
        if (!participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            throw new IllegalArgumentException("Hanya peserta rapat yang bisa ditunjuk sebagai notulis");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));
        User grantedBy = userRepository.findById(grantedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));
        notetakerRepository.save(new MeetingNotetaker(meeting, user, grantedBy, Instant.now()));
    }

    @Transactional
    public void revoke(Long meetingId, Long userId) {
        notetakerRepository.findByMeetingIdAndUserId(meetingId, userId)
                .ifPresent(notetakerRepository::delete);
    }
}
