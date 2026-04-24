package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.service.impl.CustomUserDetailsService;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - authentication
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/login/responsable").permitAll()
                        .requestMatchers("/api/auth/login/admin").permitAll()

                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/auth/utilisateurs/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/auth/utilisateurs").hasRole("ADMIN")
                        .requestMatchers("/api/auth/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers("/api/tableau-de-bord/**").hasRole("ADMIN")
                        .requestMatchers("/api/collectes/**").permitAll()
                        .requestMatchers("/api/responsable/**").hasAnyRole("ADMIN", "RESPONSABLE")
                        .requestMatchers("/api/vergers/**").hasAnyRole("ADMIN", "RESPONSABLE", "AGRICULTEUR")
                        .requestMatchers("/api/alertes/**").hasAnyRole("ADMIN", "RESPONSABLE", "AGRICULTEUR")
                        .requestMatchers("/api/tournees/**").hasAnyRole("ADMIN", "RESPONSABLE", "EQUIPE_RECOLTE")
                        .requestMatchers("/api/travailleurs/**").hasRole("RESPONSABLE")
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // IMPORTANT: Explicitly list Authorization header
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}