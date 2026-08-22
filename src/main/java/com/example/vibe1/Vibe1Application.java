package com.example.vibe1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Vibe1Application {

	public static void main(String[] args) {
		SpringApplication.run(Vibe1Application.class, args);
	}

}
