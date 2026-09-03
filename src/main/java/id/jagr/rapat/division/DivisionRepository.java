package id.jagr.rapat.division;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    Optional<Division> findByNameIgnoreCase(String name);
}
