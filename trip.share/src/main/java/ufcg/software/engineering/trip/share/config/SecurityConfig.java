package ufcg.software.engineering.trip.share.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;
import ufcg.software.engineering.trip.share.handlers.FirebaseAuthenticationFilter;
import ufcg.software.engineering.trip.share.handlers.SecurityErrors;
import ufcg.software.engineering.trip.share.service.FirebaseTokenService;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, FirebaseTokenService service, JsonMapper mapper,
                                           AuthProperties authProperties)
            throws Exception {
        var authenticationFilter = new FirebaseAuthenticationFilter(service, mapper, authProperties);
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityErrors.write(mapper, response, 401, "Firebase Bearer token is required"))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityErrors.write(mapper, response, 403, "Access denied")))
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
