package id.jagr.rapat.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Kept as its own no-dependency bean (not a field on this class) so that
     * GoogleOidcUserService can depend on it without a construction cycle with
     * filterChain(), which in turn depends on GoogleOidcUserService.
     */
    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AppOidcUserService appOidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/webjars/**", "/css/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error=credentials")
                        .defaultSuccessUrl("/", false))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .failureUrl("/login?error=oauth2")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(appOidcUserService))
                        .defaultSuccessUrl("/", false))
                // Handles the "google-calendar" registration's authorization-code
                // callback (see its redirect-uri override in
                // DynamicClientRegistrationRepository) as a plain incremental-scope
                // grant, not a login attempt -- oauth2Login() alone only processes
                // /login/oauth2/code/**.
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/error/403"));
        return http.build();
    }
}
