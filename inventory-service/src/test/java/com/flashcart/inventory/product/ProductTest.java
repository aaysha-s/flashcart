package com.flashcart.inventory.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product productWithStock(int stock) {
        return new Product("Headphones", "Wireless", 299900, stock);
    }

    @Test
    void reserve_reducesStock() {
        Product product = productWithStock(5);

        product.reserve(3);

        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void reserve_allowsTakingTheLastUnits() {
        Product product = productWithStock(3);

        product.reserve(3);

        assertThat(product.getStock()).isZero();
    }

    @Test
    void reserve_rejectsMoreThanAvailable_andLeavesStockUnchanged() {
        Product product = productWithStock(2);

        assertThatThrownBy(() -> product.reserve(3))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void reserve_rejectsZeroOrNegativeQuantity() {
        Product product = productWithStock(5);

        assertThatThrownBy(() -> product.reserve(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> product.reserve(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setStock_rejectsNegativeValues() {
        Product product = productWithStock(5);

        assertThatThrownBy(() -> product.setStock(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
