package com.greenconnect.greenconnect_api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.greenconnect.greenconnect_api.security.JwtAuthenticationFilter;





/**
 * Spring Security Configuration for GreenConnect API.
 * <p>Cấu hình authentication và authorization cho toàn bộ application.</p>
 * <p>Sử dụng JWT-based stateless authentication.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Cấu hình Security Filter Chain.
     * <p>Định nghĩa các endpoints public và protected.</p>
     *
     * @param http HttpSecurity object để configure
     * @return SecurityFilterChain đã được cấu hình
     * @throws Exception nếu có lỗi configuration
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // ⭐ MỚI THÊM

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF vì đây là REST API, không sử dụng session-based auth
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            
            // Stateless session - không lưu session trên server
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
             // ⭐ MỚI THÊM - JWT Filter TRƯỚC UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Cấu hình authorization rules

            .authorizeHttpRequests(auth -> auth
                // ========== PUBLIC ENDPOINTS ==========
                .requestMatchers("/admin/google-login").permitAll()
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/verify-otp").permitAll()
                // Authentication & Registration
                .requestMatchers("/users/register").permitAll()
                .requestMatchers("/users/google-register").permitAll()
                .requestMatchers("/users/login").permitAll()
                .requestMatchers("/users/refresh").permitAll()
                .requestMatchers("/users/google-login").permitAll()

                // ⚠️ Address endpoints require authentication (CUSTOMER or ADMIN)
                .requestMatchers("/users/{userId}/addresses").authenticated()
                .requestMatchers("/users/addresses").authenticated()
                .requestMatchers("/users/addresses/{addressId}").authenticated()
                 
                .requestMatchers("/shipping/calculate-fee").permitAll()

                .requestMatchers("/products/customer/latest/page").permitAll()
                .requestMatchers("/products/customer/best-selling/page").permitAll()
                .requestMatchers("/products/customer/flash-sale/page").permitAll()
                .requestMatchers("/products/customer/featured/page").permitAll()
                .requestMatchers("/products/customer/top-rated-with-reviews/page").permitAll()
                .requestMatchers("/orders/vnpay-callback").permitAll() // IPN (chỉ production)
                .requestMatchers("/orders/verify-payment").permitAll() // Frontend verify (sandbox + production)
                .requestMatchers("/users/profile/{userid}").hasAnyRole("PRODUCT_MANAGER", "ADMIN","CUSTOMER","ORDER_MANAGER","MARKETING_MANAGER","CUSTOMER_SUPPORT","SHIPPER")
                //.requestMatchers("/users/*").permitAll()
                //.requestMatchers("/admin/**").permitAll()
                .requestMatchers("/promotion-campaigns/customer/valid/latest/page").permitAll()
                .requestMatchers("/promotion-campaigns/customer/bulk-purchase/latest").permitAll()
                .requestMatchers("/categories/customer/active/latest/page").permitAll()
                .requestMatchers("/forgot-password/send-otp").permitAll()
                .requestMatchers("/forgot-password/verify-otp").permitAll()
                .requestMatchers("/forgot-password/reset-password").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // .requestMatchers("/conversations/**").permitAll()
                .requestMatchers("/admin/manage/activate-account").permitAll()
                .requestMatchers("/admin/manage/activate-google").permitAll()
                .requestMatchers("/auth/refresh-token").permitAll()
                .requestMatchers("/api/test/create-test-user").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                // .requestMatchers("/admin/users/filter").permitAll()  
                // Public product browsing 
                .requestMatchers("/products/**").permitAll()
                //.requestMatchers("/categories/update").permitAll()
                .requestMatchers("/categories/active").permitAll()
                // .requestMatchers("/suppliers/**").permitAll()
                .requestMatchers("/banners/**").permitAll()
                .requestMatchers("/vouchers/public/**").permitAll()
                 .requestMatchers("/search/**").permitAll()
                 .requestMatchers("/suppliers/public/all/latest/page").permitAll()
                // Public API endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/homepage/**").permitAll()
                 .requestMatchers("/ws/**").permitAll()
                 
                // Footer endpoints - GET public, POST/PUT/DELETE require auth
                .requestMatchers("/footer/all").permitAll() // GET all footer
                 
                // ⭐ Chatbot endpoints - Require authentication (CUSTOMER hoặc ADMIN)
                .requestMatchers("/api/chat/**").authenticated()
                
                // Health check & monitoring
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                
                // Swagger documentation (if enabled)
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/cart/**").permitAll()
                .requestMatchers("/vouchers/available").permitAll()
                .requestMatchers("/vouchers/active").permitAll()
                //.requestMatchers("/orders/**").permitAll() // ← CHANGE THIS
                .requestMatchers("/orders/my-orders-review").authenticated() // ⭐ Explicit rule for review endpoint
                .requestMatchers("/delivery-options/active").permitAll() 
                .requestMatchers("/addresses/**").permitAll() 
                .requestMatchers("/order-refunds/create").permitAll() 
                .requestMatchers("/order-refunds/all").permitAll() 
                .requestMatchers("/order-refunds/active").permitAll() 
                .requestMatchers("/suppliers/{id}/full2").permitAll() 
        .requestMatchers("/reviews/getReviews/{productId}").permitAll()
            .requestMatchers("/reviews/**").permitAll()
            .requestMatchers("/homepage-layouts/{id}/content").permitAll()
            .requestMatchers("/homepage-layouts/viewallcampain/{campaignId}").permitAll()
                
            ///user-vouchers
                             // // ========== CATEGORIES - SPECIFIC RULES FIRST ========== 
                // // ADMIN Categories endpoints - Require Authentication (ĐẶT TRƯỚC)
                // .requestMatchers(HttpMethod.POST, "/categories").authenticated()
                // .requestMatchers(HttpMethod.PUT, "/categories/**").authenticated()
                // .requestMatchers("/categories/admin/**").authenticated()
                
                // // PUBLIC Categories endpoints (ĐẶT SAU)
                // .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                // .requestMatchers(HttpMethod.GET, "/categories/active").permitAll()
                // .requestMatchers(HttpMethod.GET, "/categories/{categoryId}").permitAll()
                
                // // Forgot Password endpoints
                // .requestMatchers("/forgot-password/**").permitAll()



                // ========== PROTECTED ENDPOINTS ==========
                
                // Require authentication for all other endpoints
                // ADMIN ONLY
                // .requestMatchers("/admin/**").hasRole("ADMIN")
                // .requestMatchers("/users/admin/**").hasRole("ADMIN")
                
                // // PRODUCT_MANAGER OR ADMIN
                // .requestMatchers("/products/admin/**").hasAnyRole("PRODUCT_MANAGER", "ADMIN")
                // .requestMatchers("/categories/admin/**").hasAnyRole("PRODUCT_MANAGER", "ADMIN")
                
                // // ORDER_MANAGER OR ADMIN
                // .requestMatchers("/orders/admin/**").hasAnyRole("ORDER_MANAGER", "ADMIN")
                
                // // CUSTOMER OR ADMIN
                    // .requestMatchers("/users/profile/{userId}").hasAnyRole(
                    //     "CUSTOMER", "ADMIN", "ORDER_MANAGER", "PRODUCT_MANAGER", 
                    //     "MARKETING_MANAGER", "CUSTOMER_SUPPORT", "SHIPPER"
                    // )  
                                  // .requestMatchers("/orders/**").hasAnyRole("CUSTOMER", "ADMIN", "ORDER_MANAGER")
                // .requestMatchers("/cart/**").hasAnyRole("CUSTOMER", "ADMIN")
                
                // // SHIPPER OR ADMIN
                // .requestMatchers("/shipping/**").hasAnyRole("SHIPPER", "ADMIN")
                .anyRequest().authenticated()
                // .anyRequest().permitAll()
            );
            
        return http.build();
    }

    /**
     * Password Encoder Bean.
     * <p>Sử dụng BCrypt với strength 12 để hash passwords.</p>
     * <p>Phải khớp với PasswordUtils để đảm bảo consistency.</p>
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}