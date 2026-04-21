package ru.daniil.discount.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.daniil.core.entity.base.discount.Discount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends CrudRepository<Discount, Long> {
    Optional<Discount> findByCode(String code);

    @Query("""
        SELECT d FROM Discount d
        WHERE (d.startDate IS NULL OR d.startDate <= :now)
        AND (d.endDate IS NULL OR d.endDate >= :now)
        AND (d.usageLimit IS NULL OR d.usageCount < d.usageLimit)
        """)
    List<Discount> findAllActive(@Param("now") LocalDateTime now);

    @Query("UPDATE Discount d SET d.usageCount = d.usageCount + 1 WHERE d.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    @Query("UPDATE Discount d SET d.usageCount = d.usageCount - 1 WHERE d.id = :id")
    void decrementUsageCount(@Param("id") Long id);
}
