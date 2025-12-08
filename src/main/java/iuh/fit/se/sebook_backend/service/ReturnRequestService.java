package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.ReturnRequestCreateDTO;
import iuh.fit.se.sebook_backend.dto.ReturnRequestProcessDTO;
import iuh.fit.se.sebook_backend.dto.ReturnRequestResponseDTO;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import iuh.fit.se.sebook_backend.repository.ReturnRequestRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("DataFlowIssue")
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;
    private final OrderService orderService;

    public ReturnRequestService(ReturnRequestRepository returnRequestRepository,
                                OrderRepository orderRepository,
                                SecurityUtil securityUtil,
                                OrderService orderService) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.securityUtil = securityUtil;
        this.orderService = orderService;
    }

    @Transactional
    public ReturnRequestResponseDTO create(ReturnRequestCreateDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        Long orderId = Objects.requireNonNull(dto.getOrderId(), "Order id is null");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Chỉ cho phép tạo yêu cầu nếu đơn thuộc về tài khoản hiện tại hoặc current là seller staff
        boolean isSeller = current.getRoles().stream().anyMatch(r -> "SELLER_STAFF".equalsIgnoreCase(r.getName()));
        if (!isSeller && !order.getAccount().getId().equals(current.getId())) {
            throw new IllegalStateException("Không thể tạo yêu cầu cho đơn hàng không thuộc bạn");
        }

        if (!Order.COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("Chỉ cho phép yêu cầu trả hàng khi đơn đã hoàn thành");
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrder(order);
        request.setReason(dto.getReason());
        request.setStatus(ReturnRequestStatus.PENDING);
        request.setCreatedBy(current);
        request.setCreatedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepository.save(request);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestResponseDTO> getMyRequests() {
        Account current = securityUtil.getLoggedInAccount();
        Long accId = Objects.requireNonNull(current.getId(), "Account id is null");
        return returnRequestRepository.findByCreatedByIdOrderByCreatedAtDesc(accId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestResponseDTO> getAllRequests() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("DataFlowIssue")
    public ReturnRequestResponseDTO approve(Long id, ReturnRequestProcessDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        if (req.getStatus() != ReturnRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu đã được xử lý");
        }

        // Cập nhật kho và trạng thái đơn
        Long orderIdObj = Objects.requireNonNull(req.getOrder().getId(), "Order id is null");
        orderService.updateOrderStatus(orderIdObj, Order.RETURNED);

        req.setStatus(ReturnRequestStatus.APPROVED);
        req.setProcessedBy(current);
        req.setProcessedAt(LocalDateTime.now());
        req.setResponseNote(dto.getResponseNote());

        ReturnRequest saved = returnRequestRepository.save(req);
        return toDto(saved);
    }

    @Transactional
    @SuppressWarnings("DataFlowIssue")
    public ReturnRequestResponseDTO reject(Long id, ReturnRequestProcessDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found"));

        if (req.getStatus() != ReturnRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu đã được xử lý");
        }

        Objects.requireNonNull(req.getOrder().getId(), "Order id is null");
        req.setStatus(ReturnRequestStatus.REJECTED);
        req.setProcessedBy(current);
        req.setProcessedAt(LocalDateTime.now());
        req.setResponseNote(dto.getResponseNote());

        ReturnRequest saved = returnRequestRepository.save(req);
        return toDto(saved);
    }

    private ReturnRequestResponseDTO toDto(ReturnRequest req) {
        return ReturnRequestResponseDTO.builder()
                .id(req.getId())
                .orderId(req.getOrder().getId())
                .orderTotal(req.getOrder().getTotalAmount())
                .reason(req.getReason())
                .status(req.getStatus())
                .createdByName(req.getCreatedBy() != null ? req.getCreatedBy().getUsername() : null)
                .processedByName(req.getProcessedBy() != null ? req.getProcessedBy().getUsername() : null)
                .createdAt(req.getCreatedAt())
                .processedAt(req.getProcessedAt())
                .responseNote(req.getResponseNote())
                .build();
    }
}

