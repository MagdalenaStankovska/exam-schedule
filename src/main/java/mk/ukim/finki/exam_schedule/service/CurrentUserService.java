package mk.ukim.finki.exam_schedule.service;

import mk.ukim.finki.exam_schedule.model.User;
import mk.ukim.finki.exam_schedule.model.UserRole;

import java.util.Optional;

public interface CurrentUserService {
    Optional<User> getCurrentUser();

    boolean isAdmin();

    boolean isProfessor();

    default UserRole getCurrentRoleOrNull() {
        return getCurrentUser().map(User::getRole).orElse(null);
    }
}

