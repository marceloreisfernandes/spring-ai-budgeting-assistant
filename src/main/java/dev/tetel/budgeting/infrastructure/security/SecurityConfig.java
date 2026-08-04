package dev.tetel.budgeting.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless HTTP Basic authentication for this educational project. Every {@code /api/v1/**}
 * endpoint requires authentication; there is a single configured principal, sourced from the
 * environment (never hardcoded, never the Spring Boot auto-generated development password).
 *
 * <p>CSRF is disabled deliberately: this API is stateless, uses HTTP Basic (not cookie/session
 * based) authentication, and serves no browser form. CSRF exploits ambient, browser-attached
 * session cookies; without cookie-based sessions there is nothing for a forged cross-site request
 * to ride on, and every real request must carry an explicit Authorization header a forged page
 * cannot obtain. See docs/security/security-controls.md for the full write-up.
 *
 * <p>HTTP Basic sends credentials base64-encoded on every request; outside localhost this must
 * only ever run behind HTTPS/TLS, or credentials are exposed in transit.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${app.security.username}") String username,
            @Value("${app.security.password}") String password,
            PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
