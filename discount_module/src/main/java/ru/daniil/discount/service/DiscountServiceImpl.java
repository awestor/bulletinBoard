package ru.daniil.discount.service;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.exceptions.DiscountNotActiveException;
import ru.daniil.core.request.discount.CreateDiscountRequest;
import ru.daniil.discount.mapper.DiscountMapper;
import ru.daniil.discount.repository.DiscountRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountServiceImpl implements DiscountService {
    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;

    public DiscountServiceImpl(DiscountRepository discountRepository, DiscountMapper discountMapper) {
        this.discountRepository = discountRepository;
        this.discountMapper = discountMapper;
    }

    @Override
    public List<Discount> getAllActiveDiscount() {
        return discountRepository.findAllActive(LocalDateTime.now());
    }

    @Override
    public Discount getDiscountByCode(String code) {
        return discountRepository.findByCode(code).orElseThrow(
                () -> new NotFoundException("Скидка с данным кодом не найдена")
        );
    }

    @Override
    public Discount createDiscount(CreateDiscountRequest request) {
        if (discountRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Скидка с таким кодом уже существует");
        }

        Discount discount = discountMapper.toDiscount(request);
        return discountRepository.save(discount);
    }

    @Override
    public Discount getActiveDiscountByCode(String code) {
        Discount discount = discountRepository.findByCode(code).orElseThrow(
                () -> new NotFoundException("Скидка с данным кодом не найдена")
        );
        if (!isAvailable(discount)){
            throw new DiscountNotActiveException("Скидка уже просрочена");
        }
        return discount;
    }

    @Transactional
    @Override
    public void incrementUsageCount(Long discountId) {
        discountRepository.incrementUsageCount(discountId);
    }

    @Override
    public void decrementUsageCount(Long discountId) {
        discountRepository.decrementUsageCount(discountId);
    }

    @Override
    public boolean isAvailable(Discount discount){
        LocalDateTime now = LocalDateTime.now();
        return (discount.getStartDate() == null || !now.isBefore(discount.getStartDate()))
                && (discount.getEndDate() == null || !now.isAfter(discount.getEndDate()))
                && (discount.getUsageLimit() == null || discount.getUsageCount() < discount.getUsageLimit());
    }

    @Transactional
    @Override
    public void activateDiscount(String code, Integer usages) {
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Скидка с кодом " + code + " не найдена"));

        discount.setUsageCount(usages);
        discountRepository.save(discount);
    }

    @Transactional
    @Override
    public void deactivateDiscount(String code) {
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Скидка с кодом " + code + " не найдена"));

        discount.setUsageCount(0);
        discountRepository.save(discount);
    }
}
