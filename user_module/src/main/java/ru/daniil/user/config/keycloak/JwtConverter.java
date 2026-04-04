package ru.daniil.user.config.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Конвертер JWT токена Keycloak в объект аутентификации Spring Security.
 *
 * Этот класс преобразует JWT от Keycloak в JwtAuthenticationToken,
 * извлекая роли из realm_access и resource_access и преобразуя их
 * в GrantedAuthority с префиксом "ROLE_".
 */
@Component
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.principle-attribute:sub}")
    private String principleAttribute;

    @Value("${jwt.auth.converter.resource-id:bulletin-board-client}")
    private String resourceId;

    @Override
    public AbstractAuthenticationToken convert( Jwt jwt) {
        // Собираем все authorities из трех источников
        Collection<GrantedAuthority> authorities = Stream.concat(
                // 1. Стандартные authorities (scope и т.д.)
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                // 2. Роли из resource_access (клиентские роли)
                extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());

        // 3. Добавляем роли из realm_access (глобальные роли)
        authorities.addAll(extractRealmRoles(jwt));

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                getPrincipleClaimName(jwt)
        );
    }

    /**
     * Получает имя пользователя из JWT.
     * По умолчанию использует subject (sub), но можно настроить
     * через property principle-attribute (например, email)
     */
    private String getPrincipleClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (principleAttribute != null) {
            claimName = principleAttribute;
        }
        return jwt.getClaim(claimName);
    }

    /**
     * Извлекает роли для конкретного клиента из resource_access.
     * Пример структуры JWT:
     * {
     *   "resource_access": {
     *     "bulletin-board-client": {
     *       "roles": ["user", "admin"]
     *     }
     *   }
     * }
     */
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (resourceAccess == null || resourceAccess.get(resourceId) == null) {
            return Set.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceId);

        @SuppressWarnings("unchecked")
        Collection<String> resourceRoles = (Collection<String>) resource.get("roles");

        if (resourceRoles == null || resourceRoles.isEmpty()) {
            return Set.of();
        }

        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    /**
     * Извлекает глобальные роли из realm_access.
     * Пример структуры JWT:
     * {
     *   "realm_access": {
     *     "roles": ["offline_access", "uma_authorization", "default-roles-bulletin"]
     *   }
     * }
     */
    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Set.of();
        }

        @SuppressWarnings("unchecked")
        Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");

        if (realmRoles == null || realmRoles.isEmpty()) {
            return Set.of();
        }

        return realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }
}