package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.CreateOrderRequest;
import iuh.fit.se.sebook_backend.dto.OrderDTO;
import iuh.fit.se.sebook_backend.dto.UpdateOrderStatusRequest;
import iuh.fit.se.sebook_backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDTO createdOrder = orderService.createOrder(request);
        return ResponseEntity.ok(createdOrder);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest request) {
        OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long orderId) {
        OrderDTO updatedOrder = orderService.cancelOrderByCustomer(orderId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{orderId}/confirm-received")
    public ResponseEntity<OrderDTO> confirmReceived(@PathVariable Long orderId) {
        OrderDTO updatedOrder = orderService.confirmReceivedByCustomer(orderId);
        return ResponseEntity.ok(updatedOrder);
    }

    // Endpoint cho Admin/Staff xem tất cả đơn hàng
    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}