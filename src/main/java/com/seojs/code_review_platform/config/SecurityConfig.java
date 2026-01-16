package com.seojs.code_review_platform.config;

import com.seojs.code_review_platform.github.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthorizedClientService authorizedClientService;

        @Value("${app.frontend-url}")
        private String frontendUrl;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 적용
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())) // H2 콘솔을 위해 iframe 허용
                                .addFilterAfter(new GitHubTokenValidationFilter(authorizedClientService),
                                                BasicAuthenticationFilter.class)
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/github/status", "/oauth2/**").permitAll()
                                                .requestMatchers("/api/github/webhook/**").permitAll()
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**",
                                                                "/public/**")
                                                .permitAll()
                                                .requestMatchers("/favicon.ico", "/robots.txt").permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .defaultSuccessUrl(frontendUrl, true)
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService)))
                                .logout(logout -> logout
                                                .logoutUrl("/oauth2/logout")
                                                .logoutSuccessUrl(frontendUrl)
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID"));
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendUrl));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
