package iuh.fit.se.sebook_backend.service.ai;

import iuh.fit.se.sebook_backend.dto.AddressDTO;
import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.dto.OrderDTO;
import iuh.fit.se.sebook_backend.dto.OrderDetailDTO;
import iuh.fit.se.sebook_backend.entity.Address;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.entity.OrderDetail;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import iuh.fit.se.sebook_backend.service.OrderService;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${cohere.api.key}")
    private String cohereApiKey;

    private static final String CHAT_API_URL = "https://api.cohere.ai/v1/chat";

    private final RestTemplate restTemplate = new RestTemplate();
    private final BookRepository bookRepository;
    @SuppressWarnings("unused")
    private final CohereEmbeddingService embeddingService; // reserved
    @SuppressWarnings("unused")
    private final OrderService orderService; // reserved
    private final BookSearchService bookSearchService;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;

    // System prompt cho chatbot
    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI của cửa hàng sách SEBook.

        🎯 PHẠM VI BẮT BUỘC (KHÔNG ĐƯỢC VƯỢT RA NGOÀI):

        Bạn CHỈ được phép trả lời 3 nhóm nội dung sau:

        1) TƯ VẤN / TRA CỨU SÁCH từ dữ liệu thật trong database.

        2) THÔNG TIN ĐƠN HÀNG & TÀI KHOẢN của CHÍNH NGƯỜI DÙNG ĐANG ĐĂNG NHẬP

           (chỉ những gì đã được đưa vào context "📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG").

        3) THÔNG TIN CHÍNH SÁCH & DỊCH VỤ SEBOOK:

           - Chính sách giao hàng

           - Chính sách đổi trả

           - Phương thức thanh toán (COD, VNPay)

           - Chương trình khách hàng thân thiết

           - Kênh hỗ trợ khách hàng (hotline, email, thời gian hỗ trợ)

        ❌ MỌI CÂU HỎI NGOÀI 3 NHÓM NỘI DUNG TRÊN (ví dụ: lập trình, thời tiết, tin tức, giải bài tập, v.v.)

        → Bạn PHẢI TỪ CHỐI lịch sự:

          "Xin lỗi, tôi chỉ hỗ trợ các vấn đề liên quan đến sách, đơn hàng và chính sách dịch vụ của SEBook."

        📂 NGUỒN DỮ LIỆU ĐƯỢC PHÉP SỬ DỤNG:

        Bạn CHỈ ĐƯỢC sử dụng thông tin xuất hiện trong các phần context sau (do hệ thống cung cấp):

        - "📚 THÔNG TIN SÁCH TRONG CỬA HÀNG SEBOOK"

        - "📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG (DỮ LIỆU THỰC TỪ DATABASE)"

        - "📋 THÔNG TIN CHÍNH SÁCH VÀ DỊCH VỤ SEBOOK"

        - "📊 THỐNG KÊ ĐƠN HÀNG (LẤY TỪ DATABASE)"

        

        Bạn KHÔNG ĐƯỢC:

        - Dùng kiến thức chung trên internet hoặc kiến thức nền để bổ sung, sửa, hoặc đoán thông tin.

        - Tự bịa thêm sách, tác giả, giá, mô tả, chương trình khuyến mãi, chính sách, hotline, email, địa chỉ, v.v.

        - Tự bịa thêm đơn hàng, ID đơn hàng, ngày đặt, trạng thái, số tiền, địa chỉ giao hàng, thông tin người nhận, v.v.

        - Tạo ra hoặc đề xuất bất kỳ thông tin nào không có trong context được cung cấp.

        - Sử dụng thông tin từ conversation history để tạo ra dữ liệu giả (fake data).

        ⚠️ QUY TẮC NGHIÊM NGẶT: Nếu một thông tin KHÔNG có trong các context trên → bạn PHẢI nói:

          "Trong hệ thống SEBook hiện tại không có sẵn thông tin này, nên tôi không thể trả lời chính xác."

        - TUYỆT ĐỐI KHÔNG được tự tạo, bịa đặt, hoặc suy đoán thông tin dựa trên kiến thức chung.

        ===============================

        📚 1. TƯ VẤN VÀ TRA CỨU SÁCH

        ===============================

        - Khi trả lời về sách, bạn CHỈ được dùng dữ liệu từ phần:

          "📚 THÔNG TIN SÁCH TRONG CỬA HÀNG SEBOOK".

        - Tất cả sách trong phần này được tìm kiếm từ database SEBook sử dụng embedding từ table book_embedding.

        - Các trường bạn có thể sử dụng: tên sách, tác giả, giá, thể loại, tồn kho, tình trạng.

        - KHÔNG tự bịa thêm nội dung cốt truyện, review, đánh giá… nếu context không cung cấp.

        - KHÔNG được gợi ý hoặc đề cập đến sách nào không có trong phần context này.

        QUY TẮC:

        - Nếu "Tồn kho" > 0 → trả lời rõ "Còn hàng" / "Có sẵn", có thể kèm số lượng nếu có trong context.

        - Nếu "Tồn kho" = 0 → trả lời "Hết hàng" / "Hiện không còn sẵn".

        ⚠️ CẤM TUYỆT ĐỐI GỢI Ý SÁCH NGOÀI DATABASE:

        - KHÔNG ĐƯỢC đề xuất thêm sách nào mà context không liệt kê.

        - KHÔNG ĐƯỢC ghi: "Ngoài ra bạn có thể tham khảo ..." với những sách không nằm trong danh sách DB.

        - KHÔNG ĐƯỢC tự bịa ra tên sách, tác giả, giá cả, mô tả, hoặc bất kỳ thông tin sách nào.

        - KHÔNG ĐƯỢC sử dụng kiến thức chung về sách để gợi ý sách không có trong database.

        - Tất cả sách được tìm kiếm từ database SEBook sử dụng embedding từ table book_embedding.

        - Nếu không có sách phù hợp trong context, hãy nói:

          "Hiện tại trong kho SEBook không có cuốn sách phù hợp với yêu cầu của bạn. Bạn có thể thử tìm kiếm với từ khóa khác hoặc liên hệ bộ phận hỗ trợ."

        Nếu người dùng hỏi gợi ý sách theo nhu cầu (ví dụ: "sách self-help", "sách thiếu nhi"):

        - CHỈ chọn trong những sách đã được liệt kê trong context và phù hợp thể loại.

        - Nếu không có sách phù hợp trong context, nói rõ là không có dữ liệu phù hợp trong kho SEBook.

        - TUYỆT ĐỐI KHÔNG được gợi ý sách từ kiến thức chung hoặc sách nổi tiếng nếu chúng không có trong context.

        ============================================

        📦 2. ĐƠN HÀNG & TÀI KHOẢN ĐANG ĐĂNG NHẬP

        ============================================

        - Mọi thông tin về đơn hàng phải lấy từ phần:

          "📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG (DỮ LIỆU THỰC TỪ DATABASE)".

        - Phần này CHỈ chứa đơn hàng của CHÍNH tài khoản đang đăng nhập.

        BẮT BUỘC:

        - CHỈ ĐƯỢC trả lời về các đơn hàng có trong context đó.

        - KHÔNG ĐƯỢC suy đoán hay bịa thêm đơn hàng mới.

        - KHÔNG ĐƯỢC dùng lịch sử hội thoại, số điện thoại, email, tên người… để tự tưởng tượng ra đơn hàng.

        BẢO MẬT:

        - Nếu người dùng hỏi về đơn hàng hoặc thông tin cá nhân của NGƯỜI KHÁC (ví dụ:

          "Đơn hàng của bạn/em/vợ/bạn tôi", hoặc cung cấp số điện thoại/email khác):

          → Bạn PHẢI trả lời:

            "Xin lỗi, vì lý do bảo mật tôi chỉ có thể cung cấp thông tin đơn hàng của chính tài khoản đang đăng nhập trên hệ thống SEBook."

        - Cho dù user gửi email/số điện thoại trong tin nhắn, bạn KHÔNG ĐƯỢC giả sử hay tạo đơn hàng cho email/số đó

          nếu context không cung cấp sẵn dữ liệu tương ứng.

        TRẠNG THÁI ĐƠN HÀNG:

        - PENDING     → "Chờ xác nhận"

        - PROCESSING  → "Đang xử lý"

        - DELIVERING  → "Đang giao hàng"

        - COMPLETED   → "Đã hoàn thành"

        - CANCELLED   → "Đã hủy"

        - RETURNED    → "Đã trả lại"

        NẾU KHÔNG CÓ ĐƠN HÀNG:

        - Nếu context nói rõ tổng số đơn hàng = 0, hoặc không có phần "📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG":

          → Bạn PHẢI trả lời:

            "Hiện tại trong hệ thống SEBook không có đơn hàng nào của tài khoản này."

        - KHÔNG ĐƯỢC bịa đơn hàng để trả lời.

        ================================

        📋 3. CHÍNH SÁCH VÀ DỊCH VỤ SEBOOK

        ================================

        - Khi khách hàng hỏi về giao hàng, đổi trả, thanh toán, khách hàng thân thiết, hỗ trợ khách hàng,

        bạn PHẢI dùng đúng nội dung trong phần:

          "📋 THÔNG TIN CHÍNH SÁCH VÀ DỊCH VỤ SEBOOK".

        CỤ THỂ:

        1. CHÍNH SÁCH GIAO HÀNG:

           - Miễn phí giao hàng cho đơn hàng trên 500.000 VNĐ

           - Phí giao hàng: 30.000 VNĐ cho đơn hàng dưới 500.000 VNĐ

           - Thời gian giao hàng: 3-5 ngày làm việc (từ thứ 2 đến thứ 6)

           - Giao hàng toàn quốc

           - Hỗ trợ giao hàng nhanh (1-2 ngày) với phí bổ sung

        2. CHÍNH SÁCH ĐỔI TRẢ:

           - Đổi/trả hàng trong vòng 7 ngày kể từ ngày nhận hàng

           - Sách phải còn nguyên vẹn, chưa sử dụng, còn tem nhãn

           - Không áp dụng cho sách đã đọc hoặc có dấu hiệu sử dụng

           - Khách hàng chịu phí vận chuyển khi đổi/trả (trừ trường hợp lỗi từ phía cửa hàng)

           - Liên hệ bộ phận hỗ trợ để được hướng dẫn chi tiết

        3. PHƯƠNG THỨC THANH TOÁN:

           - COD (Cash on Delivery - Thanh toán khi nhận hàng)

           - VNPay: Thanh toán online qua cổng thanh toán VNPay

           - Lưu ý: Chỉ có 2 phương thức trên, KHÔNG có phương thức khác.

        4. CHƯƠNG TRÌNH KHÁCH HÀNG THÂN THIẾT:

           - Tích điểm cho mỗi đơn hàng: 1 điểm = 1.000 VNĐ

           - Đổi điểm lấy voucher giảm giá

           - Khách hàng VIP: Giảm giá 5-10% cho đơn hàng

           - Ưu tiên hỗ trợ và chăm sóc đặc biệt

        5. HỖ TRỢ KHÁCH HÀNG:

           - Hotline: 1900-xxxx (miễn phí)

           - Email: support@sebook.com

           - Thời gian hỗ trợ: 24/7 qua chatbot, 8:00-22:00 qua hotline

           - Hỗ trợ kỹ thuật: Hướng dẫn đặt hàng, thanh toán, sử dụng website

        ⚠️ KHÔNG ĐƯỢC:

        - Tự bịa thêm chính sách, gói thành viên, mã giảm giá, phương thức thanh toán khác, hoặc giờ làm việc khác.

        ======================

        📊 4. THỐNG KÊ ĐƠN HÀNG

        ======================

        - Nếu context "📊 THỐNG KÊ ĐƠN HÀNG (LẤY TỪ DATABASE)" có cung cấp thông tin

          (đơn có tổng tiền cao nhất/thấp nhất, số lượng mua cao nhất/thấp nhất),

          bạn CHỈ được đọc lại đúng các số liệu đó.

        - KHÔNG được suy ra thêm bất kỳ thống kê nào khác ngoài những gì có trong context.

        ==========================

        💬 5. LỊCH SỬ HỘI THOẠI

        ==========================

        - Bạn có thể dùng lịch sử chat để hiểu khách hàng đang hỏi tiếp cái gì.

        - Tuyệt đối KHÔNG dùng lịch sử hội thoại để:

          • Tạo thêm đơn hàng giả.

          • Tự bịa sách mới không nằm trong context.

          • Suy ra thông tin cá nhân không có trong database.

        =====================

        ✅ 6. CÁCH TRẢ LỜI

        =====================

        - Luôn trả lời bằng TIẾNG VIỆT, giọng thân thiện, rõ ràng, dễ hiểu.

        - Nếu thông tin không có trong context hoặc bạn không chắc chắn:

          → Hãy nói thẳng là hệ thống không có dữ liệu, và gợi ý khách hàng liên hệ bộ phận hỗ trợ.
        """;

    public ChatbotService(BookRepository bookRepository, 
                         CohereEmbeddingService embeddingService,
                         OrderService orderService,
                         BookSearchService bookSearchService,
                         OrderRepository orderRepository,
                         SecurityUtil securityUtil) {
        this.bookRepository = bookRepository;
        this.embeddingService = embeddingService;
        this.orderService = orderService;
        this.bookSearchService = bookSearchService;
        this.orderRepository = orderRepository;
        this.securityUtil = securityUtil;
    }

    // Lưu trữ conversation history (in-memory, có thể cải thiện bằng database sau)
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();
    
    /**
     * Xử lý tin nhắn từ khách hàng và trả lời
     * @param userMessage Tin nhắn từ khách hàng
     * @param conversationId ID cuộc hội thoại (optional)
     * @param accountId ID của account (optional - nếu đã đăng nhập)
     */
    public Map<String, Object> chat(String userMessage, String conversationId, Long accountId) {
        log.info("💬 Nhận tin nhắn từ user: {} (accountId: {}, conversationId: {})", userMessage, accountId, conversationId);
        
        // Tạo conversationId nếu chưa có
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        try {
            // Xác định account đang đăng nhập (ưu tiên tham số accountId, fallback từ SecurityContext)
            Long targetAccountId = accountId;
            if (targetAccountId == null) {
                try {
                    targetAccountId = securityUtil.getLoggedInAccount().getId();
                } catch (Exception ex) {
                    log.warn("⚠️ Không lấy được account từ SecurityContext: {}", ex.getMessage());
                }
            }

            // 1. Tìm kiếm sách liên quan (RAG)
            List<Book> relevantBooks = findRelevantBooks(userMessage);
            log.info("📚 Tìm thấy {} sách liên quan", relevantBooks.size());

            // 2. Tạo context từ thông tin sách
            String bookContext = buildContextFromBooks(relevantBooks);

            // 3. Lấy thông tin đơn hàng: chỉ cho người đang đăng nhập
            String orderContext = "";
            if (targetAccountId != null) {
                int orderCount = getOrderCountByAccountId(targetAccountId);
                orderContext = buildOrderContext(targetAccountId, userMessage);
                log.info("📦 Đã lấy thông tin đơn hàng cho account {}: {} (số lượng: {})",
                        targetAccountId,
                        orderCount > 0 ? "Có đơn hàng" : "Không có đơn hàng",
                        orderCount);
            } else {
                orderContext = """
                        ⚠️ GIỚI HẠN BẢO MẬT ĐƠN HÀNG:
                        - Bạn chưa đăng nhập, nên tôi KHÔNG thể cung cấp thông tin đơn hàng.
                        - Tôi chỉ cung cấp thông tin đơn hàng của chính bạn khi bạn đã đăng nhập.
                        """;
            }

            // 4. Thêm thông tin về chính sách và FAQ
        String policyContext = buildPolicyContext(userMessage);

        // 4.1 Thêm thông tin thống kê đơn hàng (max/min)
        String orderStatsContext = buildOrderStatsContext();
            
            // 5. Kết hợp context
        String context = bookContext;
            if (!orderContext.isEmpty()) {
                context += "\n\n" + orderContext;
            }
            if (!policyContext.isEmpty()) {
                context += "\n\n" + policyContext;
            }
        if (!orderStatsContext.isEmpty()) {
            context += "\n\n" + orderStatsContext;
        }

            // 6. Lấy conversation history
            List<Map<String, String>> chatHistory = conversationHistory.getOrDefault(conversationId, new ArrayList<>());
            
            // 7. Gọi Cohere Chat API với conversation history
            String aiResponse = callCohereChatAPI(userMessage, context, chatHistory);
            
            // 8. Lưu conversation history (sử dụng role đúng format của Cohere: "User", "Chatbot")
            chatHistory.add(Map.of("role", "User", "message", userMessage));
            chatHistory.add(Map.of("role", "Chatbot", "message", aiResponse));
            // Giới hạn lịch sử tối đa 20 tin nhắn (10 cặp user-assistant)
            if (chatHistory.size() > 20) {
                chatHistory = chatHistory.subList(chatHistory.size() - 20, chatHistory.size());
            }
            conversationHistory.put(conversationId, chatHistory);

            // 9. Trích xuất tên sách được đề xuất từ response của AI
            List<String> suggestedBooks = extractBookNames(aiResponse, relevantBooks);
            
            // 10. Tạo sources (danh sách sách được tham khảo - những sách AI thực sự đề xuất)
            // Ưu tiên sử dụng suggestedBooks vì đó là những sách AI thực sự đã đề cập trong response
            List<String> sources;
            if (!suggestedBooks.isEmpty()) {
                // Nếu AI đã đề xuất sách, dùng chúng làm sources
                sources = new ArrayList<>(suggestedBooks);
            } else {
                // Nếu không tìm thấy sách được đề xuất, lấy 3 sách đầu tiên từ danh sách tìm kiếm
                sources = relevantBooks.stream()
                        .limit(3)
                        .map(Book::getTitle)
                        .collect(Collectors.toList());
                // Đồng thời dùng sources làm suggestedBooks
                suggestedBooks = new ArrayList<>(sources);
            }

            // 11. Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("response", aiResponse);
            response.put("suggestedBooks", suggestedBooks);
            response.put("sources", sources);
            response.put("conversationId", conversationId != null ? conversationId : UUID.randomUUID().toString());

            log.info("✅ Đã trả lời tin nhắn thành công");
            return response;

        } catch (Exception e) {
            log.error("❌ Lỗi khi xử lý chat: {}", e.getMessage(), e);
            
            // Fallback response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("response", "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ bộ phận hỗ trợ.");
            errorResponse.put("suggestedBooks", List.of());
            errorResponse.put("sources", List.of());
            errorResponse.put("conversationId", conversationId != null ? conversationId : UUID.randomUUID().toString());
            return errorResponse;
        }
    }


    /**
     * Tìm kiếm sách liên quan dựa trên tin nhắn của user (RAG)
     * CHỈ sử dụng sách từ database SEBook, sử dụng embedding từ table book_embedding
     * KHÔNG trả về sách bên ngoài database
     */
    private List<Book> findRelevantBooks(String userMessage) {
        try {
            // ✅ Ưu tiên 1: Sử dụng semantic search với embedding từ table book_embedding
            // Method smartSearch() sử dụng embedding đã được tạo sẵn trong database
            List<BookDTO> semanticResults = bookSearchService.smartSearch(userMessage, 10);
            
            if (!semanticResults.isEmpty()) {
                // Chuyển BookDTO về Book entity
                // CHỈ lấy sách có isActive = true
                List<Book> books = semanticResults.stream()
                        .map(bookDTO -> {
                            // Tìm Book từ ID
                            return bookRepository.findById(bookDTO.getId()).orElse(null);
                        })
                        .filter(book -> book != null)
                        .filter(book -> {
                            // CHỈ lấy sách có isActive = true
                            boolean active = book.getIsActive() == null || Boolean.TRUE.equals(book.getIsActive());
                            return active;
                        })
                        .limit(10) // Lấy tối đa 10 sách từ semantic search
                        .collect(Collectors.toList());
                
                if (!books.isEmpty()) {
                    log.info("✅ Tìm thấy {} sách bằng semantic search", books.size());
                    return books;
                }
            }
            
            // ✅ Ưu tiên 2: Fallback về keyword matching từ database nếu semantic search không có kết quả
            // CHỈ lấy sách có isActive = true
            // Tất cả sách đều từ database, không có sách bên ngoài
            log.info("⚠️ Semantic search không có kết quả, chuyển sang keyword matching từ database");
            List<Book> allBooks = bookRepository.findByIsActiveTrue();
            
            if (allBooks.isEmpty()) {
                return List.of();
            }

            // Tìm kiếm theo từ khóa trong title, author
            String lowerMessage = userMessage.toLowerCase();
            String[] keywords = lowerMessage.split("\\s+"); // Tách thành các từ khóa
            
            List<Book> relevantBooks = allBooks.stream()
                    .filter(book -> {
                        String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
                        String author = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";
                        
                        // Kiểm tra từng từ khóa
                        for (String keyword : keywords) {
                            if (keyword.length() > 2 && // Bỏ qua từ quá ngắn
                                (title.contains(keyword) || 
                                 author.contains(keyword))) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .limit(10) // Tăng lên 10 sách
                    .collect(Collectors.toList());

            // ✅ Ưu tiên 3: Nếu vẫn không tìm thấy, trả về sách phổ biến từ database (có nhiều quantity)
            // Tất cả đều từ database, không có sách bên ngoài
            if (relevantBooks.isEmpty()) {
                log.info("⚠️ Keyword matching không có kết quả, trả về sách phổ biến từ database");
                relevantBooks = allBooks.stream()
                        .sorted((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()))
                        .limit(5)
                        .collect(Collectors.toList());
            }

            return relevantBooks;

        } catch (Exception e) {
            log.error("❌ Lỗi khi tìm kiếm sách: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Xây dựng context từ danh sách sách để đưa vào prompt
     */
    private String buildContextFromBooks(List<Book> books) {
        if (books.isEmpty()) {
            return """
                📚 THÔNG TIN SÁCH TRONG CỬA HÀNG SEBOOK:
                Hiện tại cửa hàng chưa có sách nào phù hợp với yêu cầu của khách hàng.
                
                ⚠️ QUY ĐỊNH BẮT BUỘC:
                - Bạn PHẢI trả lời: "Hiện tại trong kho SEBook không có cuốn sách phù hợp với yêu cầu của bạn."
                - KHÔNG ĐƯỢC gợi ý sách từ kiến thức chung hoặc sách bên ngoài database
                - KHÔNG ĐƯỢC tự bịa ra tên sách, tác giả, giá cả, hoặc thông tin sách nào
                - CHỈ được sử dụng sách có trong database của hệ thống SEBook
                - Nếu không có sách phù hợp, hãy đề nghị khách hàng thử tìm kiếm với từ khóa khác hoặc liên hệ bộ phận hỗ trợ
                """;
        }

        StringBuilder context = new StringBuilder();
        context.append("""
            📚 THÔNG TIN SÁCH TRONG CỬA HÀNG SEBOOK:
            Đây là danh sách các sách có sẵn trong cửa hàng phù hợp với yêu cầu của khách hàng.
            Tất cả sách này được tìm kiếm từ database của hệ thống SEBook sử dụng embedding từ table book_embedding.
            
            ⚠️ QUY ĐỊNH BẮT BUỘC KHI GỢI Ý VÀ TRẢ LỜI:
            1. CHỈ ĐƯỢC gợi ý các sách từ danh sách dưới đây (sách có sẵn trong database)
            2. KHÔNG ĐƯỢC gợi ý sách nào ngoài danh sách này, dù là sách nổi tiếng hay phổ biến
            3. KHÔNG ĐƯỢC tự bịa ra tên sách, tác giả, giá cả, mô tả, hoặc thông tin sách nào
            4. TÌNH TRẠNG CÓ SẴN:
               - Nếu "Tồn kho" > 0: Trả lời "Có sẵn" hoặc "Còn hàng"
               - Nếu "Tồn kho" = 0: Trả lời "Hết hàng" hoặc "Hiện không còn sẵn"
            5. GỢI Ý SÁCH TƯƠNG TỰ: CHỈ gợi ý sách tương tự từ danh sách dưới đây, dựa trên thể loại, tác giả
            6. Nếu khách hàng hỏi về sách không có trong danh sách: 
               → Trả lời: "Hiện tại trong kho SEBook không có cuốn sách này. Bạn có thể thử tìm kiếm với từ khóa khác hoặc liên hệ bộ phận hỗ trợ."
            
            Danh sách sách có sẵn trong cửa hàng (từ database):
            
            """);
        
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            String categories = book.getCategories() != null 
                ? book.getCategories().stream()
                    .map(cat -> cat.getName())
                    .collect(Collectors.joining(", "))
                : "";
            
            String availability = book.getQuantity() > 0 
                ? String.format("CÓ SẴN (%d cuốn)", book.getQuantity())
                : "HẾT HÀNG";
            
            context.append(String.format(
                "%d. Tên sách: %s\n" +
                "   Tác giả: %s\n" +
                "   Giá: %.0f VNĐ\n" +
                "   Thể loại: %s\n" +
                "   Tình trạng: %s\n" +
                "   Tồn kho: %d cuốn\n\n",
                i + 1,
                book.getTitle(),
                book.getAuthor(),
                book.getPrice(),
                categories.isEmpty() ? "Không có" : categories,
                availability,
                book.getQuantity()
            ));
        }
        
        context.append("""
            
            ⚠️ LƯU Ý CUỐI CÙNG: 
            - CHỈ được gợi ý sách từ danh sách trên (sách có sẵn trong database SEBook)
            - Luôn kiểm tra "Tình trạng" để trả lời chính xác về việc có sẵn hay không
            - TUYỆT ĐỐI KHÔNG được gợi ý sách bên ngoài database, dù là sách nổi tiếng
            - TUYỆT ĐỐI KHÔNG được tự bịa ra thông tin sách nào
            - Nếu không có sách phù hợp trong danh sách, hãy nói rõ là không có trong kho SEBook
            """);

        return context.toString();
    }

    /**
     * Gọi Cohere Chat API với conversation history
     */
    private String callCohereChatAPI(String userMessage, String context, List<Map<String, String>> chatHistory) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + cohereApiKey);
            headers.set("Cohere-Version", "2022-12-06");
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Kết hợp system prompt và context vào preamble
            String fullPreamble = SYSTEM_PROMPT;
            if (!context.isEmpty()) {
                fullPreamble += "\n\n" + context;
            }

            JSONObject body = new JSONObject();
            // Sử dụng command-r-08-2024 (model mới thay thế command-r đã bị xóa vào 15/09/2025)
            body.put("model", "command-r-08-2024");
            body.put("message", userMessage);
            body.put("preamble", fullPreamble);
            body.put("temperature", 0.2);
            body.put("max_tokens", 1000);
            body.put("stream", false);
            
            // Thêm chat_history nếu có (Cohere yêu cầu role: "User", "Chatbot", "System", "Tool")
            if (chatHistory != null && !chatHistory.isEmpty()) {
                JSONArray chatHistoryArray = new JSONArray();
                for (Map<String, String> msg : chatHistory) {
                    JSONObject chatMsg = new JSONObject();
                    // Đảm bảo role đúng format: "User" hoặc "Chatbot"
                    String role = msg.get("role");
                    if ("user".equalsIgnoreCase(role)) {
                        role = "User";
                    } else if ("assistant".equalsIgnoreCase(role)) {
                        role = "Chatbot";
                    }
                    chatMsg.put("role", role);
                    chatMsg.put("message", msg.get("message"));
                    chatHistoryArray.put(chatMsg);
                }
                body.put("chat_history", chatHistoryArray);
                log.info("📝 Gửi {} tin nhắn trong conversation history", chatHistory.size());
            }

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    CHAT_API_URL, HttpMethod.POST, request, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject json = new JSONObject(response.getBody());
                
                // Cohere Chat API trả về text trong field "text"
                if (json.has("text")) {
                    String text = json.getString("text");
                    log.info("✅ Nhận response từ Cohere: {}", 
                        text.length() > 100 ? text.substring(0, 100) + "..." : text);
                    return text;
                } else {
                    log.warn("⚠️ Response không có field 'text': {}", json.toString());
                    return "Xin lỗi, tôi không thể tạo câu trả lời. Vui lòng thử lại sau.";
                }
            } else {
                log.error("⚠️ Lỗi API: {} - {}", response.getStatusCode(), response.getBody());
                
                // Thử fallback nếu model không hợp lệ (404) hoặc bad request (400)
                if (response.getStatusCode() == HttpStatus.NOT_FOUND || 
                    response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    log.info("🔄 Thử fallback với model khác...");
                    return tryFallbackChat(userMessage, context, chatHistory);
                }
                
                return "Xin lỗi, tôi không thể xử lý câu hỏi này ngay bây giờ. Vui lòng thử lại sau.";
            }

        } catch (HttpClientErrorException e) {
            // Xử lý lỗi HTTP từ Cohere API
            log.error("❌ Lỗi HTTP từ Cohere API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // Nếu là lỗi 404 (model không tồn tại) hoặc 400 (bad request), thử fallback
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || 
                e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.info("🔄 Thử fallback với model khác...");
                return tryFallbackChat(userMessage, context, chatHistory);
            }
            
            return "Xin lỗi, có lỗi xảy ra khi xử lý câu hỏi của bạn. Vui lòng thử lại sau.";
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Cohere Chat API: {}", e.getMessage(), e);
            return "Xin lỗi, có lỗi xảy ra khi xử lý câu hỏi của bạn. Vui lòng thử lại sau.";
        }
    }

    /**
     * Trích xuất tên sách được đề xuất từ response
     * CHỈ lấy sách từ relevantBooks (sách trong database), không lấy sách từ bên ngoài
     */
    private List<String> extractBookNames(String response, List<Book> relevantBooks) {
        List<String> suggested = new ArrayList<>();
        String responseLower = response.toLowerCase();
        
        // Tạo map để lưu độ khớp (score) của mỗi sách
        Map<String, Integer> bookScores = new HashMap<>();
        
        // Kiểm tra xem response có đề cập đến sách nào trong database không
        for (Book book : relevantBooks) {
            String title = book.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                String titleLower = title.toLowerCase().trim();
                int score = 0;
                
                // 1. Exact match (quan trọng nhất) - điểm cao nhất
                if (responseLower.contains("\"" + titleLower + "\"") || 
                    responseLower.contains("'" + titleLower + "'") ||
                    responseLower.contains(titleLower)) {
                    // Kiểm tra exact match với dấu ngoặc kép hoặc không
                    if (responseLower.contains("\"" + titleLower + "\"") || 
                        responseLower.contains("'" + titleLower + "'")) {
                        score = 100; // Exact match với dấu ngoặc kép
                    } else if (responseLower.contains(titleLower)) {
                        score = 80; // Exact match không có dấu ngoặc kép
                    }
                }
                
                // 2. Partial match - kiểm tra từng từ quan trọng trong title
                String[] titleWords = titleLower.split("\\s+");
                int matchedWords = 0;
                for (String word : titleWords) {
                    if (word.length() > 3 && responseLower.contains(word)) {
                        matchedWords++;
                    }
                }
                // Nếu tất cả từ quan trọng đều xuất hiện, đó là match tốt
                if (matchedWords == titleWords.length && titleWords.length > 0) {
                    score = Math.max(score, 60);
                } else if (matchedWords > 0) {
                    score = Math.max(score, 30 + matchedWords * 10);
                }
                
                // Chỉ thêm nếu có điểm khớp
                if (score > 0) {
                    bookScores.put(title, score);
                }
            }
        }
        
        // Sắp xếp theo điểm khớp (cao nhất trước) và lấy top kết quả
        List<String> result = bookScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .distinct()
                .limit(10) // Lấy tối đa 10 sách
                .collect(Collectors.toList());
        
        log.info("📚 Trích xuất được {} sách từ response: {}", result.size(), result);
        
        return result;
    }

    /**
     * Fallback chat với model khác nếu model chính không khả dụng
     */
    private String tryFallbackChat(String userMessage, String context, List<Map<String, String>> chatHistory) {
        // Danh sách các model fallback theo thứ tự ưu tiên
        String[] fallbackModels = {
            "command-a-03-2025",      // Model mới nhất và mạnh nhất
            "command-r-plus-08-2024"  // Model thay thế command-r-plus
        };
        
        for (String model : fallbackModels) {
            try {
                log.info("🔄 Thử fallback với model: {}", model);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + cohereApiKey);
                headers.set("Cohere-Version", "2022-12-06");
                headers.setContentType(MediaType.APPLICATION_JSON);

                JSONObject body = new JSONObject();
                body.put("model", model);
                body.put("message", userMessage);
                body.put("preamble", SYSTEM_PROMPT + "\n\n" + context);
                body.put("temperature", 0.2);
                body.put("max_tokens", 800);
                
                // Thêm chat_history nếu có (Cohere yêu cầu role: "User", "Chatbot", "System", "Tool")
                if (chatHistory != null && !chatHistory.isEmpty()) {
                    JSONArray chatHistoryArray = new JSONArray();
                    for (Map<String, String> msg : chatHistory) {
                        JSONObject chatMsg = new JSONObject();
                        // Đảm bảo role đúng format: "User" hoặc "Chatbot"
                        String role = msg.get("role");
                        if ("user".equalsIgnoreCase(role)) {
                            role = "User";
                        } else if ("assistant".equalsIgnoreCase(role)) {
                            role = "Chatbot";
                        }
                        chatMsg.put("role", role);
                        chatMsg.put("message", msg.get("message"));
                        chatHistoryArray.put(chatMsg);
                    }
                    body.put("chat_history", chatHistoryArray);
                }

                HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
                ResponseEntity<String> response = restTemplate.exchange(
                        CHAT_API_URL, HttpMethod.POST, request, String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    JSONObject json = new JSONObject(response.getBody());
                    if (json.has("text")) {
                        log.info("✅ Fallback thành công với model: {}", model);
                        return json.getString("text");
                    }
                }
            } catch (HttpClientErrorException e) {
                log.warn("⚠️ Model {} không khả dụng: {}", model, e.getStatusCode());
                // Tiếp tục thử model tiếp theo
                continue;
            } catch (Exception e) {
                log.error("❌ Lỗi khi thử fallback với model {}: {}", model, e.getMessage());
                // Tiếp tục thử model tiếp theo
                continue;
            }
        }
        
        log.error("❌ Tất cả các model fallback đều không khả dụng");
        return "Xin lỗi, hệ thống đang gặp sự cố. Vui lòng liên hệ bộ phận hỗ trợ.";
    }

    /**
     * Xây dựng context từ thông tin đơn hàng của user
     */
    @Transactional(readOnly = true)
    private String buildOrderContext(Long accountId, String userMessage) {
        try {
            String lowerMessage = userMessage.toLowerCase();
            // Mở rộng từ khóa để phát hiện tốt hơn các câu hỏi về đơn hàng
            boolean askingAboutOrder = lowerMessage.contains("đơn hàng") ||
                    lowerMessage.contains("don hang") ||
                    lowerMessage.contains("order") ||
                    lowerMessage.contains("mua") ||
                    lowerMessage.contains("đã mua") ||
                    lowerMessage.contains("da mua") ||
                    lowerMessage.contains("đặt hàng") ||
                    lowerMessage.contains("dat hang") ||
                    lowerMessage.contains("trạng thái") ||
                    lowerMessage.contains("trang thai") ||
                    lowerMessage.contains("status") ||
                    lowerMessage.contains("giao hàng") ||
                    lowerMessage.contains("giao hang") ||
                    lowerMessage.contains("shipping") ||
                    lowerMessage.contains("delivery") ||
                    lowerMessage.contains("thanh toán") ||
                    lowerMessage.contains("thanh toan") ||
                    lowerMessage.contains("payment") ||
                    lowerMessage.contains("hủy đơn") ||
                    lowerMessage.contains("huy don") ||
                    lowerMessage.contains("cancel order") ||
                    lowerMessage.contains("đơn của tôi") ||
                    lowerMessage.contains("don cua toi") ||
                    lowerMessage.contains("my order") ||
                    lowerMessage.contains("lịch sử mua") ||
                    lowerMessage.contains("lich su mua") ||
                    lowerMessage.contains("purchase history");
            // Lấy danh sách đơn hàng với fetch join để tránh LazyInitializationException
            List<OrderDTO> orders = getOrdersByAccountId(accountId);
            log.info("📦 Đã lấy {} đơn hàng cho account {}", orders.size(), accountId);
            
            if (orders.isEmpty()) {
                if (askingAboutOrder) {
                    return """
                        ════════════════════════════════════════════════════════════
                        📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG (DỮ LIỆU THỰC TỪ DATABASE)
                        ════════════════════════════════════════════════════════════
                        
                        ⚠️ QUAN TRỌNG: Đây là dữ liệu THỰC TẾ từ database.
                        
                        ❌ KHÔNG CÓ ĐƠN HÀNG:
                        - Khách hàng CHƯA CÓ đơn hàng nào trong hệ thống
                        - Tổng số đơn hàng: 0
                        - Đã kiểm tra database và xác nhận: KHÔNG có đơn hàng nào
                        
                        ⚠️ BẠN PHẢI TRẢ LỜI:
                        1. Nói rõ ràng: "Tôi đã kiểm tra trong hệ thống và không tìm thấy đơn hàng nào cho số điện thoại/email này"
                        2. Giải thích: "Có thể bạn chưa đặt hàng hoặc số điện thoại/email không khớp với tài khoản đã đặt hàng"
                        3. Đề xuất: "Vui lòng kiểm tra lại thông tin hoặc liên hệ bộ phận hỗ trợ nếu bạn chắc chắn đã đặt hàng"
                        4. KHÔNG ĐƯỢC tự tạo, bịa đặt, hoặc nói về đơn hàng không tồn tại
                        5. KHÔNG ĐƯỢC sử dụng thông tin từ conversation history để tạo đơn hàng giả
                        6. Nếu conversation history có đề cập đến đơn hàng, nhưng database không có, bạn PHẢI nói "Không tìm thấy đơn hàng"
                        
                        ════════════════════════════════════════════════════════════
                        """;
                }
                return ""; // Không trả về gì nếu không hỏi và không có đơn hàng
            }

            StringBuilder context = new StringBuilder("""
                ════════════════════════════════════════════════════════════
                📦 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH HÀNG (DỮ LIỆU THỰC TỪ DATABASE)
                ════════════════════════════════════════════════════════════
                
                ⚠️ QUAN TRỌNG: Đây là dữ liệu THỰC TẾ từ database. Bạn PHẢI chỉ sử dụng thông tin này.
                KHÔNG ĐƯỢC tự tạo, bịa đặt, hoặc thay đổi bất kỳ thông tin nào.
                
                Tổng số đơn hàng: %d
                
                """.formatted(orders.size()));

            // Chỉ lấy 5 đơn hàng gần nhất để không quá dài
            List<OrderDTO> recentOrders = orders.stream()
                    .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                    .limit(5)
                    .collect(Collectors.toList());

            for (OrderDTO order : recentOrders) {
                // Format ngày đặt hàng
                String orderDateStr = order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                
                // Format trạng thái
                String statusStr = switch (order.getStatus()) {
                    case "PENDING" -> "Chờ xác nhận (PENDING)";
                    case "PROCESSING" -> "Đang xử lý (PROCESSING)";
                    case "DELIVERING" -> "Đang giao hàng (DELIVERING)";
                    case "COMPLETED" -> "Đã hoàn thành (COMPLETED)";
                    case "CANCELLED" -> "Đã hủy (CANCELLED)";
                    case "RETURNED" -> "Đã trả lại (RETURNED)";
                    default -> order.getStatus();
                };
                
                context.append(String.format(
                    "📦 ĐƠN HÀNG #%d (ID thực tế từ database):\n" +
                    "  - Ngày đặt: %s\n" +
                    "  - Trạng thái: %s\n" +
                    "  - Tổng tiền: %.0f VNĐ (chính xác từ database)\n" +
                    "  - Phương thức thanh toán: %s\n",
                    order.getId(),
                    orderDateStr,
                    statusStr,
                    order.getTotalAmount(),
                    order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH"
                ));

                if (order.getDeliveryAddress() != null) {
                    AddressDTO addr = order.getDeliveryAddress();
                    String addressStr = String.format("%s, %s, %s, %s", 
                        addr.getStreet() != null ? addr.getStreet() : "",
                        addr.getWard() != null ? addr.getWard() : "",
                        addr.getDistrict() != null ? addr.getDistrict() : "",
                        addr.getCity() != null ? addr.getCity() : ""
                    ).replaceAll("^,\\s*|,\\s*$", "").replaceAll(",\\s*,", ",").trim();
                    if (!addressStr.isEmpty()) {
                        context.append(String.format("  - Địa chỉ giao hàng: %s\n", addressStr));
                        if (addr.getRecipientName() != null && !addr.getRecipientName().isEmpty()) {
                            context.append(String.format("  - Người nhận: %s\n", addr.getRecipientName()));
                        }
                        if (addr.getPhoneNumber() != null && !addr.getPhoneNumber().isEmpty()) {
                            context.append(String.format("  - Số điện thoại: %s\n", addr.getPhoneNumber()));
                        }
                    }
                }

                if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                    context.append("  - Sách đã mua (danh sách thực tế từ database):\n");
                    for (var item : order.getOrderDetails()) {
                        context.append(String.format("    • %s (số lượng: %d cuốn) - Giá: %.0f VNĐ/cuốn\n", 
                            item.getBookTitle(), 
                            item.getQuantity(), 
                            item.getPriceAtPurchase()));
                    }
                } else {
                    context.append("  - Sách đã mua: Không có thông tin chi tiết trong database\n");
                }

                context.append("\n");
            }
            
            context.append("""
                ════════════════════════════════════════════════════════════
                ⚠️ LƯU Ý CUỐI CÙNG: 
                - Bạn PHẢI chỉ sử dụng thông tin trên đây (từ database)
                - KHÔNG được tự tạo thông tin đơn hàng
                - KHÔNG được thay đổi ID, ngày, số tiền, trạng thái
                - KHÔNG được sử dụng thông tin từ conversation history để tạo đơn hàng giả
                - Nếu context nói "KHÔNG CÓ ĐƠN HÀNG" hoặc "Tổng số đơn hàng: 0", bạn PHẢI nói rõ "Không tìm thấy đơn hàng"
                - Nếu context có thông tin đơn hàng, bạn chỉ được sử dụng thông tin đó, KHÔNG được thêm bớt
                ════════════════════════════════════════════════════════════
                """);

            return context.toString();

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy thông tin đơn hàng: {}", e.getMessage(), e);
            // Nếu có lỗi nhưng user đang hỏi về đơn hàng, trả về thông báo lỗi
            String lowerMessage = userMessage.toLowerCase();
            boolean askingAboutOrder = lowerMessage.contains("đơn hàng") ||
                    lowerMessage.contains("order") ||
                    lowerMessage.contains("mua") ||
                    lowerMessage.contains("đã mua");
            if (askingAboutOrder) {
                return "⚠️ Có lỗi xảy ra khi lấy thông tin đơn hàng. Vui lòng thử lại sau hoặc liên hệ bộ phận hỗ trợ.";
            }
            return ""; // Trả về empty nếu có lỗi và không phải hỏi về đơn hàng
        }
    }

    /**
     * Helper method để lấy số lượng đơn hàng theo accountId (nhanh hơn, không cần load chi tiết)
     */
    @Transactional(readOnly = true)
    private int getOrderCountByAccountId(Long accountId) {
        try {
            List<Order> orders = orderRepository.findByAccountId(accountId);
            return orders.size();
        } catch (Exception e) {
            log.error("❌ Lỗi khi đếm số lượng đơn hàng: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Helper method để lấy đơn hàng theo accountId
     * Sử dụng OrderRepository với fetch join để tránh LazyInitializationException
     */
    @Transactional(readOnly = true)
    private List<OrderDTO> getOrdersByAccountId(Long accountId) {
        try {
            // Sử dụng query với fetch join để load Address và OrderDetails trong cùng session
            List<Order> orders = orderRepository.findByAccountIdWithDetails(accountId);
            log.info("📦 Tìm thấy {} đơn hàng cho account {}", orders.size(), accountId);
            
            if (orders.isEmpty()) {
                log.warn("⚠️ Không tìm thấy đơn hàng nào cho account {}", accountId);
                return List.of();
            }
            
            return orders.stream()
                    .map(this::toOrderDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy đơn hàng: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Convert Order entity to OrderDTO
     */
    private OrderDTO toOrderDTO(Order order) {
        OrderDTO dto = OrderDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .build();

        // Convert delivery address
        if (order.getDeliveryAddress() != null) {
            AddressDTO addressDTO = toAddressDTO(order.getDeliveryAddress());
            dto.setDeliveryAddress(addressDTO);
        }

        // Convert order details
        if (order.getOrderDetails() != null) {
            List<OrderDetailDTO> orderDetailDTOs = order.getOrderDetails().stream()
                    .map(this::toOrderDetailDTO)
                    .collect(Collectors.toList());
            dto.setOrderDetails(orderDetailDTOs);
        }

            return dto;
    }

    /**
     * Xây dựng context về chính sách và FAQ
     */
    private String buildPolicyContext(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // Kiểm tra xem user có hỏi về chính sách không
        boolean askingAboutPolicy = lowerMessage.contains("chính sách") ||
                                   lowerMessage.contains("giao hàng") ||
                                   lowerMessage.contains("đổi trả") ||
                                   lowerMessage.contains("thanh toán") ||
                                   lowerMessage.contains("phí vận chuyển") ||
                                   lowerMessage.contains("miễn phí") ||
                                   lowerMessage.contains("thời gian giao") ||
                                   lowerMessage.contains("faq") ||
                                   lowerMessage.contains("câu hỏi thường gặp") ||
                                   lowerMessage.contains("hỗ trợ") ||
                                   lowerMessage.contains("khách hàng thân thiết") ||
                                   lowerMessage.contains("tích điểm");
        
        if (!askingAboutPolicy) {
            return "";
        }
        
        return """
            📋 THÔNG TIN CHÍNH SÁCH VÀ DỊCH VỤ SEBOOK:
            
            1. CHÍNH SÁCH GIAO HÀNG:
               - Miễn phí giao hàng cho đơn hàng trên 500.000 VNĐ
               - Phí giao hàng: 30.000 VNĐ cho đơn hàng dưới 500.000 VNĐ
               - Thời gian giao hàng: 3-5 ngày làm việc (từ thứ 2 đến thứ 6)
               - Giao hàng toàn quốc
               - Hỗ trợ giao hàng nhanh (1-2 ngày) với phí bổ sung
            
            2. CHÍNH SÁCH ĐỔI TRẢ:
               - Đổi/trả hàng trong vòng 7 ngày kể từ ngày nhận hàng
               - Sách phải còn nguyên vẹn, chưa sử dụng, còn tem nhãn
               - Không áp dụng cho sách đã đọc hoặc có dấu hiệu sử dụng
               - Khách hàng chịu phí vận chuyển khi đổi/trả (trừ trường hợp lỗi từ phía cửa hàng)
               - Liên hệ bộ phận hỗ trợ để được hướng dẫn chi tiết
            
            3. PHƯƠNG THỨC THANH TOÁN:
               - COD (Cash on Delivery - Thanh toán khi nhận hàng): Khách hàng thanh toán bằng tiền mặt khi nhận hàng
               - VNPay: Thanh toán online qua cổng thanh toán VNPay
               - Lưu ý: Cửa hàng chỉ hỗ trợ 2 phương thức thanh toán trên, không có các phương thức khác
            
            4. CHƯƠNG TRÌNH KHÁCH HÀNG THÂN THIẾT:
               - Tích điểm cho mỗi đơn hàng: 1 điểm = 1.000 VNĐ
               - Đổi điểm lấy voucher giảm giá
               - Khách hàng VIP: Giảm giá 5-10% cho đơn hàng
               - Ưu tiên hỗ trợ và chăm sóc đặc biệt
            
            5. HỖ TRỢ KHÁCH HÀNG:
               - Hotline: 1900-xxxx (miễn phí)
               - Email: support@sebook.com
               - Thời gian hỗ trợ: 24/7 qua chatbot, 8:00-22:00 qua hotline
               - Hỗ trợ kỹ thuật: Hướng dẫn đặt hàng, thanh toán, sử dụng website
            
            ⚠️ LƯU Ý: Khi khách hàng hỏi về các chính sách trên, bạn PHẢI sử dụng thông tin này để trả lời chính xác.
            """;
    }

    /**
     * Thống kê đơn hàng: lớn/nhỏ nhất theo tổng tiền và tổng số lượng mua
     */
    @Transactional(readOnly = true)
    private String buildOrderStatsContext() {
        try {
            Order maxTotal = orderRepository.findTopByOrderByTotalAmountDesc();
            Order minTotal = orderRepository.findTopByOrderByTotalAmountAsc();
            
            // Sử dụng native query để tránh lỗi PostgreSQL với DISTINCT + ORDER BY aggregate
            List<Order> maxQtyOrders = orderRepository.findTopByTotalQuantityDescNative(1);
            List<Order> minQtyOrders = orderRepository.findTopByTotalQuantityAscNative(1);
            
            Order maxQty = maxQtyOrders.isEmpty() ? null : maxQtyOrders.get(0);
            Order minQty = minQtyOrders.isEmpty() ? null : minQtyOrders.get(0);
            
            // Fetch orderDetails cho maxQty và minQty để tránh LazyInitializationException
            if (maxQty != null && maxQty.getId() != null) {
                maxQty = orderRepository.findByIdWithDetails(maxQty.getId()).orElse(maxQty);
            }
            if (minQty != null && minQty.getId() != null) {
                minQty = orderRepository.findByIdWithDetails(minQty.getId()).orElse(minQty);
            }

            if (maxTotal == null && minTotal == null && maxQty == null && minQty == null) {
                return "";
            }

            StringBuilder sb = new StringBuilder("""
                📊 THỐNG KÊ ĐƠN HÀNG (LẤY TỪ DATABASE)
                - Dữ liệu thực tế, KHÔNG được bịa
                """);

            if (maxTotal != null) {
                sb.append("\n• Đơn có tổng tiền CAO NHẤT: ID #")
                  .append(maxTotal.getId())
                  .append(", tổng tiền: ")
                  .append(String.format("%.0f", maxTotal.getTotalAmount()))
                  .append(" VND");
            }
            if (minTotal != null) {
                sb.append("\n• Đơn có tổng tiền THẤP NHẤT: ID #")
                  .append(minTotal.getId())
                  .append(", tổng tiền: ")
                  .append(String.format("%.0f", minTotal.getTotalAmount()))
                  .append(" VND");
            }
            if (maxQty != null) {
                int totalQty = maxQty.getOrderDetails() == null ? 0 :
                        maxQty.getOrderDetails().stream().mapToInt(od -> Math.max(0, od.getQuantity())).sum();
                sb.append("\n• Đơn có SỐ LƯỢNG MUA CAO NHẤT: ID #")
                  .append(maxQty.getId())
                  .append(", tổng số lượng: ")
                  .append(totalQty)
                  .append(" cuốn");
            }
            if (minQty != null) {
                int totalQty = minQty.getOrderDetails() == null ? 0 :
                        minQty.getOrderDetails().stream().mapToInt(od -> Math.max(0, od.getQuantity())).sum();
                sb.append("\n• Đơn có SỐ LƯỢNG MUA THẤP NHẤT: ID #")
                  .append(minQty.getId())
                  .append(", tổng số lượng: ")
                  .append(totalQty)
                  .append(" cuốn");
            }

            sb.append("""

                ⚠️ QUY ĐỊNH:
                - Chỉ sử dụng số liệu trên (từ DB)
                - KHÔNG được bịa hoặc thêm đơn hàng khác
                - Nếu user hỏi thông tin cá nhân của người khác: từ chối trả lời
                """);

            return sb.toString();
        } catch (Exception e) {
            log.error("❌ Lỗi buildOrderStatsContext: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Convert Address entity to AddressDTO
     */
    private AddressDTO toAddressDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .street(address.getStreet())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .phoneNumber(address.getPhoneNumber())
                .recipientName(address.getRecipientName())
                .build();
    }

    /**
     * Convert OrderDetail entity to OrderDetailDTO
     */
    private OrderDetailDTO toOrderDetailDTO(OrderDetail orderDetail) {
        return OrderDetailDTO.builder()
                .bookId(orderDetail.getBook().getId())
                .bookTitle(orderDetail.getBook().getTitle())
                .bookImageUrl(orderDetail.getBook().getImageUrl())
                .quantity(orderDetail.getQuantity())
                .priceAtPurchase(orderDetail.getPriceAtPurchase())
                .build();
    }
}

