package com.easy.config; // Or a general 'com.easy.config.AppConfig'

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean // This bean can now be created independently
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}