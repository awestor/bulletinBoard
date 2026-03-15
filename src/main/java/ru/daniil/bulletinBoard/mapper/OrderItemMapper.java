package ru.daniil.bulletinBoard.mapper;

import org.mapstruct.*;
import ru.daniil.bulletinBoard.entity.base.order.OrderItem;
import ru.daniil.bulletinBoard.entity.request.CreateOrderItemRequest;
import ru.daniil.bulletinBoard.entity.response.OrderItemResponse;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {ProductMapper.class})
public interface OrderItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "priceAtTime", ignore = true)
    @Mapping(target = "reservedUntil", ignore = true)
    OrderItem toEntity(CreateOrderItemRequest request);

    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "reservationExpired", expression = "java(orderItem.isExpired())")
    OrderItemResponse toResponse(OrderItem orderItem);

    List<OrderItemResponse> toResponseList(List<OrderItem> orderItems);
}