package id.jagr.rapat.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

/**
 * One-time migration for installs that already had real Google OAuth
 * credentials in {@code .env} before {@link GoogleOAuthConfig} existed
 * (issue #32) -- without this, deploying issue #31/#32 to a running install
 * would silently break Google login and Calendar sync until an Admin
 * remembered to fill in the new settings form.
 *
 * <p>Runs once per boot, but only actually does anything the very first
 * time: if {@link GoogleOAuthConfigService#isConfigured()} is already
 * {@code true} (seeded by a previous boot, or filled in by Admin), this is a
 * no-op forever after -- the database becomes the sole source of truth, and
 * {@code GOOGLE_OAUTH_CLIENT_ID}/{@code GOOGLE_OAUTH_CLIENT_SECRET} (if still
 * present in the environment) are ignored from then on, not resynced.
 *
 * <p>Reads the raw environment variables directly (not through
 * {@code application.properties}, which no longer references them) --
 * {@link Environment} exposes OS environment variables regardless of
 * whether any property file mentions them.
 */
@Component
@RequiredArgsConstructor
class GoogleOAuthConfigBootstrap implements ApplicationRunner {

    private final GoogleOAuthConfigService googleOAuthConfigService;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (googleOAuthConfigService.isConfigured()) {
            return;
        }
        String clientId = environment.getProperty("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = environment.getProperty("GOOGLE_OAUTH_CLIENT_SECRET");
        if (StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret)) {
            googleOAuthConfigService.save(clientId, clientSecret);
        }
    }
}
