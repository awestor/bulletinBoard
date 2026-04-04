package ru.daniil.user.service.auth;

import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.exceptions.UserBlockedExeption;
import ru.daniil.core.exceptions.UserNotFoundException;
import ru.daniil.user.repository.user.UserRepository;


@Service
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;

        if (username.contains("@")) {
            user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
        } else {
            user = userRepository.findByLogin(username)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
        }

        if (!user.isAccountNonLocked()) {
            throw new UserBlockedExeption("Аккаунт заблокирован до: " + user.getBlockedUntil());
        }

        return (UserDetails) user;
    }
}
