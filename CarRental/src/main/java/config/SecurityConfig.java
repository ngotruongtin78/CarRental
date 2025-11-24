package CarRental.example.config;

import CarRental.example.security.CustomUserDetailsService;
import CarRental.example.security.CustomLoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;



    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(customUserDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**", "/api/vehicles/admin/**", "/api/stations/admin/**", "/api/rental/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**", "/api/staff/**", "/api/staff/return/**").hasRole("STAFF")
                        .requestMatchers("/payment/webhook", "/api/payment/webhook", "/api/sepay/webhook", "/payment/return", "/payment/cancel").permitAll()
                        .requestMatchers("/datxe", "/thanhtoan", "/sepay-qr", "/payos-qr", "/lichsuthue", "/user-hosocanhan",
                                "/api/rental/**", "/api/payment/**").authenticated()
                        .requestMatchers("/", "/home", "/register", "/css/**", "/js/**", "/images/**",
                                "/api/stations/**", "/api/vehicles/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login-process")
                        .successHandler(new CustomLoginSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/home?logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(sess -> sess
                        .invalidSessionUrl("/home")
                );

        return http.build();
    }
}