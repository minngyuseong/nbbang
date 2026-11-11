package com.nbbang.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.nbbang.security.OAuthUserService;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OAuthUserService oauthUsers,
                                            @Value("${app.web-origin}") String webOrigin) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .ignoringRequestMatchers("/ws/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/api/institutions", "/oauth2/**", "/login/**", "/ws/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(login -> login
                        .userInfoEndpoint(info -> info.userService(oauthUsers))
                        .defaultSuccessUrl(webOrigin + "/auth/callback", true))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.web-origin}") String webOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(webOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "If-Match", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
