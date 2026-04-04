package ru.daniil.core.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.daniil.core.enums.AuthProvider;

@Data
@Builder
public class RegistrationRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email",
            regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$")
    private String email;

    @NotBlank(message = "Логин обязателен")
    @Size(min = 6, message = "Логин должен содержать минимум 6 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Логин может содержать только буквы, цифры и подчеркивания")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!$%^()_+\\-=\\[\\]{};:'\",.<>])[A-Za-z\\d!$%^()_+\\-=\\[\\]{};:'\",.<>]{8,}$",
            message = "Пароль должен содержать минимум 8 символов, включая заглавные и строчные буквы латинского алфавита, цифры и специальные символы (кроме @/|\\*#&?)")
    private String password;

    private AuthProvider authProvider;

    public RegistrationRequest() {
        authProvider = null;
    }

    public RegistrationRequest(String login, String email, String password) {
        this.login = login;
        this.email = email;
        this.password = password;
    }

    public RegistrationRequest(String login, String email, String password, AuthProvider authProvider) {
        this.login = login;
        this.email = email;
        this.password = password;
        this.authProvider = authProvider;
    }
}
