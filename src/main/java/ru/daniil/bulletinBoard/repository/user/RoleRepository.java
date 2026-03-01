package ru.daniil.bulletinBoard.repository.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.bulletinBoard.entity.base.user.Role;
import ru.daniil.bulletinBoard.enums.RoleName;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

}
