package expense_tracker.config;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import expense_tracker.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central Spring Security configuration for the Expense Tracker API.
 * <p>
 * ================================ WARNING ================================
 * This configuration currently permits ALL requests without authentication
 * ({@code anyRequest().permitAll()}). This is a deliberate, TEMPORARY state
 * to unblock backend development before JWT authentication exists. It MUST
 * NOT reach a production or publicly-reachable environment in this state —
 * every endpoint, including future user/expense/category endpoints, is
 * currently unauthenticated and unauthorized.
 * ===========================================================================
 */
@Configuration
public class SecurityConfig {

    /**
     * Provides the password hashing strategy used by {@code UserService}.
     * BCrypt is adaptive (configurable work factor) and salts each hash
     * automatically, making it the current industry-standard choice for
     * password storage. This bean is what resolves UserService's
     * previously-unsatisfied PasswordEncoder dependency.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the HTTP security filter chain using the modern lambda DSL.
     * {@code WebSecurityConfigurerAdapter} is not used here — it was
     * removed in Spring Security 6.x; the SecurityFilterChain bean is the
     * only supported approach going forward.
     *
     * <p><b>CSRF is disabled</b> because this API has no cookie/session-based
     * authentication mechanism yet — CSRF protection exists specifically to
     * defend session-authenticated browser flows against forged requests,
     * and there is currently no session to forge against. This decision
     * must be re-evaluated if a future design ever stores auth tokens in
     * cookies rather than an Authorization header.
     *
     * <p><b>Session policy is set to STATELESS</b> in anticipation of JWT
     * authentication, even though no token mechanism exists yet. This
     * avoids the server creating and discarding HttpSessions today, and
     * keeps this config aligned with the app's intended final architecture
     * rather than requiring a session-policy change bundled into a future,
     * more complex JWT-related pull request.
     *
     * <p><b>{@code anyRequest().permitAll()}</b> is a temporary placeholder
     * — see the class-level warning. This must be replaced with explicit
     * authorization rules once JWT authentication is implemented.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
        );
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
