package com.example.espreitar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAutenticacaoFilter jwtAutenticacaoFilter;

    public SecurityConfig(JwtAutenticacaoFilter jwtAutenticacaoFilter) {
        this.jwtAutenticacaoFilter = jwtAutenticacaoFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/Espreitar/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/admin", "/usuarios/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/midias/externa", "/midias/externa/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/midias", "/midias/**", "/avaliacoes/midia/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/denuncias", "/denuncias/**",
                                "/solicitacoes/fila", "/solicitacoes/fila/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/denuncias", "/denuncias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/solicitacoes/*/aprovar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/solicitacoes/*/rejeitar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/midias", "/midias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/midias", "/midias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/midias", "/midias/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAutenticacaoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}