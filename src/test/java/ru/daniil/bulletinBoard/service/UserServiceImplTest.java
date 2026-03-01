package ru.daniil.bulletinBoard.service;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.entity.request.RegistrationRequest;
import ru.daniil.bulletinBoard.repository.user.UserRepository;
import ru.daniil.bulletinBoard.service.user.UserServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegistrationRequest validRequest;
    private User user;

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest();
        validRequest.setEmail("test@test.com");
        validRequest.setLogin("testuser");
        validRequest.setPassword("Test123!$%");

        user = new User("test@test.com", "testuser", "encodedPassword");
        user.setId(1L);
    }

    @Test
    void registerUser_WithValidData_ShouldRegister() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByLogin("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Test123!$%")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.registerUser(validRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithBlankEmail_ShouldThrowException() {
        validRequest.setEmail("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertEquals("Некорректный формат email", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithInvalidEmailFormat_ShouldThrowException() {
        validRequest.setEmail("invalid");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertEquals("Некорректный формат email", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithValidEmail_ShouldPass() {
        validRequest.setEmail("valid@test.com");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByLogin(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.registerUser(validRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WithNullEmail_ShouldThrowException() {
        validRequest.setEmail(null);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertEquals("Некорректный формат email", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "short",
            "onlylowercase",
            "ONLYUPPERCASE",
            "NoDigits!",
            "NoSpecial123",
            "новыйпароль123!",
            "Valid1!",
            "Invalid@Character#123"
    })
    void registerUser_WithInvalidPassword_ShouldThrowException(String invalidPassword) {
        validRequest.setPassword(invalidPassword);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertTrue(exception.getMessage().contains("Пароль должен содержать"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowException() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertEquals("Пользователь с email test@test.com уже существует", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithExistingLogin_ShouldThrowException() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByLogin("testuser")).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser(validRequest));

        assertEquals("Пользователь с логином testuser уже существует", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAuthUser_WhenAuthenticated_ShouldReturnUser() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        User result = userService.getAuthUser();

        assertEquals(user, result);
    }

    @Test
    void getAuthUser_WhenNotAuthenticated_ShouldThrowException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);

        assertThrows(RuntimeException.class, () -> userService.getAuthUser());
    }

    @Test
    void getAuthUser_WhenAnonymous_ShouldThrowException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        SecurityContextHolder.setContext(securityContext);

        assertThrows(RuntimeException.class, () -> userService.getAuthUser());
    }

    @Test
    void getAuthUser_WhenAuthenticationNull_ShouldThrowException() {
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(null);

        SecurityContextHolder.setContext(securityContext);

        assertThrows(RuntimeException.class, () -> userService.getAuthUser());
    }

    @Test
    void existsUserByEmail_WhenExists_ShouldReturnTrue() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        boolean result = userService.existsUserByEmail("test@test.com");

        assertTrue(result);
    }

    @Test
    void existsUserByEmail_WhenNotExists_ShouldReturnFalse() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        boolean result = userService.existsUserByEmail("test@test.com");

        assertFalse(result);
    }

    @Test
    void findByLogin_WhenExists_ShouldReturnUser() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByLogin("testuser");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findByLogin_WhenNotExists_ShouldReturnEmpty() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByLogin("unknown");

        assertTrue(result.isEmpty());
    }
}