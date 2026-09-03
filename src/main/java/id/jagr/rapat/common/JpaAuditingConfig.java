package id.jagr.rapat.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kept off {@code RapatApplication} itself: a {@code @WebMvcTest} slice still
 * uses that class as its {@code @SpringBootConfiguration} source (just without
 * full component scanning), so {@code @EnableJpaAuditing} declared there would
 * force JPA auditing beans into JPA-less web slices and fail with
 * "JPA metamodel must not be empty". As an ordinary {@code @Configuration}
 * bean here, slice tests' type-exclude filters correctly skip it.
 */
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {
}
