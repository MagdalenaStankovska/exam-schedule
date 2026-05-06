package mk.ukim.finki.exam_schedule.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.model.UserRole;
import mk.ukim.finki.exam_schedule.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class HeaderUserAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_HEADER = "X-User-Email";

    private final UserRepository userRepository;

    public HeaderUserAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String email = request.getHeader(USER_HEADER);

        if (StringUtils.hasText(email) && SecurityContextHolder.getContext().getAuthentication() == null) {
            userRepository.findByEmail(email).ifPresent(user -> {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        mapAuthorities(user.getRole())
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> mapAuthorities(UserRole role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.roleName()));

        if (role.isProfessor()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
        }
        if (role.isStudent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        }
        if (!role.isStudent() && !role.isProfessor()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return authorities;
    }
}

