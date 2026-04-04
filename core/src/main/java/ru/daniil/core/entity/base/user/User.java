package ru.daniil.core.entity.base.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.daniil.core.enums.AuthProvider;
import ru.daniil.core.enums.RoleName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tbl_user")
@Data
@Builder
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "blocked_until")
    private LocalDate blockedUntil;

    @Column(name = "trading_blocked", nullable = false)
    private boolean tradingBlocked;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    public User() {
        tradingBlocked = false;
        roles = new HashSet<>();
    }

    public User(String email, String login, String password, AuthProvider authProvider) {
        this();
        this.email = email;
        this.login = login;
        this.password = password;
        this.authProvider = authProvider;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> (GrantedAuthority) () -> "ROLE_" + role.getName())
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonLocked() {
        return blockedUntil == null || blockedUntil.isBefore(LocalDate.now());
    }

    @Override
    public boolean isEnabled() {
        return isAccountNonLocked();
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream()
                .anyMatch(role -> Objects.equals(role.getName(), roleName.toString()));
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}