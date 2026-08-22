package com.example.vibe1.division;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    Optional<Division> findByNameIgnoreCase(String name);
}
