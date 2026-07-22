package com.frezo.auth.config;

import com.frezo.auth.security.JwtAccessDeniedHandler;
import com.frezo.auth.security.JwtAuthenticationEntryPoint;
import com.frezo.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security config — JWT stateless + RBAC qua {@code @CheckPermission} aspect + method security qua {@code @PreAuthorize}.
 * <p>
 * <b>Đã fix (v1.1 — Batch B):</b>
 * <ul>
 *   <li>Password encoder: {@code DelegatingPasswordEncoder} (default = BCrypt-12), backward-compat với {@code {noop}} legacy plain text.</li>
 *   <li>CORS: whitelist qua {@link SecurityCorsProperties} — KHÔNG còn {@code allowedOrigins("*")}.</li>
 *   <li>DI: constructor injection thay {@code @Autowired} field.</li>
 *   <li>Method security: {@code @EnableMethodSecurity(prePostEnabled=true)} — cho phép {@code @PreAuthorize}, {@code @PostAuthorize}.</li>
 * </ul>
 * <p>
 * <b>Kế hoạch tiếp theo (Batch C):</b> bật lại toàn bộ {@code @CheckPermission} đang bị comment-out trong controllers,
 * implement SUPER_ADMIN bypass, rà soát permission code trong DB.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final SecurityCorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           JwtAuthenticationEntryPoint unauthorizedHandler,
                                           JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Auth + OpenAPI docs + register + websocket + error
                        .requestMatchers(
                                "/auth/**",
                                "/api-docs/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/qtht/user/register",
                                "/ws-endpoint/**",
                                "/error"
                        ).permitAll()
                        // Public landing / catalog / article (chỉ GET) — dùng cho landing page KHÔNG cần login
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/public/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/public/product/filter").permitAll()
                        // Public inbox — landing contact form + Zalo OA webhook (POST). Có anti-spam
                        // (honeypot + IP rate limit) + signature verify riêng cho Zalo trong controller.
                        // Context-path là /api → matcher chỉ ghi path phần sau context, không có prefix `/api`.
                        .requestMatchers("/public/inbox/**").permitAll()
                        // Internal gateway (service-to-service, có protection riêng qua network policy)
                        .requestMatchers("/qtht/internal-gateway/**").permitAll()
                        // Actuator — dev bật; prod override qua application-prod.yml
                        .requestMatchers("/actuator/**").permitAll()
                        // v1.1 fix (Batch H): /customer/** và /voucher/** trước đây permitAll cho MỌI method
                        //   → cho phép anonymous POST/PUT/DELETE. GỠ. Nếu cần API public cho storefront,
                        //     tách endpoint riêng dưới prefix /public/customer, /public/voucher.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS config đọc từ {@link SecurityCorsProperties} — cấm {@code allowedOrigins("*")}.
     * <p>
     * Nếu {@code allowCredentials=true} + có {@code "*"} trong allowed-origins → Spring throw exception (đúng quy chuẩn).
     * Log warning nếu detect {@code "*"} trong config để phát hiện sớm cấu hình sai.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if (corsProperties.getAllowedOrigins().contains("*")) {
            log.warn("CORS allowedOrigins contains '*' — this is INSECURE for production. " +
                    "Set 'app.cors.allowed-origins' env to explicit whitelist.");
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Password encoder — {@code DelegatingPasswordEncoder} với default id = {@code bcrypt} (strength 10 mặc định Spring).
     * <p>
     * Hỗ trợ đồng thời:
     * <ul>
     *   <li>Password mới register → luôn hash {@code {bcrypt}$2a$10$...}</li>
     *   <li>Password cũ chưa migrate → prefix {@code {noop}plaintext} vẫn login được (chỉ trong giai đoạn migration)</li>
     * </ul>
     * <p>
     * <b>Migration bắt buộc:</b> chạy script SQL trong {@code FrezoBE/module-auth-bom/src/main/resources/scripts/migrate-passwords-to-bcrypt.sql}
     * để convert tất cả password plain text sang BCrypt. Sau migration, xoá support {@code {noop}} bằng cách chuyển sang
     * {@code new BCryptPasswordEncoder(12)} thuần.
     * <p>
     * <b>CẤM</b> quay lại {@code NoOpPasswordEncoder} kể cả tạm thời.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
