package com.example.vibe1.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    List<User> findByDivisionIdAndRole(Long divisionId, Role role);

    /**
     * spring.jpa.open-in-view is disabled, so division (LAZY, and nullable
     * for Admin/Direktur -- hence LEFT JOIN) must be fetched here for any
     * caller that renders it outside the request's own transaction (i.e.
     * from a controller, not from inside a @Transactional service method).
     */
    @Query("select u from User u left join fetch u.division order by u.email")
    List<User> findAllWithDivision();

    @Query("select u from User u left join fetch u.division where u.id = :id")
    Optional<User> findByIdWithDivision(Long id);
}
