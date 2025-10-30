package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.PaymentRequestDTO;
import iuh.fit.se.sebook_backend.dto.PaymentResponseDTO;
import iuh.fit.se.sebook_backend.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> vnpayReturn(HttpServletRequest request) {
        boolean success = vnPayService.processPaymentReturn(request);
        if (success) {
            // TODO: Chuyển hướng về trang thành công của Frontend
            // return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://frontend.com/payment-success")).build();
            return ResponseEntity.ok("Thanh toán thành công!");
        } else {
            // TODO: Chuyển hướng về trang thất bại của Frontend
            // return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://frontend.com/payment-failure")).build();
            return ResponseEntity.badRequest().body("Thanh toán thất bại hoặc chữ ký không hợp lệ.");
        }
    }
}