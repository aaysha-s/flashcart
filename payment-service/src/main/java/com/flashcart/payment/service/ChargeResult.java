package com.flashcart.payment.service;

public record ChargeResult(boolean success, String failureReason) {

    public static ChargeResult approved() {
        return new ChargeResult(true, null);
    }

    public static ChargeResult declined(String reason) {
        return new ChargeResult(false, reason);
    }
}
