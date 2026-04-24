package com.example.server.models;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String shippingAddress;
    private String phoneNumber;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Long photoId;
        private String printSize;
        private Integer quantity;
        private String paperType;
    }
}