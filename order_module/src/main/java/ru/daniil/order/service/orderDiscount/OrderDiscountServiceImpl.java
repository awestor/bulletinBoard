package ru.daniil.order.service.orderDiscount;

import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.order.repository.discount.OrderDiscountRepository;

import java.util.List;
import java.util.Optional;

@Service
public class OrderDiscountServiceImpl implements OrderDiscountService{

    private final OrderDiscountRepository orderDiscountRepository;

    public OrderDiscountServiceImpl(OrderDiscountRepository orderDiscountRepository) {
        this.orderDiscountRepository = orderDiscountRepository;
    }

    @Override
    public List<OrderDiscount> getOrderDiscountByOrderId(Long orderId) {
        return orderDiscountRepository.findByOrderId(orderId);
    }

    @Override
    public OrderDiscount save(OrderDiscount orderDiscount) {
        return orderDiscountRepository.save(orderDiscount);
    }

    @Override
    public Optional<OrderDiscount> getOrderDiscountByIds(Long orderId, Long discountId) {
        return orderDiscountRepository.findByOrderIdAndDiscountId(orderId, discountId);
    }

    @Override
    public void deleteByIds(Long orderId, Long discountId) {
        orderDiscountRepository.deleteByOrderIdAndDiscountId(orderId, discountId);
    }
}
