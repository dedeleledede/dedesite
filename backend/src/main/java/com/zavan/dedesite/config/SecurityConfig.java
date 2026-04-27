package com.zavan.dedesite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("umaChaveSeguraUnica")
                .tokenValiditySeconds(1209600) // 14 dias
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.sendRedirect("/access-denied");
                    })
                    .accessDeniedPage("/access-denied")
            )
                
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
              .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .authorizeHttpRequests(auth -> auth
                // arquivos públicos (imagens)
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // páginas públicas
                .requestMatchers("/", "/blog", "/blog/**", "/login", "/register", "/health", "/access-denied", "/error").permitAll()
                // estáticos comuns (se tiver)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                // upload restrito (mas também há @PreAuthorize no controller)
                .requestMatchers("/api/uploads/**").hasAnyRole("ADMIN", "AUTHOR")
                .anyRequest().authenticated()
            );
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
}
