package com.flashcart.inventory.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    ProductService productService;

    @InjectMocks
    StockReservationService reservationService;

    private final ObjectOptimisticLockingFailureException conflict =
            new ObjectOptimisticLockingFailureException(Product.class, 1L);

    @Test
    void retriesAfterConflictsAndSucceeds() {
        Product product = new Product("Headphones", "Wireless", 299900, 5);
        when(productService.reserve(1L, 1))
                .thenThrow(conflict)
                .thenThrow(conflict)
                .thenReturn(product);

        Product result = reservationService.reserve(1L, 1);

        assertThat(result).isSameAs(product);
        verify(productService, times(3)).reserve(1L, 1);
    }

    @Test
    void givesUpAfterTenConflicts() {
        when(productService.reserve(1L, 1)).thenThrow(conflict);

        assertThatThrownBy(() -> reservationService.reserve(1L, 1))
                .isInstanceOf(StockConflictException.class);
        verify(productService, times(10)).reserve(1L, 1);
    }
}