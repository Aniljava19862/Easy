package com.easy.config;

import com.easy.auth.service.UserService;
import com.easy.util.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder; // Still needed for DaoAuthenticationProvider
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final JwtRequestFilter jwtRequestFilter;
    private final PasswordEncoder passwordEncoder; // Now inject PasswordEncoder (from AppConfig)

    // Use constructor injection for dependencies
    public SecurityConfig(UserService userService, JwtRequestFilter jwtRequestFilter, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.passwordEncoder = passwordEncoder; // Assign the injected encoder
    }

    // REMOVE THE passwordEncoder() @Bean METHOD FROM HERE!
    // It's now in AppConfig.java

    // Configure Authentication Provider (using your UserService and PasswordEncoder)
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder); // Use the injected passwordEncoder
        return authProvider;
    }

    // Expose AuthenticationManager as a Bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Configure the Security Filter Chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless APIs
                .authorizeHttpRequests(authorize -> authorize
                                // Allow all requests to these specific paths without authentication
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/h2-console/**",
                                        // ⭐ THE CHANGE IS HERE: Allow ALL other requests as well
                                        "/**" // This pattern matches EVERYTHING
                                ).permitAll()
                        // ⭐ Remove .anyRequest().authenticated() if you want everything public
                        // If you uncomment the line below, it means any request *not* matched by the above
                        // requestMatchers will require authentication.
                        // .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for REST APIs
                )
                .authenticationProvider(authenticationProvider()) // Set your custom authentication provider
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class); // Add your JWT filter before UsernamePasswordAuthenticationFilter

        return http.build();
    }
}