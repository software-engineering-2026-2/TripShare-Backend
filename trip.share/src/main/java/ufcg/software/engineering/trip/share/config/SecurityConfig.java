package ufcg.software.engineering.trip.share.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ufcg.software.engineering.trip.share.handlers.AuthProblemResponses;
import ufcg.software.engineering.trip.share.handlers.SessionAuthenticationFilter;
import ufcg.software.engineering.trip.share.service.SessionService;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionService sessions, AuthProblemResponses problems) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, failure) -> problems.write(request, response,
                                HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Sessão inválida ou expirada. Autentique-se novamente."))
                        .accessDeniedHandler((request, response, failure) -> problems.write(request, response,
                                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acesso não permitido.")))
                // Instanciado apenas na cadeia de segurança, sem registro duplicado como filtro servlet.
                .addFilterBefore(new SessionAuthenticationFilter(sessions, problems), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
