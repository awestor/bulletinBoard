package ru.daniil.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.entity.base.discount.OrderDiscount;
import ru.daniil.core.response.discount.DiscountInfo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderDiscountMapper {

    @Mapping(target = "discountCode", source = "code")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "percentage", source = "percentage")
    @Mapping(target = "fixedAmount", source = "fixedAmount")
    @Mapping(target = "applicableCategoryId", source = "applicableCategoryId")
    DiscountInfo discountToOrderDiscountInfo(Discount discount);

    default DiscountInfo toOrderDiscountInfo(OrderDiscount orderDiscount) {
        return discountToOrderDiscountInfo(orderDiscount.getDiscount());
    }

    default List<DiscountInfo> toOrderDiscountInfoList(List<OrderDiscount> orderDiscounts) {
        return orderDiscounts.stream()
                .map(this::toOrderDiscountInfo)
                .toList();
    }
}
