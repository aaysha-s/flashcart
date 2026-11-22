package com.flashcart.inventory.product;

import com.flashcart.inventory.web.dto.CreateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.flashcart.inventory.web.dto.ProductResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderReservationRepository reservationRepository;
    private final ProductSearchService searchService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

    private final ApplicationEventPublisher events;

    public ProductService(ProductRepository productRepository,
                          OrderReservationRepository reservationRepository,
                          ProductSearchService searchService,
                          ApplicationEventPublisher events) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.searchService = searchService;
        this.events = events;
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

        Product product = productRepository.save(new Product(request.name().trim(), request.description(),
                request.pricePaise(), request.stock(), request.saleStartsAt()));

        try {
            searchService.index(product);
        } catch (RuntimeException e) {
            log.warn("Could not index product {} for search; run reindex later", product.getId(), e);
        }
        announce(product);
        return product;
    }

    @CacheEvict(cacheNames = "products", key = "#id")
    @Transactional
    public Product updateStock(Long id, int stock) {
        Product product = get(id);
        product.setStock(stock);
        announce(product);
        return product;
    }

    @CacheEvict(cacheNames = "products", key = "#id")
    @Transactional
    public Product reserve(Long id, int quantity) {
        Product product = get(id);
        requireOnSale(product);
        product.reserve(quantity);
        announce(product);
        return product;
    }

    @CacheEvict(cacheNames = "products", key = "#productId")
    @Transactional
    public OrderReservation reserveForOrder(Long orderId, Long productId, int quantity) {
        Product product = get(productId);
        requireOnSale(product);
        product.reserve(quantity);
        announce(product);
        return reservationRepository.save(OrderReservation.reserved(
                orderId, productId, quantity, product.getPricePaise() * quantity));
    }

    @CacheEvict(cacheNames = "products", key = "#productId")
    @Transactional
    public boolean releaseForOrder(Long orderId, Long productId) {
        OrderReservation reservation = reservationRepository.findByOrderId(orderId).orElse(null);
        if (reservation == null || !reservation.isReserved()) {
            return false; // nothing reserved, or already released: safe to ignore
        }
        Product product = get(reservation.getProductId());
        product.release(reservation.getQuantity());
        reservation.markReleased();
        announce(product);
        return true;
    }

    @Transactional
    public OrderReservation recordRejection(Long orderId, Long productId, int quantity, String reason) {
        return reservationRepository.save(OrderReservation.rejected(orderId, productId, quantity, reason));
    }

    @Cacheable(cacheNames = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getResponse(Long id) {
        return ProductResponse.from(get(id));
    }

    private void announce(Product product) {
        events.publishEvent(new StockChanged(product.getId(), product.getStock(), Instant.now()));
    }

    private static void requireOnSale(Product product) {
        if (!product.isOnSale(Instant.now())) {
            throw new SaleNotStartedException(product.getId(), product.getSaleStartsAt());
        }
    }
}
