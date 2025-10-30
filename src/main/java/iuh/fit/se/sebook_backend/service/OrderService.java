package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.CreateOrderRequest;
import iuh.fit.se.sebook_backend.dto.OrderDTO;
import iuh.fit.se.sebook_backend.dto.OrderDetailDTO;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.*;
import iuh.fit.se.sebook_backend.utils.EmailSenderUtil;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;
    private final EmailSenderUtil emailSenderUtil;

    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository,
                        CartRepository cartRepository, BookRepository bookRepository,
                        SecurityUtil securityUtil, EmailSenderUtil emailSenderUtil, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
        this.emailSenderUtil = emailSenderUtil;
        this.notificationService = notificationService;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Cart> cartItems = cartRepository.findByAccountId(currentUser.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Order order = new Order();
        order.setAccount(currentUser);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        List<OrderDetail> orderDetails = new ArrayList<>();
        double totalAmount = 0;

        for (Cart cartItem : cartItems) {
            Book book = cartItem.getBook();
            int quantity = cartItem.getQuantity();

            if (book.getQuantity() < quantity) {
                throw new IllegalStateException("Not enough stock for book: " + book.getTitle());
            }

            book.setQuantity(book.getQuantity() - quantity);
            bookRepository.save(book);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setBook(book);
            detail.setQuantity(quantity);
            detail.setPriceAtPurchase(book.getPrice());
            orderDetails.add(detail);

            totalAmount += book.getPrice() * quantity;
        }

        order.setTotalAmount(totalAmount);
        order.setOrderDetails(orderDetails);

        Order savedOrder = orderRepository.save(order);

        cartRepository.deleteAll(cartItems);

        sendOrderConfirmationEmail(currentUser, savedOrder);


        return toDto(savedOrder);
    }

    public List<OrderDTO> getMyOrders() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Order> orders = orderRepository.findByAccountId(currentUser.getId());
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        String oldStatus = order.getStatus();

        // Kiểm tra logic chuyển đổi trạng thái hợp lệ (tùy chọn, có thể thêm sau)
        // Ví dụ: Không thể chuyển từ COMPLETED sang CANCELLED
        // Xử lý hoàn kho nếu trạng thái là CANCELLED hoặc RETURNED
        if ((newStatus.equals(Order.CANCELLED) && !oldStatus.equals(Order.CANCELLED)) ||
                (newStatus.equals(Order.RETURNED) && !oldStatus.equals(Order.RETURNED))) {
            returnInventory(order);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // TẠO THÔNG BÁO CHO NGƯỜI DÙNG
        String title = "Đơn hàng #" + order.getId() + " đã được cập nhật";
        String content = "Đơn hàng của bạn đã được chuyển sang trạng thái: " + newStatus;
        // 'null' nghĩa là sender là hệ thống
        notificationService.createNotification(null, order.getAccount(), title, content);

        return toDto(updatedOrder);
    }

    private void returnInventory(Order order) {
        for (OrderDetail detail : order.getOrderDetails()) {
            Book book = detail.getBook();
            int quantityToReturn = detail.getQuantity();
            book.setQuantity(book.getQuantity() + quantityToReturn);
            bookRepository.save(book);
        }
    }

    // --- Phương thức Lấy tất cả đơn hàng cho Admin/Staff ---
    public List<OrderDTO> getAllOrders() {
        // Chỉ lấy các trường cần thiết để tránh lỗi lazy loading account
        // Hoặc cấu hình FetchType.EAGER cho account trong Order entity nếu muốn lấy cả thông tin account
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    private OrderDTO toDto(Order order) {
        List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream()
                .map(detail -> OrderDetailDTO.builder()
                        .bookId(detail.getBook().getId())
                        .bookTitle(detail.getBook().getTitle())
                        .quantity(detail.getQuantity())
                        .priceAtPurchase(detail.getPriceAtPurchase())
                        .build())
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDetails(detailDTOs)
                .build();
    }

    private void sendOrderConfirmationEmail(Account account, Order order) {
        String subject = "Xác nhận đơn hàng #" + order.getId() + " tại SEBook";
        StringBuilder body = new StringBuilder();
        body.append("<h1>Cảm ơn bạn đã đặt hàng tại SEBook!</h1>");
        body.append("<p>Xin chào ").append(account.getUsername()).append(",</p>");
        body.append("<p>Đơn hàng của bạn đã được tạo thành công.</p>");
        body.append("<h2>Chi tiết đơn hàng #").append(order.getId()).append("</h2>");
        body.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        body.append("<thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>");
        body.append("<tbody>");
        for (OrderDetail detail : order.getOrderDetails()) {
            body.append("<tr>");
            body.append("<td>").append(detail.getBook().getTitle()).append("</td>");
            body.append("<td style='text-align:center;'>").append(detail.getQuantity()).append("</td>");
            body.append("<td style='text-align:right;'>").append(String.format("%,.0f", detail.getPriceAtPurchase())).append(" đ</td>");
            body.append("<td style='text-align:right;'>").append(String.format("%,.0f", detail.getPriceAtPurchase() * detail.getQuantity())).append(" đ</td>");
            body.append("</tr>");
        }
        body.append("</tbody></table>");
        body.append("<p style='text-align:right; font-weight:bold;'>Tổng cộng: ").append(String.format("%,.0f", order.getTotalAmount())).append(" đ</p>");
        body.append("<p>Trân trọng,<br/>Đội ngũ SEBook</p>");

        try {
            emailSenderUtil.sendEmail(account.getEmail(), subject, body.toString());
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận đơn hàng: " + e.getMessage());
        }
    }
}