package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.*;
import ru.daniil.bulletinBoard.entity.base.order.Order;
import ru.daniil.bulletinBoard.entity.response.OrderResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OrderItemMapper.class, OrderDiscountMapper.class})
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "appliedDiscounts", source = "appliedDiscounts")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    @AfterMapping
    default void setTotalPrice(@MappingTarget OrderResponse.OrderResponseBuilder response,
                               Order order) {
        if (order.getTotalPrice() != null) {
            response.totalPrice(order.getTotalPrice());
        }
    }
}