package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.PaymentRequestDTO;
import iuh.fit.se.sebook_backend.dto.PaymentResponseDTO;
import iuh.fit.se.sebook_backend.dto.PaymentReturnDTO;
import iuh.fit.se.sebook_backend.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final VnPayService vnPayService;

    public PaymentController(VnPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    /**
     * API để tạo URL thanh toán cho một đơn hàng
     */
    @PostMapping("/create-payment")
    public ResponseEntity<PaymentResponseDTO> createPayment(@RequestBody PaymentRequestDTO request,
                                                            HttpServletRequest httpServletRequest) {
        PaymentResponseDTO response = vnPayService.createPayment(request, httpServletRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * API callback do VNPay gọi sau khi thanh toán
     * Endpoint này phải được truy cập công khai
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        PaymentReturnDTO result = vnPayService.processPaymentReturn(request);
        String redirectUrl = vnPayService.buildClientRedirectUrl(result);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}