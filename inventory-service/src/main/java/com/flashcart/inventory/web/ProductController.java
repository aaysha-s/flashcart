package com.flashcart.inventory.web;

import com.flashcart.inventory.product.Product;
import com.flashcart.inventory.product.ProductService;
import com.flashcart.inventory.product.StockReservationService;
import com.flashcart.inventory.web.dto.CreateProductRequest;
import com.flashcart.inventory.web.dto.ProductResponse;
import com.flashcart.inventory.web.dto.ReserveRequest;
import com.flashcart.inventory.web.dto.UpdateStockRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final StockReservationService reservationService;

    public ProductController(ProductService productService, StockReservationService reservationService) {
        this.productService = productService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public PagedModel<ProductResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(productService.list(pageable).map(ProductResponse::from));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(productService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse updateStock(@PathVariable Long id, @Valid @RequestBody UpdateStockRequest request) {
        return ProductResponse.from(productService.updateStock(id, request.stock()));
    }

    @PostMapping("/{id}/reserve")
    public ProductResponse reserve(@PathVariable Long id, @Valid @RequestBody ReserveRequest request) {
        return ProductResponse.from(reservationService.reserve(id, request.quantity()));
    }
}