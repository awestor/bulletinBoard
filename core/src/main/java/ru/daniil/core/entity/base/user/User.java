package ru.daniil.core.entity.base.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.daniil.core.entity.base.product.Product;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.enums.AuthProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Wallet wallet;

    public User() {
        tradingBlocked = false;
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
        // Роли теперь приходят из JWT, поэтому возвращает пустую коллекцию
        return Collections.emptySet();
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
}