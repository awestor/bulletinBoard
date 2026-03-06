package ru.daniil.bulletinBoard.entity.base.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.daniil.bulletinBoard.enums.RoleName;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tbl_roles")
@Data
@Builder
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private RoleName name;

    private String description;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

    public Role() {
        users = new HashSet<>();
    }

    public Role(RoleName name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public String getName() { return name.toString(); }

    @Override
    public boolean equals(Object rol) {
        if (this == rol) return true;
        if (!(rol instanceof Role role)) return false;
        return id != null && id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
