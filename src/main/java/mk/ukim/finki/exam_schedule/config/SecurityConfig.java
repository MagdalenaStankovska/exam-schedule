package mk.ukim.finki.exam_schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsManager userDetailsService() {
        // Dev users for quick local testing without CAS.
        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin123")
                .roles("ADMIN")
                .build();
        UserDetails professor = User.withUsername("professor")
                .password("{noop}prof123")
                .roles("PROFESSOR")
                .build();
        UserDetails student = User.withUsername("student")
                .password("{noop}student123")
                .roles("STUDENT")
                .build();
        return new InMemoryUserDetailsManager(admin, professor, student);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   HeaderUserAuthenticationFilter headerUserAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/io.png", "/login", "/admin/calendar-view", "/admin/calendar-view/**", "/schedule/**", "/error", "/error/**").permitAll()
                        .requestMatchers("/professor/**").hasRole("PROFESSOR")
                        .requestMatchers("/admin/exam-session/**", "/admin/exam-definition/**", "/admin/scheduling/**", "/admin/schedule-export/**").hasRole("ADMIN")
                        .requestMatchers("/admin/subject-exam/**").hasAnyRole("ADMIN", "PROFESSOR")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
                .addFilterBefore(headerUserAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .defaultSuccessUrl("/admin/subject-exam", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}




