package iuh.fit.se.sebook_backend.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import iuh.fit.se.sebook_backend.configs.CustomerJwtDecoder;
import iuh.fit.se.sebook_backend.configs.JwtAuthenticationConverter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomerJwtDecoder customerJwtDecoder;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép truy cập static files (ảnh) mà không cần authentication - ĐẶT ĐẦU TIÊN
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")
                        .requestMatchers("/api/categories/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")

                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/my-orders").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{orderId}/status").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/orders/all").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")

                        .requestMatchers("/api/suppliers/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_WAREHOUSE_STAFF")
                        .requestMatchers("/api/import-stocks/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_WAREHOUSE_STAFF")

                        .requestMatchers("/api/promotions/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")
                        .requestMatchers("/api/promotion-logs/**").hasAnyAuthority("SCOPE_ADMIN", "SCOPE_SELLER_STAFF")

                        .requestMatchers(HttpMethod.POST, "/api/payment/create-payment").authenticated() // Chỉ người dùng đăng nhập mới được tạo thanh toán
                        .requestMatchers(HttpMethod.GET, "/api/payment/vnpay-return").permitAll() // VNPay callback phải là public

                        .requestMatchers("/api/notifications/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/admin/accounts/me").authenticated() // Cho phép user xem thông tin của mình
                        .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/me").authenticated() // Cho phép user tự cập nhật thông tin của mình
                        .requestMatchers("/api/user/addresses/**").authenticated() // Cho phép user quản lý địa chỉ
                        .requestMatchers("/api/admin/**").hasAuthority("SCOPE_ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customerJwtDecoder)
                                .jwtAuthenticationConverter(new JwtAuthenticationConverter())
                        )
                );
        return http.build();
    }
}