package id.jagr.rapat.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByNameIgnoreCase(String name);

    /**
     * capabilities is LAZY, so it must be fetched here for RoleAdminController/role-list.html,
     * which render outside a transaction (spring.jpa.open-in-view=false) -- same pattern as
     * UserRepository.findAllWithDivision().
     */
    @Query("select distinct r from AppRole r left join fetch r.capabilities")
    List<AppRole> findAllWithCapabilities();

    @Query("select r from AppRole r left join fetch r.capabilities where r.id = :id")
    Optional<AppRole> findByIdWithCapabilities(@Param("id") Long id);
}
