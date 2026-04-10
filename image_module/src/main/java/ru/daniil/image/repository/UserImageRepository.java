package ru.daniil.image.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.user.User;

@Repository
public interface UserImageRepository extends CrudRepository<User, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.imageName = :imageName WHERE u.login = :login")
    void saveImage(@Param("imageName") String imageName,
                   @Param("login") String login);
}
