package mk.ukim.finki.exam_schedule.config;

import jakarta.servlet.FilterChain;
import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.model.UserRole;
import mk.ukim.finki.exam_schedule.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeaderUserAuthenticationFilterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final HeaderUserAuthenticationFilter filter = new HeaderUserAuthenticationFilter(userRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesSecurityContextFromXUserEmailHeader() throws Exception {
        User user = new User("u1", "Admin", "admin@example.com", UserRole.DEAN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Email", "admin@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_DEAN", "ROLE_PROFESSOR")
                .doesNotContain("ROLE_ADMIN");
        verify(chain).doFilter(request, response);
    }
}

