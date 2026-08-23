package com.example.vibe1.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    List<User> findByDivisionIdAndRole(Long divisionId, Role role);
}
