package com.example.server.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum PrintSize {
    SIZE_10x15("10x15 см", new BigDecimal("50")),
    SIZE_15x21("15x21 см", new BigDecimal("100")),
    SIZE_20x30("20x30 см", new BigDecimal("150")),
    SIZE_30x40("30x40 см", new BigDecimal("250"));

    private final String description;
    private final BigDecimal price;
}