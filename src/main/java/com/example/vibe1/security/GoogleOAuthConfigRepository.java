package com.example.vibe1.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface GoogleOAuthConfigRepository extends JpaRepository<GoogleOAuthConfig, Long> {

    Optional<GoogleOAuthConfig> findFirstByOrderByIdAsc();
}
