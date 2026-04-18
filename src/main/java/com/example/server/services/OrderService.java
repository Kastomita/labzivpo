package com.example.server.services;

import com.example.server.entities.Order;
import com.example.server.entities.OrderItem;
import com.example.server.entities.User;
import com.example.server.models.OrderStatus;
import com.example.server.models.PrintSize;
import com.example.server.repositories.OrderItemRepository;
import com.example.server.repositories.OrderRepository;
import com.example.server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(Order order) {
        User currentUser = getCurrentUser();

        order.setOrderNumber(generateOrderNumber());
        order.setUser(currentUser);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        // Вычислить общую сумму
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            PrintSize printSize = item.getPrintSize();
            item.setPricePerUnit(printSize.getPrice());
            item.setTotalPrice(printSize.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            item.setOrder(order);
            total = total.add(item.getTotalPrice());
        }
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            orderItemRepository.save(item);
        }

        return savedOrder;
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