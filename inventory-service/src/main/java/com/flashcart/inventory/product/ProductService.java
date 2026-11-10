package com.flashcart.inventory.product;

import com.flashcart.inventory.web.dto.CreateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> list(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        Product product = new Product(request.name().trim(), request.description(),
                request.pricePaise(), request.stock());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateStock(Long id, int stock) {
        Product product = get(id);
        product.setStock(stock);
        return product;
    }
}
