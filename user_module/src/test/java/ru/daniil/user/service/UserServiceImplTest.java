package ru.daniil.user.service;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.request.auth.RegistrationRequest;
import ru.daniil.user.repository.UserRepository;
import ru.daniil.user.service.user.UserServiceImpl;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
        validRequest.setLogin("testuser");
        validRequest.setEmail("test@test.com");
        validRequest.setPassword("Test123!$%");
        validRequest.setAuthProvider(AuthProvider.LOCAL);

        user = new User("test@test.com", "testuser", "encodedPassword", AuthProvider.LOCAL);
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
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("email")).thenReturn(user.getEmail());

        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userService.getByEmail(user.getEmail())).thenReturn(Optional.of(user));

        User result = userService.getAuthUser();

        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getLogin());
    }

    @Test
    void getAuthUser_WhenAnonymous_ShouldThrowException() {
        AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
                "anonymousKey",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        SecurityContextHolder.getContext().setAuthentication(anonymousToken);

        assertThrows(ClassCastException.class, () -> {
            userService.getAuthUser();
        });
    }

    @Test
    void getAuthUser_WhenAuthenticationNull_ShouldThrowException() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Throwable exception = assertThrows(Throwable.class, () -> {
            userService.getAuthUser();
        });

        assertInstanceOf(AssertionError.class, exception);
    }

    @Test
    void existsUserByEmail_WhenExists_ShouldReturnTrue() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        boolean result = userService.existsByEmail("test@test.com");

        assertTrue(result);
    }

    @Test
    void existsUserByEmail_WhenNotExists_ShouldReturnFalse() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        boolean result = userService.existsByEmail("test@test.com");

        assertFalse(result);
    }

    @Test
    void getByLogin_WhenExists_ShouldReturnUser() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getByLogin("testuser");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getByLogin_WhenNotExists_ShouldReturnEmpty() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.getByLogin("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserByCommentId_Success() {
        when(userRepository.findUserByCommentId(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserByCommentId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLogin()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getUserByCommentId_CommentNotFound_ThrowsNotFoundException() {
        when(userRepository.findUserByCommentId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByCommentId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Пользователь по указанном id комментария не найден");
    }
}