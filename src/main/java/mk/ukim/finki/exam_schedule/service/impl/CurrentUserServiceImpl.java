package mk.ukim.finki.exam_schedule.service.impl;

import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Override
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    @Override
    public boolean isAdmin() {
        return getCurrentUser().map(user -> !user.getRole().isStudent() && !user.getRole().isProfessor()).orElse(false);
    }

    @Override
    public boolean isProfessor() {
        return getCurrentUser().map(user -> user.getRole().isProfessor()).orElse(false);
    }
}

