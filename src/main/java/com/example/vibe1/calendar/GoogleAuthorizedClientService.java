package com.example.vibe1.calendar;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom {@link OAuth2AuthorizedClientService} backed by {@link GoogleAuthorizedClient},
 * so tokens are encrypted at rest (unlike Spring's own JdbcOAuth2AuthorizedClientService).
 */
@Service
class GoogleAuthorizedClientService implements OAuth2AuthorizedClientService {

    private final GoogleAuthorizedClientRepository repository;
    private final ClientRegistrationRepository clientRegistrationRepository;

    GoogleAuthorizedClientService(GoogleAuthorizedClientRepository repository,
                                   ClientRegistrationRepository clientRegistrationRepository) {
        this.repository = repository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        return (T) repository.findByPrincipalNameAndClientRegistrationId(principalName, clientRegistrationId)
                .map(this::toAuthorizedClient)
                .orElse(null);
    }

    @Override
    @Transactional
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        String registrationId = authorizedClient.getClientRegistration().getRegistrationId();
        GoogleAuthorizedClient entity = repository
                .findByPrincipalNameAndClientRegistrationId(principal.getName(), registrationId)
                .orElseGet(GoogleAuthorizedClient::new);

        entity.setPrincipalName(principal.getName());
        entity.setClientRegistrationId(registrationId);

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        entity.setAccessTokenEnc(accessToken.getTokenValue());
        entity.setAccessTokenIssuedAt(accessToken.getIssuedAt());
        entity.setAccessTokenExpiresAt(accessToken.getExpiresAt());
        entity.setScopes(String.join(",", accessToken.getScopes()));

        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        if (refreshToken != null) {
            entity.setRefreshTokenEnc(refreshToken.getTokenValue());
        }

        repository.save(entity);
    }

    @Override
    @Transactional
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        repository.deleteByPrincipalNameAndClientRegistrationId(principalName, clientRegistrationId);
    }

    private OAuth2AuthorizedClient toAuthorizedClient(GoogleAuthorizedClient entity) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(entity.getClientRegistrationId());

        Set<String> scopes = entity.getScopes() == null
                ? Set.of()
                : Arrays.stream(entity.getScopes().split(",")).collect(Collectors.toSet());

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                entity.getAccessTokenEnc(),
                entity.getAccessTokenIssuedAt(),
                entity.getAccessTokenExpiresAt(),
                scopes);

        OAuth2RefreshToken refreshToken = entity.getRefreshTokenEnc() == null
                ? null
                : new OAuth2RefreshToken(entity.getRefreshTokenEnc(), entity.getAccessTokenIssuedAt());

        return new OAuth2AuthorizedClient(registration, entity.getPrincipalName(), accessToken, refreshToken);
    }
}
