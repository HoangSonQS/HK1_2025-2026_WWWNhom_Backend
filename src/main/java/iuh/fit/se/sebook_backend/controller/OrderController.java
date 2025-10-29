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

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest request) {
        OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }

    // Endpoint cho Admin/Staff xem tất cả đơn hàng
    @GetMapping("/all")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}