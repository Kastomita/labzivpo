package com.example.server.entities;

import com.example.server.models.PrintSize;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    @JsonIgnoreProperties({"user", "orderItems", "hibernateLazyInitializer", "handler"})
    private Photo photo;

    @Enumerated(EnumType.STRING)
    private PrintSize printSize;

    private Integer quantity;

    private BigDecimal pricePerUnit;

    private BigDecimal totalPrice;

    private String paperType;
}