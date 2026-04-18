package com.example.server.models;

public enum OrderStatus {
    PENDING("Ожидает обработки"),
    CONFIRMED("Подтвержден"),
    PRINTING("В печати"),
    READY("Готов к выдаче"),
    COMPLETED("Завершен"),
    CANCELLED("Отменен");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}