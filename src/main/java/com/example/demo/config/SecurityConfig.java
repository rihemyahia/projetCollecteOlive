package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/login/responsable").permitAll()
                        .requestMatchers("/api/auth/login/admin").permitAll()
                        .requestMatchers("/api/auth/utilisateurs").hasRole("ADMIN") // Seul l'admin peut gérer les utilisateurs
                        .requestMatchers("/api/auth/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers("/api/tableau-de-bord/**").hasRole("ADMIN")
                        .requestMatchers("/api/vergers/**").hasAnyRole("ADMIN", "RESPONSABLE") // Le responsable peut gérer les vergers
                        .requestMatchers("/api/alertes/**").hasAnyRole("ADMIN", "RESPONSABLE", "AGRICULTEUR") // Le responsable et l'agriculteur peuvent gérer les alertes
                        .requestMatchers("/api/tournees/**").hasAnyRole("ADMIN", "RESPONSABLE", "EQUIPE_RECOLTE") // Le responsable et l'équipe de récolte peuvent gérer les tournées
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
}