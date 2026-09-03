package com.example.vibe1.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthConfigBootstrapTest {

    @Mock
    GoogleOAuthConfigService googleOAuthConfigService;
    @Mock
    Environment environment;

    GoogleOAuthConfigBootstrap bootstrap;

    @Test
    void seedsConfigFromEnvWhenTableEmptyAndEnvVarsPresent() {
        bootstrap = new GoogleOAuthConfigBootstrap(googleOAuthConfigService, environment);
        when(googleOAuthConfigService.isConfigured()).thenReturn(false);
        when(environment.getProperty("GOOGLE_OAUTH_CLIENT_ID")).thenReturn("env-client-id");
        when(environment.getProperty("GOOGLE_OAUTH_CLIENT_SECRET")).thenReturn("env-client-secret");

        bootstrap.run(new DefaultApplicationArguments());

        verify(googleOAuthConfigService).save("env-client-id", "env-client-secret");
    }

    @Test
    void doesNothingWhenAlreadyConfigured() {
        bootstrap = new GoogleOAuthConfigBootstrap(googleOAuthConfigService, environment);
        when(googleOAuthConfigService.isConfigured()).thenReturn(true);

        bootstrap.run(new DefaultApplicationArguments());

        verify(googleOAuthConfigService, never()).save(any(), any());
    }

    @Test
    void doesNothingWhenEnvVarsAbsentEvenIfTableEmpty() {
        bootstrap = new GoogleOAuthConfigBootstrap(googleOAuthConfigService, environment);
        when(googleOAuthConfigService.isConfigured()).thenReturn(false);
        lenient().when(environment.getProperty("GOOGLE_OAUTH_CLIENT_ID")).thenReturn(null);
        lenient().when(environment.getProperty("GOOGLE_OAUTH_CLIENT_SECRET")).thenReturn(null);

        bootstrap.run(new DefaultApplicationArguments());

        verify(googleOAuthConfigService, never()).save(any(), any());
    }

    @Test
    void doesNothingWhenOnlyOneEnvVarPresent() {
        bootstrap = new GoogleOAuthConfigBootstrap(googleOAuthConfigService, environment);
        when(googleOAuthConfigService.isConfigured()).thenReturn(false);
        when(environment.getProperty("GOOGLE_OAUTH_CLIENT_ID")).thenReturn("env-client-id");
        lenient().when(environment.getProperty("GOOGLE_OAUTH_CLIENT_SECRET")).thenReturn(null);

        bootstrap.run(new DefaultApplicationArguments());

        verify(googleOAuthConfigService, never()).save(any(), any());
    }
}
