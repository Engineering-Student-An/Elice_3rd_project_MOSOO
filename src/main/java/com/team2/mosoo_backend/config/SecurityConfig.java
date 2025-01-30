package com.team2.mosoo_backend.config;

import com.team2.mosoo_backend.jwt.JwtFilter;
import com.team2.mosoo_backend.oath.OAuth2SuccessHandler;
import com.team2.mosoo_backend.oath.service.CustomOAuth2UserService;
import com.team2.mosoo_backend.user.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler successHandler;
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**", "/**").permitAll() // 공개 API
                        .anyRequest().authenticated() // 그 외 요청은 인증 필요
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
                .sessionManagement(session -> // 세션 정책을 Stateless로 적용
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Oauth2 로그인 설정
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/login") // 커스텀 로그인 페이지 경로 설정
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint
                                        .authorizationRequestRepository(customOAuth2UserService))
                        .successHandler(successHandler)
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint.userService(customOAuth2UserService))// 쿠키 기반 OAuth2 요청 저장소
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                // .requestMatchers(toH2Console())
                .requestMatchers("/static/")
                .requestMatchers("/");
    }

    // CORS 설정을 위한 Bean 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://mosooo.netlify.app")); // 허용할 출처
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH")); // 허용할 메서드
        configuration.setAllowCredentials(true); // 인증 정보 포함 여부
        configuration.setAllowedHeaders(List.of("*")); // 허용할 헤더

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정
        return source;
    }


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() { return new BCryptPasswordEncoder(); }
}
