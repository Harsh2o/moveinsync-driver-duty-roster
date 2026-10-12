package com.moveinsync.roster.config;

import com.moveinsync.roster.security.CustomUserDetailsService;
import com.moveinsync.roster.security.JwtAuthenticationEntryPoint;
import com.moveinsync.roster.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/drivers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/drivers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/drivers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/duties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/duties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/duties/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/roster/generate").hasRole("ADMIN")
                        .requestMatchers("/api/v1/roster/reassign").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // Admin and Approver
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "APPROVER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/duties/**").hasAnyRole("ADMIN", "APPROVER", "EMPLOYEE")

                        // Read access
                        .requestMatchers(HttpMethod.GET, "/api/v1/drivers/**").hasAnyRole("ADMIN", "APPROVER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/**").hasAnyRole("ADMIN", "APPROVER")

                        // Actuator (restricted)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
