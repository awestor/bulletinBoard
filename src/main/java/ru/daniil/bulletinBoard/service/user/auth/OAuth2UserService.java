package ru.daniil.bulletinBoard.service.user.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import ru.daniil.bulletinBoard.entity.base.user.Role;
import ru.daniil.bulletinBoard.entity.base.user.User;
import ru.daniil.bulletinBoard.enums.RoleName;
import ru.daniil.bulletinBoard.repository.user.RoleRepository;
import ru.daniil.bulletinBoard.repository.user.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Обновляем информацию при необходимости
        } else {
            // Создаем нового пользователя
            Role userRole = roleRepository.findByName(RoleName.USER)
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));

            user = User.builder()
                    .email(email)
                    .login(name)
                    .password("")
                    .roles(Set.of(userRole))
                    .build();

            userRepository.save(user);
        }

        return (OAuth2User) user;
    }
}
