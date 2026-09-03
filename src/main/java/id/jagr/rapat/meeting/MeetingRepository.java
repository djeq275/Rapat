package id.jagr.rapat.meeting;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    /**
     * spring.jpa.open-in-view is disabled, so division/organizer (both LAZY)
     * must be fetched here -- they're rendered directly in list/detail templates.
     */
    @Query("select m from Meeting m join fetch m.division join fetch m.organizer where m.division.id = :divisionId order by m.startTime desc")
    List<Meeting> findByDivisionIdOrderByStartTimeDesc(Long divisionId);

    @Query("select m from Meeting m join fetch m.division join fetch m.organizer order by m.startTime desc")
    List<Meeting> findAllByOrderByStartTimeDesc();

    @Query("select m from Meeting m join fetch m.division join fetch m.organizer where m.id = :id")
    Optional<Meeting> findDetailById(Long id);
}
