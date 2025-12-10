package iuh.fit.se.sebook_backend.service.ai;

import iuh.fit.se.sebook_backend.dto.AddressDTO;
import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.dto.OrderDTO;
import iuh.fit.se.sebook_backend.dto.OrderDetailDTO;
import iuh.fit.se.sebook_backend.entity.Address;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.entity.OrderDetail;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import iuh.fit.se.sebook_backend.service.OrderService;
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
    @SuppressWarnings("unused")
    private final AccountRepository accountRepository;

    // System prompt cho chatbot
    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI thân thiện của cửa hàng sách SEBook, hoạt động 24/7 để hỗ trợ khách hàng.
        
        📚 CÁC TÍNH NĂNG CHÍNH CỦA BẠN:
        
        1. TRA CỨU THÔNG TIN SÁCH:
           - Cung cấp thông tin chi tiết về sách: tên sách, tác giả, giá, thể loại, số lượng tồn kho
           - Kiểm tra tình trạng có sẵn: "Có sẵn" nếu quantity > 0, "Hết hàng" nếu quantity = 0
           - Trả lời câu hỏi về sách một cách chi tiết và chính xác
           - Ví dụ: "Có sẵn cuốn [tên sách] không?" → Bạn phải kiểm tra quantity và trả lời rõ ràng
           
        2. GỢI Ý SÁCH CHO KHÁCH HÀNG:
           - Gợi ý sách dựa trên sở thích, thể loại yêu thích
           - Gợi ý sách tương tự sau khi khách hàng chọn một cuốn sách
           - Gợi ý sách được đánh giá cao hoặc phổ biến
           - Luôn ưu tiên sách có sẵn trong cửa hàng
           
        3. TRA CỨU ĐƠN HÀNG:
           - Kiểm tra trạng thái đơn hàng theo số điện thoại hoặc email
           - Hiển thị thông tin đơn hàng: ID, ngày đặt, trạng thái, sách đã mua, tổng tiền
           - Hỗ trợ tra cứu đổi/trả hàng (hướng dẫn liên hệ bộ phận hỗ trợ)
           
        4. TƯ VẤN VỀ SÁCH:
           - Tư vấn sách theo nhu cầu: hỏi về sở thích, mục đích đọc (trẻ em, phát triển bản thân, kinh doanh, v.v.)
           - Đưa ra gợi ý dựa trên thông tin khách hàng cung cấp
           - Cung cấp thông tin về thể loại, tác giả, nội dung sách
           
        5. CHĂM SÓC KHÁCH HÀNG VÀ GIẢI ĐÁP THẮC MẮC:
           - Giải đáp câu hỏi thường gặp về:
             * Chính sách giao hàng: Miễn phí giao hàng cho đơn hàng trên 500.000 VNĐ, thời gian 3-5 ngày
             * Chính sách đổi trả: Trong vòng 7 ngày, sách còn nguyên vẹn
             * Chương trình khách hàng thân thiết: Tích điểm, giảm giá cho khách hàng VIP
             * Phương thức thanh toán: COD (Thanh toán khi nhận hàng), VNPay
           - Hỗ trợ các vấn đề kỹ thuật: Hướng dẫn sử dụng website, đặt hàng, thanh toán
           - Nếu không thể giải quyết, hướng dẫn liên hệ bộ phận hỗ trợ
           
        6. CHẾ ĐỘ GIAO TIẾP 24/7:
           - Luôn sẵn sàng hỗ trợ khách hàng mọi lúc
           - Trả lời nhanh chóng và chính xác
           - Thân thiện, nhiệt tình, chuyên nghiệp

        7. TRUY VẤN THỐNG KÊ ĐƠN HÀNG (CHỈ DÙNG DỮ LIỆU TỪ DATABASE):
           - Khi được hỏi: đơn hàng tổng tiền cao nhất/thấp nhất, hoặc số lượng mua cao nhất/thấp nhất
           - Chỉ trả lời bằng các số liệu thực được cung cấp trong phần context "📊 THỐNG KÊ ĐƠN HÀNG"
           - KHÔNG được bịa ra đơn hàng hay số liệu khác
        
        ⚠️ QUY TẮC QUAN TRỌNG:
        - Trả lời bằng tiếng Việt một cách tự nhiên và thân thiện
        - Luôn ưu tiên sử dụng dữ liệu thực từ database
        - Nếu không biết câu trả lời, thành thật nói và đề nghị liên hệ bộ phận hỗ trợ
        - TUÂN THỦ BẢO MẬT: KHÔNG được tiết lộ thông tin cá nhân của bất kỳ người dùng nào khác.
          Chỉ cung cấp thông tin cá nhân của chính người đang đăng nhập/tra cứu (nếu có trong context).
          Nếu bị hỏi thông tin cá nhân của người khác, hãy từ chối: "Xin lỗi, tôi không thể cung cấp thông tin cá nhân của người khác."
        
        💬 SỬ DỤNG CONVERSATION HISTORY:
        - Bạn có quyền truy cập vào lịch sử chat trước đó (chat_history)
        - Sử dụng lịch sử để hiểu ngữ cảnh và trả lời phù hợp
        - Ví dụ: Nếu bạn đã yêu cầu số điện thoại ở tin nhắn trước, và user cung cấp số điện thoại ở tin nhắn sau,
          bạn PHẢI hiểu rằng đây là để tra cứu đơn hàng và thực hiện tra cứu ngay
        - ⚠️ QUAN TRỌNG: Nếu conversation history có đề cập đến đơn hàng, nhưng database KHÔNG có đơn hàng,
          bạn PHẢI nói rõ "Không tìm thấy đơn hàng" - KHÔNG ĐƯỢC tự tạo thông tin đơn hàng từ conversation history
        - Đừng hỏi lại những gì đã hỏi trước đó nếu user đã trả lời
        - Duy trì ngữ cảnh xuyên suốt cuộc hội thoại
        
        📚 QUY TẮC GỢI Ý SÁCH (ƯU TIÊN):
        1. ƯU TIÊN GỢI Ý SÁCH TỪ CỬA HÀNG:
           - Nếu có danh sách "Thông tin về các cuốn sách trong cửa hàng" được cung cấp bên dưới
           - Hãy ƯU TIÊN gợi ý các sách từ danh sách này trước
           - Đề cập rõ ràng: "Trong cửa hàng chúng tôi có..." hoặc "Cửa hàng đang có sách..."
        
        2. GỢI Ý SÁCH BÊN NGOÀI (KHI KHÔNG CÓ HOẶC KHÔNG ĐỦ):
           - Nếu danh sách sách từ cửa hàng rỗng, không có sách phù hợp, hoặc không đủ số lượng khách hàng yêu cầu
           - Bạn CÓ THỂ gợi ý thêm sách từ kiến thức chung (từ internet, sách nổi tiếng)
           - Nhưng phải nói rõ: "Ngoài ra, bạn cũng có thể tham khảo..." hoặc "Một số sách khác bạn có thể quan tâm..."
           - Luôn nhấn mạnh rằng những sách này hiện chưa có trong cửa hàng
        
        3. CÁCH TRÌNH BÀY:
           - Luôn bắt đầu với sách từ cửa hàng (nếu có)
           - Sau đó mới đề cập đến sách bên ngoài (nếu cần)
           - Phân biệt rõ ràng giữa sách có sẵn và sách tham khảo
        
        Khi khách hàng hỏi về đơn hàng:
        - Nếu khách hàng đã đăng nhập, bạn sẽ tự động có thông tin đơn hàng của họ
        - Nếu khách hàng chưa đăng nhập, bạn có thể yêu cầu họ cung cấp email hoặc số điện thoại để tra cứu
        - Khi khách hàng cung cấp email hoặc số điện thoại, bạn sẽ tự động tra cứu và hiển thị thông tin đơn hàng
        
        ⚠️ QUAN TRỌNG - KHI KHÔNG TÌM THẤY TÀI KHOẢN:
        - Nếu trong context có phần "⚠️ KHÔNG TÌM THẤY TÀI KHOẢN", bạn PHẢI:
          1. Thông báo rõ ràng: "Xin lỗi, tôi không tìm thấy tài khoản nào với số điện thoại/email [số điện thoại/email bạn đã cung cấp]"
          2. Giải thích: "Có thể số điện thoại/email này chưa được đăng ký trong hệ thống hoặc không chính xác"
          3. Đề xuất giải pháp: "Vui lòng kiểm tra lại thông tin hoặc thử đăng nhập vào tài khoản của bạn. Nếu bạn chưa có tài khoản, vui lòng đăng ký trước"
          4. KHÔNG được trả lời mơ hồ hoặc chuyển sang chủ đề khác
          5. KHÔNG được nói "tôi không thể cung cấp thông tin về số điện thoại" - điều này sai, bạn PHẢI nói rõ là không tìm thấy tài khoản
        
        ⚠️ QUY TẮC NGHIÊM NGẶT KHI TRẢ LỜI VỀ ĐƠN HÀNG:
        1. CHỈ SỬ DỤNG DỮ LIỆU THỰC TỪ DATABASE:
           - Bạn PHẢI chỉ sử dụng thông tin đơn hàng được cung cấp trong phần "Thông tin đơn hàng của khách hàng" bên dưới
           - KHÔNG ĐƯỢC tự tạo, bịa đặt, hoặc suy đoán thông tin đơn hàng
           - KHÔNG ĐƯỢC sử dụng thông tin từ kiến thức chung hoặc ví dụ
           - Nếu không có thông tin đơn hàng trong context, hãy nói rõ "Bạn chưa có đơn hàng nào" hoặc "Không tìm thấy đơn hàng"
        
        2. SỬ DỤNG ĐÚNG THÔNG TIN:
           - Sử dụng ĐÚNG ID đơn hàng từ context (ví dụ: #2, không phải #123456)
           - Sử dụng ĐÚNG ngày đặt hàng từ context (format: yyyy-MM-dd HH:mm:ss)
           - Sử dụng ĐÚNG trạng thái đơn hàng từ context (PENDING, PROCESSING, DELIVERING, COMPLETED, CANCELLED, RETURNED)
           - Sử dụng ĐÚNG tổng tiền từ context (không làm tròn, không thay đổi số)
           - Sử dụng ĐÚNG danh sách sách đã mua từ context (tên sách, số lượng, giá)
           - Sử dụng ĐÚNG địa chỉ giao hàng từ context (nếu có)
        
        3. FORMAT TRẠNG THÁI ĐƠN HÀNG:
           - PENDING → "Chờ xác nhận"
           - PROCESSING → "Đang xử lý"
           - DELIVERING → "Đang giao hàng"
           - COMPLETED → "Đã hoàn thành"
           - CANCELLED → "Đã hủy"
           - RETURNED → "Đã trả lại"
        
        4. NẾU KHÔNG CÓ THÔNG TIN:
           - Nếu context không có thông tin đơn hàng, hãy nói rõ "Bạn chưa có đơn hàng nào trong hệ thống"
           - KHÔNG được tự tạo thông tin đơn hàng giả
        
        Nếu bạn không biết câu trả lời, hãy thành thật nói rằng bạn không chắc chắn và đề nghị khách hàng liên hệ bộ phận hỗ trợ.
        """;

    public ChatbotService(BookRepository bookRepository, 
                         CohereEmbeddingService embeddingService,
                         OrderService orderService,
                         BookSearchService bookSearchService,
                         OrderRepository orderRepository,
                         AccountRepository accountRepository) {
        this.bookRepository = bookRepository;
        this.embeddingService = embeddingService;
        this.orderService = orderService;
        this.bookSearchService = bookSearchService;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
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
            // 1. Tìm kiếm sách liên quan (RAG)
            List<Book> relevantBooks = findRelevantBooks(userMessage);
            log.info("📚 Tìm thấy {} sách liên quan", relevantBooks.size());

            // 2. Tạo context từ thông tin sách
            String bookContext = buildContextFromBooks(relevantBooks);

            // 3. Lấy thông tin đơn hàng: chỉ cho người đang đăng nhập
            String orderContext = "";
            Long targetAccountId = accountId;

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

            // 9. Tạo sources (danh sách sách được tham khảo - context đã dùng)
            List<String> sources = relevantBooks.stream()
                    .limit(3) // Chỉ lấy 3 sách đầu tiên
                    .map(Book::getTitle)
                    .collect(Collectors.toList());

            // 10. Trích xuất tên sách được đề xuất từ response của AI
            List<String> suggestedBooks = extractBookNames(aiResponse, relevantBooks);
            
            // Nếu không tìm thấy sách nào được đề xuất trong response,
            // thì dùng sources làm suggestedBooks (vì đó là những sách liên quan nhất)
            if (suggestedBooks.isEmpty() && !sources.isEmpty()) {
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
     * Ưu tiên sử dụng semantic search để tìm sách chính xác hơn
     */
    private List<Book> findRelevantBooks(String userMessage) {
        try {
            // ✅ Ưu tiên 1: Sử dụng semantic search (tìm kiếm thông minh với embedding)
            List<BookDTO> semanticResults = bookSearchService.smartSearch(userMessage, 10);
            
            if (!semanticResults.isEmpty()) {
                // Chuyển BookDTO về Book entity
                List<Book> books = semanticResults.stream()
                        .map(bookDTO -> {
                            // Tìm Book từ ID
                            return bookRepository.findById(bookDTO.getId()).orElse(null);
                        })
                        .filter(book -> book != null)
                        .limit(10) // Lấy tối đa 10 sách từ semantic search
                        .collect(Collectors.toList());
                
                if (!books.isEmpty()) {
                    log.info("✅ Tìm thấy {} sách bằng semantic search", books.size());
                    return books;
                }
            }
            
            // ✅ Ưu tiên 2: Fallback về keyword matching nếu semantic search không có kết quả
            log.info("⚠️ Semantic search không có kết quả, chuyển sang keyword matching");
            List<Book> allBooks = bookRepository.findAll();
            
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

            // ✅ Ưu tiên 3: Nếu vẫn không tìm thấy, trả về sách phổ biến (có nhiều quantity)
            if (relevantBooks.isEmpty()) {
                log.info("⚠️ Keyword matching không có kết quả, trả về sách phổ biến");
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
                📚 THÔNG TIN SÁCH TRONG CỬA HÀNG:
                Hiện tại cửa hàng chưa có sách nào phù hợp với yêu cầu của khách hàng.
                
                ⚠️ HƯỚNG DẪN:
                - Bạn có thể gợi ý sách từ kiến thức chung (sách nổi tiếng, sách phổ biến)
                - Nhưng phải nói rõ: "Hiện tại cửa hàng chưa có sách này, nhưng bạn có thể tham khảo..."
                - Hoặc: "Một số sách tương tự bạn có thể quan tâm (hiện chưa có trong cửa hàng)..."
                """;
        }

        StringBuilder context = new StringBuilder();
        context.append("""
            📚 THÔNG TIN SÁCH TRONG CỬA HÀNG SEBOOK:
            Đây là danh sách các sách có sẵn trong cửa hàng phù hợp với yêu cầu của khách hàng.
            
            ⚠️ HƯỚNG DẪN GỢI Ý VÀ TRẢ LỜI:
            1. ƯU TIÊN: Gợi ý các sách từ danh sách dưới đây trước (sách có sẵn trong cửa hàng)
            2. TÌNH TRẠNG CÓ SẴN:
               - Nếu "Tồn kho" > 0: Trả lời "Có sẵn" hoặc "Còn hàng"
               - Nếu "Tồn kho" = 0: Trả lời "Hết hàng" hoặc "Hiện không còn sẵn"
            3. BỔ SUNG: Nếu khách hàng cần thêm gợi ý hoặc không hài lòng với danh sách, 
               bạn có thể gợi ý thêm sách từ kiến thức chung, nhưng phải nói rõ:
               "Ngoài ra, bạn cũng có thể tham khảo [tên sách] (hiện chưa có trong cửa hàng)"
            4. GỢI Ý SÁCH TƯƠNG TỰ: Dựa trên thể loại, tác giả để gợi ý sách tương tự
            5. TƯ VẤN: Hỏi về sở thích, mục đích đọc để đưa ra gợi ý phù hợp
            
            Danh sách sách có sẵn trong cửa hàng:
            
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
            
            ⚠️ LƯU Ý: 
            - Ưu tiên gợi ý sách từ danh sách trên (sách có sẵn trong cửa hàng)
            - Luôn kiểm tra "Tình trạng" để trả lời chính xác về việc có sẵn hay không
            - Có thể bổ sung gợi ý sách bên ngoài nếu cần, nhưng phải phân biệt rõ ràng
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
            body.put("temperature", 0.7);
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
        
        // Kiểm tra xem response có đề cập đến sách nào trong database không
        for (Book book : relevantBooks) {
            String title = book.getTitle();
            if (title != null) {
                String titleLower = title.toLowerCase();
                // Kiểm tra exact match hoặc partial match
                if (responseLower.contains(titleLower) || 
                    titleLower.contains(responseLower) ||
                    // Kiểm tra từng từ trong title
                    title.split("\\s+").length > 0 && 
                    Arrays.stream(title.split("\\s+"))
                        .anyMatch(word -> word.length() > 3 && responseLower.contains(word.toLowerCase()))) {
                    suggested.add(title);
                }
            }
        }

        // Chỉ trả về sách từ database, không có sách nào khác
        return suggested.stream()
                .distinct()
                .limit(5) // Tăng lên 5 sách
                .collect(Collectors.toList());
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
                body.put("temperature", 0.7);
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
            boolean askingAboutOrder = lowerMessage.contains("đơn hàng") ||
                    lowerMessage.contains("order") ||
                    lowerMessage.contains("mua") ||
                    lowerMessage.contains("đã mua") ||
                    lowerMessage.contains("trạng thái") ||
                    lowerMessage.contains("status") ||
                    lowerMessage.contains("giao hàng") ||
                    lowerMessage.contains("shipping");
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
            Order maxQty = orderRepository.findTopByTotalQuantityDesc(PageRequest.of(0,1))
                    .stream().findFirst().orElse(null);
            Order minQty = orderRepository.findTopByTotalQuantityAsc(PageRequest.of(0,1))
                    .stream().findFirst().orElse(null);

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

