package id.jagr.rapat.calendar;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface GoogleAuthorizedClientRepository extends JpaRepository<GoogleAuthorizedClient, Long> {

    Optional<GoogleAuthorizedClient> findByPrincipalNameAndClientRegistrationId(String principalName, String clientRegistrationId);

    void deleteByPrincipalNameAndClientRegistrationId(String principalName, String clientRegistrationId);
}
