package com.example.vibe1.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface KeycloakConfigRepository extends JpaRepository<KeycloakConfig, Long> {

    Optional<KeycloakConfig> findFirstByOrderByIdAsc();
}
