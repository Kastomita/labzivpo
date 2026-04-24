package com.example.server.services;

import com.example.server.entities.Order;
import com.example.server.entities.OrderItem;
import com.example.server.entities.Photo;
import com.example.server.entities.User;
import com.example.server.models.CreateOrderRequest;
import com.example.server.models.OrderStatus;
import com.example.server.models.PrintSize;
import com.example.server.repositories.OrderItemRepository;
import com.example.server.repositories.OrderRepository;
import com.example.server.repositories.PhotoRepository;
import com.example.server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        User currentUser = getCurrentUser();

        if (request.getItems() == null) {
            request.setItems(new ArrayList<>());
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(currentUser)
                .shippingAddress(request.getShippingAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Photo photo = photoRepository.findById(itemRequest.getPhotoId())
                    .orElseThrow(() -> new RuntimeException("Фото не найдено: " + itemRequest.getPhotoId()));

            PrintSize printSize = PrintSize.valueOf(itemRequest.getPrintSize());
            BigDecimal pricePerUnit = printSize.getPrice();
            BigDecimal itemTotal = pricePerUnit.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .photo(photo)
                    .printSize(printSize)
                    .quantity(itemRequest.getQuantity())
                    .pricePerUnit(pricePerUnit)
                    .totalPrice(itemTotal)
                    .paperType(itemRequest.getPaperType())
                    .build();

            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
            totalAmount = totalAmount.add(itemTotal);
        }

        savedOrder.setTotalAmount(totalAmount);
        return orderRepository.save(savedOrder);
    }

    public List<Order> getUserOrders() {
        User currentUser = getCurrentUser();
        return orderRepository.findByUserId(currentUser.getId());
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = getOrderById(id);
        User currentUser = getCurrentUser();

        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Нет прав на отмену этого заказа");
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        } else {
            throw new RuntimeException("Невозможно отменить заказ в статусе: " + order.getStatus());
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        order.setStatus(OrderStatus.valueOf(status));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
}