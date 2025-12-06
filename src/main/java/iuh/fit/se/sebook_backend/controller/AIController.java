package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.dto.ChatRequest;
import iuh.fit.se.sebook_backend.dto.ChatResponse;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.service.ai.BookSearchService;
import iuh.fit.se.sebook_backend.service.ai.ChatbotService;
import iuh.fit.se.sebook_backend.service.ai.EmbeddingAsyncService;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AIController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    private final BookSearchService bookSearchService;
    private final ChatbotService chatbotService;
    private final SecurityUtil securityUtil;
    private final EmbeddingAsyncService embeddingAsyncService;

    public AIController(BookSearchService bookSearchService, 
                       ChatbotService chatbotService,
                       SecurityUtil securityUtil,
                       EmbeddingAsyncService embeddingAsyncService) {
        this.bookSearchService = bookSearchService;
        this.chatbotService = chatbotService;
        this.securityUtil = securityUtil;
        this.embeddingAsyncService = embeddingAsyncService;
    }

    /**
     * Tìm kiếm sách thông minh bằng semantic search
     * @param q Câu truy vấn tìm kiếm
     * @param limit Số lượng kết quả tối đa (mặc định 10, tối đa 50)
     * @return Danh sách sách được sắp xếp theo độ liên quan
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchBooks(
            @RequestParam @NotBlank(message = "Query không được để trống") String q,
            @RequestParam(required = false) @Min(value = 1, message = "Limit phải lớn hơn 0") Integer limit) {
        
        // Giới hạn limit tối đa để tránh quá tải
        int resultLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        
        List<BookDTO> results = bookSearchService.smartSearch(q, resultLimit);
        return ResponseEntity.ok(results);
    }

    /**
     * Chatbot hỗ trợ khách hàng
     * Sử dụng RAG (Retrieval-Augmented Generation) với dữ liệu sách và đơn hàng
     * 
     * @param request ChatRequest chứa message và conversationId (optional)
     * @param userDetails User hiện tại từ authentication (optional - nếu đã đăng nhập)
     * @return ChatResponse với câu trả lời từ AI và danh sách sách được đề xuất
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody @jakarta.validation.Valid ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("💬 Nhận yêu cầu chat: {}", request.getMessage());
        
        try {
            // Lấy accountId từ authentication nếu có
            Long accountId = null;
            if (userDetails != null) {
                try {
                    Account account = securityUtil.getLoggedInAccount();
                    accountId = account.getId();
                    log.info("🔐 Lấy accountId từ authentication: {}", accountId);
                } catch (Exception e) {
                    log.warn("⚠️ Không thể lấy accountId từ authentication: {}", e.getMessage());
                }
            }
            
            Map<String, Object> result = chatbotService.chat(
                request.getMessage(), 
                request.getConversationId(),
                accountId
            );
            
            ChatResponse response = new ChatResponse(
                (String) result.get("response"),
                (List<String>) result.get("suggestedBooks"),
                (List<String>) result.get("sources"),
                (String) result.get("conversationId")
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý chat: {}", e.getMessage(), e);
            ChatResponse errorResponse = new ChatResponse(
                "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.",
                List.of(),
                List.of(),
                request.getConversationId() != null ? request.getConversationId() : ""
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Tạo embedding cho tất cả sách chưa có embedding
     * Chạy trong background thread để không block request
     * @return Thông báo xác nhận
     */
    @PostMapping("/generate-embeddings")
    public ResponseEntity<Map<String, String>> generateEmbeddings() {
        log.info("📥 Nhận yêu cầu tạo embedding cho tất cả sách");
        
        // Chạy async để không block request
        embeddingAsyncService.generateEmbeddingsAsync();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "🚀 Đã bắt đầu sinh embedding cho các sách chưa có. Xem log để theo dõi tiến trình.");
        response.put("status", "processing");
        return ResponseEntity.accepted().body(response);
    }
}

