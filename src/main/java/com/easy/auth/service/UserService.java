package com.easy.auth.service;

import org.springframework.security.core.userdetails.User; // Spring Security's User class
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder; // Needed to encode hardcoded password
import org.springframework.stereotype.Service;

import java.util.ArrayList; // For simple granted authorities

@Service
public class UserService implements UserDetailsService {

    // Inject PasswordEncoder to encode the hardcoded password
    // This is crucial! Passwords must always be encoded.
    private final PasswordEncoder passwordEncoder;

    // Use constructor injection
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // --- For DEMONSTRATION PURPOSES ONLY: Hardcoded user ---
        // In a real application, you would fetch user details from a database
        // using UserRepository.
        if ("user".equals(username)) {
            return new User(
                    "user",
                    passwordEncoder.encode("password"), // Encode the hardcoded password!
                    new ArrayList<>() // Empty list for roles/authorities for now
            );
        } else if ("admin".equals(username)) {
            return new User(
                    "admin",
                    passwordEncoder.encode("adminpass"), // Encode the hardcoded password!
                    new ArrayList<>() // Add roles like new SimpleGrantedAuthority("ROLE_ADMIN") here for real apps
            );
        }
        // --- End of Demonstration Code ---

        throw new UsernameNotFoundException("User not found with username: " + username);
    }

    // You can remove or comment out any 'save' methods that relied on UserRepository for now.
    // public User save(User user) { ... }
}