package com.marketplace.security.config;

import com.marketplace.security.jwt.JwtAuthenticationFilter;
import com.marketplace.security.jwt.JwtTokenProvider;
import com.marketplace.user.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) throws Exception {
        http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/sellers/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/sellers/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stores").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/me").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/stores/me").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/stores/me/deactivate").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH,"/api/v1/stores/me/activate").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/variants").permitAll()
                        .requestMatchers("/api/v1/products/me/*/variants/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/me").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/me/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/me/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/me/*/publish").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/me/*/archive").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/me/*/restore").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/me/*/images").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/me/*/images").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/products/me/*/images/*/primary").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/me/*/images/order").hasRole("SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/me/*/images/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/products/*/add-stock").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/products/*/remove-stock").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/products/{productId}").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/products/*/movements").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/cart").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/cart/items").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cart/items/{itemId}").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cart/items/{itemId}").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/favorites/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/storage/objects/products/*/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/reviews/*/approve", "/api/v1/reviews/*/reject").hasRole("ADMIN")
                        .requestMatchers("/api/v1/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/admin/promocodes/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/promocodes/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*/history").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/cancel").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/seller/orders/*/confirm").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/seller/orders/*/cancel").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/seller/orders").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/seller/orders/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/seller/orders/*/history").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/seller/stores/*/delivery-zones").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET,"/api/v1/seller/stores/*/delivery-zones").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/seller/stores/*/delivery-zones/*/status").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery/regions").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/stores/*/products").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/stores/*/products/*/images").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/stores/*/products/*").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());
        http.csrf((csrf -> csrf.disable()));


        JwtAuthenticationFilter jwtAuthenticationFilter= new JwtAuthenticationFilter(jwtTokenProvider,userRepository);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(form->form.disable());
        http.httpBasic(basic->basic.disable());
        http.exceptionHandling(exception->
                exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusAccessDeniedHandler(HttpStatus.FORBIDDEN)));
        return http.build();
    }
}
