package ru.daniil.order.service.payment;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;
import ru.daniil.core.entity.base.order.Order;
import ru.daniil.core.entity.base.order.OrderItem;
import ru.daniil.core.entity.base.user.User;
import ru.daniil.core.entity.base.wallet.Wallet;
import ru.daniil.core.entity.base.wallet.WalletTransaction;
import ru.daniil.core.enums.OrderStatus;
import ru.daniil.core.enums.wallet.TransactionStatus;
import ru.daniil.core.enums.wallet.TransactionType;
import ru.daniil.core.request.payment.UpdateStatusPaymentRequest;
import ru.daniil.core.response.payment.PaymentCompleteResponse;
import ru.daniil.core.response.payment.PaymentInitiateResponse;
import ru.daniil.core.response.payment.SellerAmountDto;
import ru.daniil.order.mapper.PaymentMapper;
import ru.daniil.order.service.order.OrderService;
import ru.daniil.order.service.orderItem.OrderItemService;
import ru.daniil.order.service.wallet.WalletService;
import ru.daniil.product.service.product.ProductService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentProcessorServiceImpl implements PaymentProcessorService {
    private final WalletService walletService;
    private final OrderItemService orderItemService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PaymentMapper paymentMapper;

    public PaymentProcessorServiceImpl(WalletService walletService,
                                       OrderItemService orderItemService,
                                       OrderService orderService,
                                       ProductService productService, PaymentMapper paymentMapper) {
        this.walletService = walletService;
        this.orderItemService = orderItemService;
        this.orderService = orderService;
        this.productService = productService;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(User user) {

        Order order = orderService.getLastOrCreateOrderByUser(user);

        if (order.getStatus() != OrderStatus.DRAFT && order.getItems() != null) {
            throw new IllegalStateException("Оплата возможна только для черновых вариантов заказа");
        }

        Wallet wallet = walletService.getOrCreateWallet(user);

        BigDecimal totalPrice = order.getTotalPrice();
        if (!walletService.hasSufficientBalance(wallet, totalPrice)) {
            BigDecimal available = walletService.getAvailableBalance(wallet);
            throw new IllegalStateException(
                    String.format("Недостаточно средств. Доступно: %s, требуется: %s", available, totalPrice)
            );
        }

        List<OrderItem> orderItems = orderItemService.getByOrderNumber(order.getOrderNumber());

        if (orderItems.isEmpty()) {
            throw new IllegalStateException("Нельзя оплатить пустой заказ");
        }

        Map<String, Integer> skuQuantityMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            String sku = item.getProduct().getSku();
            Integer quantity = item.getQuantity();
            skuQuantityMap.merge(sku, quantity, Integer::sum);
        }

        try {
            productService.decreaseStockQuantities(skuQuantityMap);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Некоторые товары недоступны в запрошенном количестве: "
                    + e.getMessage(), e);
        }

        walletService.freezeFunds(
                wallet,
                totalPrice,
                order,
                "Заморозка средств для заказа " + order.getOrderNumber()
        );

        orderService.updateStatus(order.getId(), OrderStatus.PROCESSING);

        return paymentMapper.toInitiateResponse(order);
    }

    @Override
    @Transactional
    public PaymentCompleteResponse completePayment(UpdateStatusPaymentRequest request, User user) {
        Order order = orderService.getByOrderNumber(request.getOrderNumber());

        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Заказ не принадлежит пользователю");
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Заказ не находится в ожидании оплаты");
        }

        WalletTransaction freezeTransaction = walletService.getActiveFreezeByOrderNumber(order)
                .orElseThrow(() -> new IllegalStateException("Активная заморозка для заказа не найдена"));

        WalletTransaction withdrawalTransaction = walletService.confirmWithdrawal(freezeTransaction.getId());

        List<OrderItem> orderItems = orderItemService.getByOrderNumber(order.getOrderNumber());
        List<SellerAmountDto> sellerAmounts = calculateSellerAmounts(orderItems);

        distributeFundsToSellers(order, sellerAmounts);

        orderService.updateStatus(order.getId(), OrderStatus.PAID);

        return paymentMapper.toCompleteResponse(order, withdrawalTransaction);
    }

    @Override
    @Transactional
    public PaymentCompleteResponse cancelPayment(UpdateStatusPaymentRequest request, User user) {
        Order order = orderService.getByOrderNumber(request.getOrderNumber());

        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Заказ не принадлежит пользователю");
        }

        if (!(order.getStatus() == OrderStatus.PROCESSING)){
            throw new BadRequestException("Заказ не находится в состоянии ожидания оплаты");
        }

        List<OrderItem> orderItems = orderItemService.getByOrderNumber(request.getOrderNumber());
        Map<String, Integer> skuQuantityMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            String sku = item.getProduct().getSku();
            Integer quantity = item.getQuantity();
            skuQuantityMap.merge(sku, quantity, Integer::sum);
        }
        productService.increaseStockQuantities(skuQuantityMap);

        WalletTransaction freezeTransaction =
                walletService.getActiveFreezeByOrderNumber(order).orElseThrow(
                        () -> new NotFoundException("Транзакция с замороженными средствами не найдена")
                );

        walletService.unfreezeFunds(freezeTransaction.getId());

        orderService.updateStatus(order.getId(), OrderStatus.DRAFT);

        return paymentMapper.toCancelResponse(order.getOrderNumber());
    }

    private List<SellerAmountDto> calculateSellerAmounts(List<OrderItem> orderItems) {
        Map<Long, SellerAmountDto> sellerMap = new HashMap<>();

        for (OrderItem item : orderItems) {
            User seller = item.getProduct().getSeller();
            BigDecimal itemTotal = item.getPriceAtTime();

            SellerAmountDto dto = sellerMap.get(seller.getId());
            if (dto == null) {
                dto = SellerAmountDto.builder()
                        .seller(seller)
                        .amount(BigDecimal.ZERO)
                        .build();
                sellerMap.put(seller.getId(), dto);
            }
            dto.setAmount(dto.getAmount().add(itemTotal));
        }

        return new ArrayList<>(sellerMap.values());
    }

    private void distributeFundsToSellers(Order order,
                                          List<SellerAmountDto> sellerAmounts) {
        for (SellerAmountDto dto : sellerAmounts) {
            BigDecimal amount = dto.getAmount();

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            User seller = dto.getSeller();

            Wallet sellerWallet = walletService.getOrCreateWallet(seller);

            walletService.increaseBalance(sellerWallet.getId(), amount);

            walletService.createTransaction(
                    sellerWallet,
                    amount,
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    "Поступление средств от заказа " + order.getOrderNumber(),
                    null,
                    order
            );
        }
    }
}
