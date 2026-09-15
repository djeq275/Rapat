package id.jagr.rapat.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * role is LAZY and never null -- eagerly fetched here because every login path
     * (form/Google/Keycloak) calls this to build the security principal, and
     * UserPrincipal.getAuthorities() reads user.getRole() *after* the request's own
     * transaction (if any) has already closed, so a plain derived query would throw
     * LazyInitializationException on the very first authenticated request.
     */
    @Query("select u from User u join fetch u.role where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    List<User> findByRole_AutoInviteToAllMeetingsTrue();

    List<User> findByDivisionIdAndRole(Long divisionId, AppRole role);

    boolean existsByRole(AppRole role);

    long countByRole(AppRole role);

    /**
     * spring.jpa.open-in-view is disabled, so division (LAZY, and nullable
     * for Admin/Direktur -- hence LEFT JOIN) and role (LAZY, never null --
     * hence plain JOIN) must be fetched here for any caller that renders
     * them outside the request's own transaction (i.e. from a controller,
     * not from inside a @Transactional service method).
     */
    @Query("select u from User u join fetch u.role left join fetch u.division order by u.email")
    List<User> findAllWithDivision();

    @Query("select u from User u join fetch u.role left join fetch u.division where u.id = :id")
    Optional<User> findByIdWithDivision(Long id);
}
