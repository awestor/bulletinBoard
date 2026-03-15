package ru.daniil.bulletinBoard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.daniil.bulletinBoard.config.filters.JwtAuthenticationFilter;
import ru.daniil.bulletinBoard.entity.base.user.RefreshToken;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.service.user.auth.JwtService;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuth2UserService oAuth2UserService;
    private final JwtService jwtService;

    public SpringSecurityConfig(JwtAuthenticationFilter jwtAuthFilter, OAuth2UserService oAuth2UserService, JwtService jwtService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2UserService = oAuth2UserService;
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security) throws Exception {
        return security
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/register",
                                "/api/auth/**",
                                "/login",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error",
                                "/test-auth"
                        ).permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            // Генерация JWT токена и редирект на фронтенд с токеном
                            User user = (User) authentication.getPrincipal();
                            String accessToken = jwtService.generateToken(user);
                            RefreshToken refreshToken = jwtService.generateRefreshToken(user);
                            response.sendRedirect("/oauth2/success?access_token=" + accessToken +
                                    "&refresh_token=" + refreshToken.getToken());
                        })
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}