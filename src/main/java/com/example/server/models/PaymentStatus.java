package com.example.server.models;

public enum PaymentStatus {
    PENDING("Ожидает оплаты"),
    PAID("Оплачен"),
    FAILED("Ошибка оплаты"),
    REFUNDED("Возврат");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}