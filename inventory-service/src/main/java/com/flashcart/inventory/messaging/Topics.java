package com.flashcart.inventory.messaging;

public final class Topics {

    public static final String ORDER_PLACED = "order.placed";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_REJECTED = "stock.rejected";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String STOCK_CHANGED = "stock.changed";

    private Topics() {
    }
}
