package ru.daniil.user.service.user;

import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;

import java.time.LocalDate;

@Service
public interface UserAdminService {
    User blockUser(Long userId, LocalDate blockedUntil);
    User unblockUser(Long userId);
    User blockTrading(Long userId);
    User unblockTrading(Long userId);
}
