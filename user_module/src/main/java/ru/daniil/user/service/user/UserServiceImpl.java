package ru.daniil.user.service.user;

import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.image.service.user.UserImageService;
import ru.daniil.user.repository.UserRepository;

import java.util.Optional;
import java.util.regex.Pattern;

@Transactional
@Service
public class UserServiceImpl implements UserService {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!$%^()_+\\-=\\[\\]{};:'\",.<>])[A-Za-z\\d!$%^()_+\\-=\\[\\]{};:'\",.<>]{8,}$"
    );
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserImageService userImageService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserImageService userImageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userImageService = userImageService;
    }

    @Override
    public UserDetails registerUser(RegistrationRequest request) {
        validateUserData(request.getEmail(), request.getPassword(), request.getLogin());

        User user = new User(
                request.getEmail(),
                request.getLogin(),
                passwordEncoder.encode(request.getPassword()),
                AuthProvider.LOCAL
        );

        return (UserDetails) userRepository.save(user);
    }

    @Override
    public UserDetails registerUserWithoutValidation(RegistrationRequest request) {
        User user = new User(
                request.getEmail(),
                request.getLogin(),
                "-",
                request.getAuthProvider()
        );

        return (UserDetails) userRepository.save(user);
    }

    private void validateUserData(String email, String password, String login) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Некорректный формат email");
        }
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("Пароль должен содержать минимум 8 символов, включая заглавные и строчные буквы латинского алфавита, цифры и специальные символы (кроме @/|\\*#&?)");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Пользователь с email " + email + " уже существует");
        }
        if (userRepository.existsByLogin(login)) {
            throw new ValidationException("Пользователь с логином " + login + " уже существует");
        }
    }

    @Override
    public User getAuthUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationException("Пользователь не авторизован") {};
        }

        return (User) authentication.getPrincipal();
    }

    @Override

    public String getUserAvatar(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()){
            return userImageService.completePath(user.get().getImageName());
        }
        else {
            throw new NotFoundException("Пользователь не был найден");
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    @Override
    public Optional<User> getByLogin(String login) {
        return userRepository.findByLogin(login);
    }
}
