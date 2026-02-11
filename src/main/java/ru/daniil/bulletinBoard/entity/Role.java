package ru.daniil.bulletinBoard.entity;

import jakarta.persistence.*;
import ru.daniil.bulletinBoard.enums.RoleName;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tbl_roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private RoleName name;

    private String description;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    public Role() {}

    public Role(RoleName name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name.toString(); }
    public void setName(RoleName name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

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
