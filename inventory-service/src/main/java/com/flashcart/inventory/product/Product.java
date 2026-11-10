package com.flashcart.inventory.product;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "price_paise", nullable = false)
    private long pricePaise;

    @Column(nullable = false)
    private int stock;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
        // required by JPA
    }

    public Product(String name, String description, long pricePaise, int stock) {
        this.name = name;
        this.description = description;
        this.pricePaise = pricePaise;
        this.stock = stock;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (stock < quantity) {
            throw new InsufficientStockException(id, quantity, stock);
        }
        this.stock -= quantity;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getPricePaise() { return pricePaise; }
    public int getStock() { return stock; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}