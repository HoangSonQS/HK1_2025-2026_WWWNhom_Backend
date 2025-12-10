package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.*;
import iuh.fit.se.sebook_backend.utils.EmailSenderUtil;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final AddressRepository addressRepository;
    private final PromotionRepository promotionRepository;
    private final SecurityUtil securityUtil;
    private final EmailSenderUtil emailSenderUtil;

    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository,
            CartRepository cartRepository, BookRepository bookRepository,
            AddressRepository addressRepository, PromotionRepository promotionRepository,
            SecurityUtil securityUtil, EmailSenderUtil emailSenderUtil, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.addressRepository = addressRepository;
        this.promotionRepository = promotionRepository;
        this.securityUtil = securityUtil;
        this.emailSenderUtil = emailSenderUtil;
        this.notificationService = notificationService;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Cart> allCartItems = cartRepository.findByAccountId(currentUser.getId());

        if (allCartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Chỉ lấy các cart item được chọn (nếu FE gửi danh sách cartItemIds)
        List<Long> selectedIds = request.getCartItemIds();
        List<Cart> cartItems;
        if (selectedIds != null && !selectedIds.isEmpty()) {
            cartItems = allCartItems.stream()
                    .filter(ci -> selectedIds.contains(ci.getId()))
                    .collect(Collectors.toList());
            if (cartItems.isEmpty()) {
                throw new IllegalStateException("Không có sản phẩm nào được chọn để đặt hàng");
            }
        } else {
            cartItems = allCartItems;
        }

        // Validate and set address if provided
        Address deliveryAddress = null;
        if (request.getAddressId() != null) {
            deliveryAddress = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));
            if (!deliveryAddress.getAccount().getId().equals(currentUser.getId())) {
                throw new IllegalStateException("Address does not belong to current user");
            }
        }

        // Validate and apply promotion if provided
        Promotion promotion = null;
        if (request.getPromotionCode() != null && !request.getPromotionCode().trim().isEmpty()) {
            promotion = promotionRepository
                    .findByCodeAndIsActiveTrueAndEndDateAfter(request.getPromotionCode().trim(), LocalDate.now())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired promotion code"));

            // Check if promotion quantity is still available
            if (promotion.getQuantity() <= 0) {
                throw new IllegalStateException("Promotion code has been fully used");
            }
        }

        Order order = new Order();
        order.setAccount(currentUser);
        order.setOrderDate(LocalDateTime.now());
        // Với VNPay: khởi tạo trạng thái chưa thanh toán, chỉ chuyển sang PENDING sau khi thanh toán thành công
        boolean isVnPay = "VNPAY".equalsIgnoreCase(request.getPaymentMethod());
        order.setStatus(isVnPay ? Order.UNPAID : Order.PENDING);
        if (promotion != null) {
            order.setAppliedPromotion(promotion);
            // Decrease promotion quantity
            promotion.setQuantity(promotion.getQuantity() - 1);
            promotionRepository.save(promotion);
        }
        if (deliveryAddress != null) {
            order.setDeliveryAddress(deliveryAddress);
        }
        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }

        List<OrderDetail> orderDetails = new ArrayList<>();
        double subtotal = 0;

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

            subtotal += book.getPrice() * quantity;
        }

        // Apply promotion discount
        double discountAmount = 0;
        double totalAmount = subtotal;
        if (promotion != null) {
            discountAmount = subtotal * (promotion.getDiscountPercent() / 100.0);
            totalAmount = subtotal - discountAmount;
        }

        order.setTotalAmount(totalAmount);
        order.setOrderDetails(orderDetails);

        Order savedOrder = orderRepository.save(order);

        // Chỉ xoá các item đã được đặt hàng, giữ lại các item khác trong giỏ
        cartRepository.deleteAll(cartItems);

        // Gửi email ngay với phương thức thanh toán không phải VNPAY
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            sendOrderConfirmationEmail(savedOrder);
        }

        // Tạo thông báo khi đặt hàng thành công
        String title = "Đơn hàng #" + savedOrder.getId() + " đã được tạo";
        String content = "Đặt hàng thành công, đơn hàng của bạn đang chờ xác nhận";
        notificationService.createNotification(null, currentUser, title, content);

        return toDto(savedOrder);
    }

    public List<OrderDTO> getMyOrders() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Order> orders = orderRepository.findByAccountId(currentUser.getId());
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long orderId) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("admin"));
        boolean isSellerStaff = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("seller_staff"));
        boolean isWarehouseStaff = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("warehouse_staff"));

        // Admin hoặc Staff (seller/warehouse) được xem tất cả. Người dùng thường chỉ xem đơn của mình.
        if (!isAdmin && !isSellerStaff && !isWarehouseStaff
                && !order.getAccount().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You don't have permission to view this order");
        }

        // Trigger lazy loading cho các quan hệ cần thiết trước khi gọi toDto
        order.getOrderDetails().size();
        if (order.getAppliedPromotion() != null) {
            order.getAppliedPromotion().getCode();
        }
        if (order.getDeliveryAddress() != null) {
            order.getDeliveryAddress().getStreet();
        }
        order.getAccount().getEmail();

        return toDto(order);
    }

    public OrderDTO updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        String oldStatus = order.getStatus();

        // Nếu trạng thái không thay đổi, không cần làm gì
        if (oldStatus.equals(newStatus)) {
            return toDto(order);
        }

        // Không cho phép cập nhật nếu đã hủy hoặc đã trả lại
        if (oldStatus.equals(Order.CANCELLED)) {
            throw new IllegalStateException("Không thể cập nhật trạng thái đơn hàng đã bị hủy");
        }
        if (oldStatus.equals(Order.RETURNED)) {
            throw new IllegalStateException("Không thể cập nhật trạng thái đơn hàng đã trả lại");
        }

        // Cho phép chuyển từ COMPLETED -> RETURNED (xử lý hoàn/đổi), chặn các chuyển đổi khác từ COMPLETED
        if (oldStatus.equals(Order.COMPLETED) && !newStatus.equals(Order.RETURNED)) {
            throw new IllegalStateException("Không thể cập nhật trạng thái đơn hàng đã hoàn thành");
        }

        // Bảo vệ luồng chuyển trạng thái hợp lệ
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalStateException("Chuyển trạng thái không hợp lệ từ " + oldStatus + " sang " + newStatus);
        }

        // Xử lý hoàn kho nếu trạng thái là CANCELLED hoặc RETURNED
        if ((newStatus.equals(Order.CANCELLED) && !oldStatus.equals(Order.CANCELLED)) ||
                (newStatus.equals(Order.RETURNED) && !oldStatus.equals(Order.RETURNED))) {
            returnInventory(order);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // TẠO THÔNG BÁO CHO NGƯỜI DÙNG - Mỗi khi trạng thái thay đổi
        createStatusChangeNotification(updatedOrder, newStatus);

        return toDto(updatedOrder);
    }

    /**
     * Kiểm tra luồng chuyển trạng thái hợp lệ:
     * PENDING -> PROCESSING hoặc CANCELLED
     * PROCESSING -> DELIVERING hoặc CANCELLED
     * DELIVERING -> COMPLETED hoặc PROCESSING (trả về xử lý khi giao thất bại) hoặc CANCELLED
     * COMPLETED -> RETURNED (xử lý hoàn/đổi)
     * CANCELLED/RETURNED -> không cho chuyển tiếp
     */
    private boolean isValidTransition(String oldStatus, String newStatus) {
        return switch (oldStatus) {
            case Order.UNPAID -> newStatus.equals(Order.PENDING) || newStatus.equals(Order.CANCELLED);
            case Order.PENDING -> newStatus.equals(Order.PROCESSING) || newStatus.equals(Order.CANCELLED);
            case Order.PROCESSING -> newStatus.equals(Order.DELIVERING) || newStatus.equals(Order.CANCELLED);
            case Order.DELIVERING -> newStatus.equals(Order.COMPLETED)
                    || newStatus.equals(Order.PROCESSING)
                    || newStatus.equals(Order.CANCELLED);
            case Order.COMPLETED -> newStatus.equals(Order.RETURNED);
            case Order.CANCELLED, Order.RETURNED -> false;
            default -> false;
        };
    }

    /**
     * Helper method để tạo thông báo khi trạng thái đơn hàng thay đổi
     */
    private void createStatusChangeNotification(Order order, String newStatus) {
        String title = "Đơn hàng #" + order.getId() + " đã được cập nhật";
        String content;
        if (newStatus.equals(Order.PENDING)) {
            content = "Đặt hàng thành công, đơn hàng của bạn đang chờ xác nhận";
        } else if (newStatus.equals(Order.PROCESSING)) {
            content = "Đơn hàng đã được xác nhận và đang được chuẩn bị";
        } else if (newStatus.equals(Order.COMPLETED)) {
            content = "Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua sắm!";
        } else if (newStatus.equals(Order.CANCELLED)) {
            content = "Đơn hàng đã được hủy";
        } else if (newStatus.equals(Order.RETURNED)) {
            content = "Đơn hàng đã được trả lại";
        } else if (newStatus.equals(Order.DELIVERING)) {
            content = "Đơn hàng đang trên đường giao đến bạn";
        } else {
            content = "Đơn hàng của bạn đã được chuyển sang trạng thái: " + newStatus;
        }
        // 'null' nghĩa là sender là hệ thống
        notificationService.createNotification(null, order.getAccount(), title, content);
    }

    public OrderDTO cancelOrderByCustomer(Long orderId) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        // Trigger lazy loading để tránh lỗi
        Long accountId = order.getAccount().getId();

        // Kiểm tra order có thuộc về user hiện tại không
        if (!accountId.equals(currentUser.getId())) {
            throw new IllegalStateException("You don't have permission to cancel this order");
        }

        // Chỉ cho phép hủy khi trạng thái là PENDING hoặc UNPAID (chưa thanh toán VNPay)
        if (!(order.getStatus().equals(Order.PENDING) || order.getStatus().equals(Order.UNPAID))) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi trạng thái là Chờ xác nhận/Chưa thanh toán");
        }

        order.setStatus(Order.CANCELLED);

        // Hoàn kho
        returnInventory(order);

        Order updatedOrder = orderRepository.save(order);

        // Trigger lazy loading cho các quan hệ cần thiết trước khi gọi toDto
        updatedOrder.getOrderDetails().size();
        if (updatedOrder.getAppliedPromotion() != null) {
            updatedOrder.getAppliedPromotion().getCode();
        }
        if (updatedOrder.getDeliveryAddress() != null) {
            updatedOrder.getDeliveryAddress().getStreet();
        }
        updatedOrder.getAccount().getEmail();

        // Tạo thông báo khi trạng thái thay đổi
        createStatusChangeNotification(updatedOrder, Order.CANCELLED);

        return toDto(updatedOrder);
    }

    public OrderDTO confirmReceivedByCustomer(Long orderId) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        // Trigger lazy loading để tránh lỗi
        Long accountId = order.getAccount().getId();

        // Kiểm tra order có thuộc về user hiện tại không
        if (!accountId.equals(currentUser.getId())) {
            throw new IllegalStateException("You don't have permission to confirm this order");
        }

        // Chỉ cho phép xác nhận khi trạng thái là DELIVERING
        if (!order.getStatus().equals(Order.DELIVERING)) {
            throw new IllegalStateException(
                    "Chỉ có thể xác nhận đã nhận hàng khi đơn hàng đang trong trạng thái Đang giao");
        }

        order.setStatus(Order.COMPLETED);
        Order updatedOrder = orderRepository.save(order);

        // Trigger lazy loading cho các quan hệ cần thiết trước khi gọi toDto
        updatedOrder.getOrderDetails().size();
        if (updatedOrder.getAppliedPromotion() != null) {
            updatedOrder.getAppliedPromotion().getCode();
        }
        if (updatedOrder.getDeliveryAddress() != null) {
            updatedOrder.getDeliveryAddress().getStreet();
        }
        updatedOrder.getAccount().getEmail();

        // Tạo thông báo khi trạng thái thay đổi
        createStatusChangeNotification(updatedOrder, Order.COMPLETED);

        return toDto(updatedOrder);
    }

    public void returnInventory(Order order) {
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
        // Hoặc cấu hình FetchType.EAGER cho account trong Order entity nếu muốn lấy cả
        // thông tin account
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    private OrderDTO toDto(Order order) {
        List<OrderDetailDTO> detailDTOs = order.getOrderDetails().stream()
                .map(detail -> OrderDetailDTO.builder()
                        .bookId(detail.getBook().getId())
                        .bookTitle(detail.getBook().getTitle())
                        .bookImageUrl(detail.getBook().getImageUrl())
                        .quantity(detail.getQuantity())
                        .priceAtPurchase(detail.getPriceAtPurchase())
                        .build())
                .collect(Collectors.toList());

        // Calculate subtotal and discount
        double subtotal = order.getOrderDetails().stream()
                .mapToDouble(detail -> detail.getPriceAtPurchase() * detail.getQuantity())
                .sum();
        double discountAmount = subtotal - order.getTotalAmount();

        // Build promotion info if exists
        PromotionInfoDTO promotionInfo = null;
        if (order.getAppliedPromotion() != null) {
            Promotion promo = order.getAppliedPromotion();
            promotionInfo = PromotionInfoDTO.builder()
                    .code(promo.getCode())
                    .name(promo.getName())
                    .discountPercent(promo.getDiscountPercent())
                    .build();
        }

        // Build address info if exists
        AddressDTO addressDTO = null;
        if (order.getDeliveryAddress() != null) {
            Address addr = order.getDeliveryAddress();
            addressDTO = AddressDTO.builder()
                    .id(addr.getId())
                    .addressType(addr.getAddressType())
                    .isDefault(addr.isDefault())
                    .street(addr.getStreet())
                    .ward(addr.getWard())
                    .district(addr.getDistrict())
                    .city(addr.getCity())
                    .phoneNumber(addr.getPhoneNumber())
                    .recipientName(addr.getRecipientName())
                    .build();
        }

        // Build customer info
        Account account = order.getAccount();
        CustomerInfoDTO customerInfo = CustomerInfoDTO.builder()
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .build();

        return OrderDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryAddress(addressDTO)
                .appliedPromotion(promotionInfo)
                .customerInfo(customerInfo)
                .orderDetails(detailDTOs)
                .build();
    }

    public void sendOrderConfirmationEmail(Order order) {
        Account account = order.getAccount();
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
            body.append("<td style='text-align:right;'>").append(String.format("%,.0f", detail.getPriceAtPurchase()))
                    .append(" đ</td>");
            body.append("<td style='text-align:right;'>")
                    .append(String.format("%,.0f", detail.getPriceAtPurchase() * detail.getQuantity()))
                    .append(" đ</td>");
            body.append("</tr>");
        }
        body.append("</tbody></table>");
        double subtotal = order.getOrderDetails().stream()
                .mapToDouble(d -> d.getPriceAtPurchase() * d.getQuantity())
                .sum();
        double discountAmount = subtotal - order.getTotalAmount();
        body.append("<p style='text-align:right; margin:4px 0;'>Tạm tính: ")
                .append(String.format("%,.0f", subtotal)).append(" đ</p>");
        if (discountAmount > 0) {
            body.append("<p style='text-align:right; margin:4px 0;'>Giảm giá: -")
                    .append(String.format("%,.0f", discountAmount)).append(" đ</p>");
        }
        body.append("<p style='text-align:right; font-weight:bold;'>Thanh toán: ")
                .append(String.format("%,.0f", order.getTotalAmount())).append(" đ</p>");
        body.append("<p>Trân trọng,<br/>Đội ngũ SEBook</p>");

        try {
            emailSenderUtil.sendEmail(account.getEmail(), subject, body.toString());
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận đơn hàng: " + e.getMessage());
        }
    }
}