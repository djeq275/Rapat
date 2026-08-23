package com.example.vibe1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full context smoke test against a real MariaDB container: exercises the
 * whole Liquibase changelog chain (constraints included) and security wiring
 * on startup, not just Hibernate's own schema generation.
 */
@Testcontainers
@SpringBootTest
class Vibe1ApplicationTests {

	@Container
	@ServiceConnection
	static final MariaDBContainer<?> mariaDb = new MariaDBContainer<>("mariadb:11");

	@Test
	void contextLoads() {
	}

}
