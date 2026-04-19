package ru.daniil.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.response.OrderResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface  OrderMapper {

    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "createdAt", source = "order.createdAt")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);
}
