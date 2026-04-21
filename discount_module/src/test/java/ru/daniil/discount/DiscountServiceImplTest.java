package ru.daniil.discount;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.daniil.core.entity.base.discount.Discount;
import ru.daniil.core.enums.DiscountType;
import ru.daniil.core.exceptions.DiscountNotActiveException;
import ru.daniil.core.request.discount.CreateDiscountRequest;
import ru.daniil.discount.mapper.DiscountMapper;
import ru.daniil.discount.repository.DiscountRepository;
import ru.daniil.discount.service.DiscountServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceImplTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private DiscountMapper discountMapper;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Discount discount;
    private CreateDiscountRequest createRequest;

    @BeforeEach
    void setUp() {
        discount = Discount.builder()
                .id(1L)
                .code("SUMMER2024")
                .name("Летняя скидка")
                .description("Скидка 20% на летние товары")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("20.00"))
                .fixedAmount(null)
                .applicableCategoryId(5L)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(1000)
                .usageCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CreateDiscountRequest.builder()
                .code("SUMMER2024")
                .name("Летняя скидка")
                .description("Скидка 20% на летние товары")
                .type("PERCENTAGE")
                .valuePercentage(new BigDecimal("20.00"))
                .valueAmount(null)
                .applicableCategoryId(5L)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .usageLimit(1000)
                .build();
    }

    @Test
    void getAllActiveDiscount_ShouldReturnListOfActiveDiscounts() {
        List<Discount> expectedDiscounts = List.of(discount);
        when(discountRepository.findAllActive(any(LocalDateTime.class))).thenReturn(expectedDiscounts);

        List<Discount> result = discountService.getAllActiveDiscount();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(discount.getCode(), result.getFirst().getCode());
        verify(discountRepository).findAllActive(any(LocalDateTime.class));
    }

    @Test
    void getAllActiveDiscount_WhenNoActiveDiscounts_ShouldReturnEmptyList() {
        when(discountRepository.findAllActive(any(LocalDateTime.class))).thenReturn(List.of());

        List<Discount> result = discountService.getAllActiveDiscount();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(discountRepository).findAllActive(any(LocalDateTime.class));
    }

    @Test
    void getDiscountByCode_WhenExists_ShouldReturnDiscount() {
        when(discountRepository.findByCode("SUMMER2024")).thenReturn(Optional.of(discount));

        Discount result = discountService.getDiscountByCode("SUMMER2024");

        assertNotNull(result);
        assertEquals("SUMMER2024", result.getCode());
        assertEquals("Летняя скидка", result.getName());
        verify(discountRepository).findByCode("SUMMER2024");
    }

    @Test
    void getDiscountByCode_WhenNotFound_ShouldThrowNotFoundException() {
        when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> discountService.getDiscountByCode("INVALID"));

        assertEquals("Скидка с данным кодом не найдена", exception.getMessage());
        verify(discountRepository).findByCode("INVALID");
    }

    @Test
    void createDiscount_WithValidRequest_ShouldCreateAndReturnDiscount() {
        when(discountRepository.findByCode(createRequest.getCode())).thenReturn(Optional.empty());
        when(discountMapper.toDiscount(createRequest)).thenReturn(discount);
        when(discountRepository.save(discount)).thenReturn(discount);

        Discount result = discountService.createDiscount(createRequest);

        assertNotNull(result);
        assertEquals(createRequest.getCode(), result.getCode());
        verify(discountRepository).findByCode(createRequest.getCode());
        verify(discountMapper).toDiscount(createRequest);
        verify(discountRepository).save(discount);
    }

    @Test
    void createDiscount_WhenCodeAlreadyExists_ShouldThrowIllegalArgumentException() {
        when(discountRepository.findByCode(createRequest.getCode())).thenReturn(Optional.of(discount));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> discountService.createDiscount(createRequest));

        assertEquals("Скидка с таким кодом уже существует", exception.getMessage());
        verify(discountRepository).findByCode(createRequest.getCode());
        verify(discountMapper, never()).toDiscount(any());
        verify(discountRepository, never()).save(any());
    }

    @Test
    void getActiveDiscountByCode_WhenDiscountIsAvailable_ShouldReturnDiscount() {
        when(discountRepository.findByCode("SUMMER2024")).thenReturn(Optional.of(discount));

        Discount result = discountService.getActiveDiscountByCode("SUMMER2024");

        assertNotNull(result);
        assertEquals("SUMMER2024", result.getCode());
        verify(discountRepository).findByCode("SUMMER2024");
    }

    @Test
    void getActiveDiscountByCode_WhenDiscountIsNotAvailable_ShouldThrowDiscountNotActiveException() {
        Discount expiredDiscount = Discount.builder()
                .id(2L)
                .code("EXPIRED")
                .name("Просроченная скидка")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("10.00"))
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1))
                .usageLimit(100)
                .usageCount(0)
                .build();

        when(discountRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredDiscount));

        assertThrows(DiscountNotActiveException.class,
                () -> discountService.getActiveDiscountByCode("EXPIRED"));

        verify(discountRepository).findByCode("EXPIRED");
    }

    @Test
    void getActiveDiscountByCode_WhenDiscountUsageLimitExceeded_ShouldThrowDiscountNotActiveException() {
        Discount exhaustedDiscount = Discount.builder()
                .id(3L)
                .code("EXHAUSTED")
                .name("Исчерпанная скидка")
                .type(DiscountType.SYSTEM)
                .percentage(new BigDecimal("15.00"))
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .usageLimit(100)
                .usageCount(100)
                .build();

        when(discountRepository.findByCode("EXHAUSTED")).thenReturn(Optional.of(exhaustedDiscount));

        assertThrows(DiscountNotActiveException.class,
                () -> discountService.getActiveDiscountByCode("EXHAUSTED"));

        verify(discountRepository).findByCode("EXHAUSTED");
    }

    @Test
    void getActiveDiscountByCode_WhenNotFound_ShouldThrowNotFoundException() {
        when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> discountService.getActiveDiscountByCode("INVALID"));

        assertEquals("Скидка с данным кодом не найдена", exception.getMessage());
        verify(discountRepository).findByCode("INVALID");
    }

    @Test
    void incrementUsageCount_ShouldCallRepository() {
        Long discountId = 1L;
        doNothing().when(discountRepository).incrementUsageCount(discountId);

        discountService.incrementUsageCount(discountId);

        verify(discountRepository).incrementUsageCount(discountId);
    }

    @Test
    void decrementUsageCount_ShouldCallRepository() {
        Long discountId = 1L;
        doNothing().when(discountRepository).decrementUsageCount(discountId);

        discountService.decrementUsageCount(discountId);

        verify(discountRepository).decrementUsageCount(discountId);
    }

    @Test
    void isAvailable_WhenDiscountIsActive_ShouldReturnTrue() {
        boolean result = discountService.isAvailable(discount);

        assertTrue(result);
    }

    @Test
    void isAvailable_WhenDiscountNotStarted_ShouldReturnFalse() {
        discount.setStartDate(LocalDateTime.now().plusDays(1));

        boolean result = discountService.isAvailable(discount);

        assertFalse(result);
    }

    @Test
    void isAvailable_WhenDiscountExpired_ShouldReturnFalse() {
        discount.setEndDate(LocalDateTime.now().minusDays(1));

        boolean result = discountService.isAvailable(discount);

        assertFalse(result);
    }

    @Test
    void isAvailable_WhenUsageLimitExceeded_ShouldReturnFalse() {
        discount.setUsageCount(1000);
        discount.setUsageLimit(1000);

        boolean result = discountService.isAvailable(discount);

        assertFalse(result);
    }

    @Test
    void isAvailable_WithNullDatesAndLimit_ShouldReturnTrue() {
        discount.setStartDate(null);
        discount.setEndDate(null);
        discount.setUsageLimit(null);

        boolean result = discountService.isAvailable(discount);

        assertTrue(result);
    }

    @Test
    void activateDiscount_WhenExists_ShouldSetUsageCountAndSave() {
        when(discountRepository.findByCode("SUMMER2024")).thenReturn(Optional.of(discount));
        when(discountRepository.save(discount)).thenReturn(discount);

        discountService.activateDiscount("SUMMER2024", 0);

        assertEquals(0, discount.getUsageCount());
        verify(discountRepository).findByCode("SUMMER2024");
        verify(discountRepository).save(discount);
    }

    @Test
    void activateDiscount_WhenNotFound_ShouldThrowIllegalArgumentException() {
        when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> discountService.activateDiscount("INVALID", 0));

        assertEquals("Скидка с кодом INVALID не найдена", exception.getMessage());
        verify(discountRepository).findByCode("INVALID");
        verify(discountRepository, never()).save(any());
    }

    @Test
    void deactivateDiscount_WhenExists_ShouldSetUsageCountToZeroAndSave() {
        discount.setUsageCount(500);
        when(discountRepository.findByCode("SUMMER2024")).thenReturn(Optional.of(discount));
        when(discountRepository.save(discount)).thenReturn(discount);

        discountService.deactivateDiscount("SUMMER2024");

        assertEquals(0, discount.getUsageCount());
        verify(discountRepository).findByCode("SUMMER2024");
        verify(discountRepository).save(discount);
    }

    @Test
    void deactivateDiscount_WhenNotFound_ShouldThrowIllegalArgumentException() {
        when(discountRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> discountService.deactivateDiscount("INVALID"));

        assertEquals("Скидка с кодом INVALID не найдена", exception.getMessage());
        verify(discountRepository).findByCode("INVALID");
        verify(discountRepository, never()).save(any());
    }

    @Test
    void isAvailable_WhenStartDateIsNow_ShouldReturnTrue() {
        discount.setStartDate(LocalDateTime.now());

        boolean result = discountService.isAvailable(discount);

        assertTrue(result);
    }

    @Test
    void isAvailable_WhenEndDateIsNow_ShouldReturnTrue() {
        discount.setEndDate(LocalDateTime.now());

        boolean result = discountService.isAvailable(discount);

        assertTrue(result);
    }
}