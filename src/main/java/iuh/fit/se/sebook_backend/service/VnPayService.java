package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.configs.VnPayConfig;
import iuh.fit.se.sebook_backend.dto.PaymentRequestDTO;
import iuh.fit.se.sebook_backend.dto.PaymentResponseDTO;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayService {

    @Value("${vnpay.tmncode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashsecret}")
    private String vnp_HashSecret;

    private final OrderRepository orderRepository;

    public VnPayService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        // Gán giá trị vào VnPayConfig để các phương thức static có thể dùng
        VnPayConfig.VNP_TMNCODE = this.vnp_TmnCode;
        VnPayConfig.VNP_HASHSECRET = this.vnp_HashSecret;
    }

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request, HttpServletRequest httpServletRequest) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Kiểm tra xem đơn hàng đã được thanh toán chưa hoặc đã bị hủy chưa
        if (!order.getStatus().equals(Order.PENDING)) {
            throw new IllegalStateException("Order is not in PENDING state, cannot create payment.");
        }

        // Tạo mã giao dịch (vnp_TxnRef) duy nhất
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);

        // Cập nhật mã này vào đơn hàng để đối soát khi VNPay gọi lại
        order.setPaymentCode(vnp_TxnRef);
        orderRepository.save(order);

        // Số tiền (VNPay yêu cầu nhân 100)
        long amount = (long) (order.getTotalAmount() * 100);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang SEBook #" + order.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.VNP_RETURNURL);
        vnp_Params.put("vnp_IpAddr", VnPayConfig.getIpAddress(httpServletRequest));

        // Cài đặt thời gian
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Tạo chữ ký (hash)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnPayConfig.VNP_PAYURL + "?" + queryUrl;

        return new PaymentResponseDTO(paymentUrl);
    }

    /**
     * Xử lý dữ liệu VNPay trả về (callback)
     * @param request HttpServletRequest chứa các tham số từ VNPay
     * @return true nếu thanh toán thành công và hợp lệ, false nếu thất bại
     */
    @Transactional
    public boolean processPaymentReturn(HttpServletRequest request) {
        try {
            Map<String, String> params = VnPayConfig.getParamsFromRequest(request);
            String vnp_SecureHash = params.remove("vnp_SecureHash");

            // Xóa hash type nếu tồn tại (để kiểm tra chữ ký)
            params.remove("vnp_SecureHashType");

            // Sắp xếp và tạo chuỗi hash data
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            // Kiểm tra chữ ký
            String mySecureHash = VnPayConfig.hmacSHA512(vnp_HashSecret, hashData.toString());
            if (!mySecureHash.equals(vnp_SecureHash)) {
                return false; // Chữ ký không hợp lệ
            }

            // Chữ ký hợp lệ, kiểm tra trạng thái thanh toán
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef"); // Đây là paymentCode của Order

            if ("00".equals(vnp_ResponseCode)) {
                // Thanh toán thành công
                Order order = orderRepository.findByPaymentCode(vnp_TxnRef)
                        .orElse(null);

                if (order != null && order.getStatus().equals(Order.PENDING)) {
                    // Cập nhật trạng thái đơn hàng
                    order.setStatus(Order.PROCESSING); // Hoặc "PAID" tùy logic
                    orderRepository.save(order);

                    // TODO: Gửi email xác nhận thanh toán thành công (nếu cần)

                    return true;
                }
            }
            return false; // Thanh toán thất bại theo VNPay

        } catch (Exception e) {
            throw new RuntimeException("Error processing VNPay return: " + e.getMessage());
        }
    }
}