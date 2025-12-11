package iuh.fit.se.sebook_backend.service.ai;

import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.BookEmbedding;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.BookEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookSearchService {

    private static final Logger log = LoggerFactory.getLogger(BookSearchService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.2; // Giảm ngưỡng để không bỏ sót kết quả
    
    // Trọng số cho hybrid search
    private static final double SEMANTIC_WEIGHT = 0.6; // 60% cho semantic similarity
    private static final double TEXT_MATCH_WEIGHT = 0.4; // 40% cho text matching
    
    // Boost factors
    private static final double TITLE_EXACT_MATCH_BOOST = 0.3; // Boost khi query xuất hiện trong title
    private static final double TITLE_PARTIAL_MATCH_BOOST = 0.15; // Boost khi từ khóa xuất hiện trong title
    private static final double AUTHOR_MATCH_BOOST = 0.1; // Boost khi query xuất hiện trong author
    private static final double CATEGORY_MATCH_BOOST = 0.25; // Boost khi thể loại khớp với query (rất quan trọng)

    private final BookRepository bookRepository;
    private final BookEmbeddingRepository embeddingRepository;
    private final AIService aiService;

    public BookSearchService(BookRepository bookRepository,
                             BookEmbeddingRepository embeddingRepository,
                             AIService aiService) {
        this.bookRepository = bookRepository;
        this.embeddingRepository = embeddingRepository;
        this.aiService = aiService;
    }

    /**
     * Semantic search tối ưu: batch load embeddings, tính cosine similarity
     * CHỈ sử dụng embedding từ table book_embedding trong database
     * KHÔNG tạo ra sách giả, chỉ trả về sách có trong database SEBook
     * 
     * @param query Câu truy vấn tìm kiếm
     * @param limit Số lượng kết quả tối đa (mặc định 10)
     * @return Danh sách sách được sắp xếp theo độ liên quan (chỉ từ database)
     */
    @Transactional(readOnly = true)
    public List<BookDTO> smartSearch(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("⚠️ Query rỗng, trả về danh sách trống");
            return List.of();
        }

        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
        log.info("🔍 Bắt đầu tìm kiếm semantic với query: '{}', limit: {}", query, resultLimit);

        try {
            // 1. Tạo embedding cho query
            List<Double> queryVector = aiService.generateEmbedding(query.trim());
            if (queryVector.isEmpty()) {
                log.warn("⚠️ Không thể tạo embedding cho query: '{}'", query);
                return List.of();
            }

            // 2. Load tất cả sách và embedding vào bộ nhớ (CHỈ từ database)
            // CHỈ lấy sách có isActive = true
            // Tất cả sách đều từ database SEBook, không có sách bên ngoài
            List<Book> allBooks = bookRepository.findByIsActiveTrue();
            if (allBooks.isEmpty()) {
                log.info("📚 Không có sách nào active trong database");
                return List.of();
            }

            // 3. Map Book -> embedding vector (CHỈ từ table book_embedding)
            // Chỉ sử dụng embedding đã được tạo sẵn trong database
            Map<Book, List<Double>> bookVectors = new HashMap<>();
            List<BookEmbedding> allEmbeddings = embeddingRepository.findAll();

            // Build map BookId -> BookEmbedding
            Map<Long, BookEmbedding> embeddingMap = allEmbeddings.stream()
                    .filter(be -> be.getBook() != null)
                    .collect(Collectors.toMap(be -> be.getBook().getId(), be -> be));

            int missingEmbeddings = 0;
            for (Book book : allBooks) {
                BookEmbedding embedding = embeddingMap.get(book.getId());
                if (embedding != null && embedding.getEmbeddingVector() != null && !embedding.getEmbeddingVector().isEmpty()) {
                    bookVectors.put(book, embedding.getEmbeddingVector());
                } else {
                    missingEmbeddings++;
                    log.debug("📝 Sách '{}' chưa có embedding, sẽ bỏ qua trong lần tìm kiếm này", book.getTitle());
                }
            }

            if (missingEmbeddings > 0) {
                log.info("ℹ️ Có {} sách chưa có embedding, đã bỏ qua", missingEmbeddings);
            }

            if (bookVectors.isEmpty()) {
                log.warn("⚠️ Không có sách nào có embedding để tìm kiếm");
                return List.of();
            }

            // 4. Tính hybrid score: kết hợp semantic similarity + text matching
            Map<Book, Double> scoreMap = new HashMap<>();
            String queryLower = query.trim().toLowerCase();
            String[] queryWords = queryLower.split("\\s+");
            
            // Kiểm tra xem query có chứa từ khóa về thể loại không
            boolean isCategoryQuery = isCategoryRelatedQuery(queryLower, queryWords);
            
            for (Map.Entry<Book, List<Double>> entry : bookVectors.entrySet()) {
                Book book = entry.getKey();
                List<Double> bookVector = entry.getValue();
                
                // 4.1. Tính semantic similarity (0.0 - 1.0)
                double semanticScore = cosineSimilarity(queryVector, bookVector);
                
                // 4.2. Tính text matching score (0.0 - 1.0)
                double textScore = calculateTextMatchScore(book, queryLower, queryWords);
                
                // 4.3. Tính hybrid score (kết hợp semantic + text matching)
                double hybridScore = (semanticScore * SEMANTIC_WEIGHT) + (textScore * TEXT_MATCH_WEIGHT);
                
                // 4.4. Kiểm tra category matching nếu query về thể loại
                if (isCategoryQuery) {
                    boolean hasMatchingCategory = hasMatchingCategory(book, queryLower, queryWords);
                    if (!hasMatchingCategory) {
                        // Nếu query rõ ràng về thể loại và sách không khớp, loại bỏ hoàn toàn
                        // (không thêm vào scoreMap)
                        log.debug("❌ Sách '{}' không khớp thể loại, loại bỏ khỏi kết quả", book.getTitle());
                        continue; // Bỏ qua sách này
                    } else {
                        // Nếu có category khớp, boost mạnh
                        hybridScore = Math.min(hybridScore * 1.5, 1.0); // Tăng 50% điểm (tối đa 1.0)
                        log.debug("✅ Sách '{}' khớp thể loại, boost điểm", book.getTitle());
                    }
                }
                
                // 4.5. Áp dụng boost cho exact/partial matches
                double boostedScore = applyBoosts(book, queryLower, queryWords, hybridScore);
                
                // Chỉ thêm vào kết quả nếu score >= ngưỡng tối thiểu
                if (boostedScore >= MIN_SIMILARITY_THRESHOLD) {
                    scoreMap.put(book, boostedScore);
                }
            }

            // 5. Sắp xếp và lấy top kết quả
            List<Book> topBooks = scoreMap.entrySet().stream()
                    .sorted(Map.Entry.<Book, Double>comparingByValue().reversed())
                    .limit(resultLimit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 6. Map sang BookDTO
            List<BookDTO> results = topBooks.stream()
                    .map(this::toBookDTO)
                    .collect(Collectors.toList());

            log.info("✅ Tìm thấy {} kết quả cho query: '{}'", results.size(), query);
            return results;

        } catch (Exception e) {
            log.error("❌ Lỗi khi thực hiện semantic search: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Kiểm tra xem query có liên quan đến thể loại không
     */
    private boolean isCategoryRelatedQuery(String queryLower, String[] queryWords) {
        // Danh sách từ khóa về thể loại (dựa trên các thể loại trong database)
        String[] categoryKeywords = {
            "thể loại", "the loai", "loại", "loai", "category", "genre", "thể loại sách", "the loai sach",
            // Văn học
            "văn học", "van hoc", "literature", "fiction", "văn chương", "van chuong",
            // Tiểu thuyết
            "tiểu thuyết", "tieu thuyet", "novel", "tiểu thuyết", "tieu thuyet",
            // Lịch sử
            "lịch sử", "lich su", "history", "sử liệu", "su lieu", "historical", "lịch sử việt nam", "lich su viet nam",
            // Thiếu nhi
            "thiếu nhi", "thieu nhi", "children", "kid", "trẻ em", "tre em", "children's", "sách thiếu nhi", "sach thieu nhi",
            // Khoa học
            "khoa học", "khoa hoc", "science", "khoa học tự nhiên", "khoa hoc tu nhien", "scientific",
            // Kinh doanh
            "kinh doanh", "business", "kinh tế", "kinh te", "economics", "quản trị", "quan tri", "management",
            // Công nghệ thông tin
            "công nghệ", "cong nghe", "technology", "công nghệ thông tin", "cong nghe thong tin", 
            "IT", "information technology", "lập trình", "lap trinh", "programming", "coding", "software",
            // Chính trị
            "chính trị", "chinh tri", "politics", "political", "chính trị học", "chinh tri hoc",
            // Quân sự
            "quân sự", "quan su", "military", "quốc phòng", "quoc phong", "defense",
            // Hồi ký - Tự truyện
            "hồi ký", "hoi ky", "tự truyện", "tu truyen", "memoir", "autobiography", "hồi ký - tự truyện", "hoi ky - tu truyen",
            // Truyện tranh
            "truyện tranh", "truyen tranh", "comic", "manga", "graphic novel", "truyện tranh", "truyen tranh",
            // Sách tẩm đá (có thể là lỗi đánh máy, nhưng vẫn hỗ trợ)
            "sách tẩm đá", "sach tam da"
        };
        
        for (String keyword : categoryKeywords) {
            if (queryLower.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Kiểm tra xem sách có thể loại khớp với query không
     * Hỗ trợ mapping từ khóa tìm kiếm sang tên thể loại trong database
     */
    private boolean hasMatchingCategory(Book book, String queryLower, String[] queryWords) {
        if (book.getCategories() == null || book.getCategories().isEmpty()) {
            return false;
        }
        
        // Tạo map từ khóa tìm kiếm -> tên thể loại trong database
        Map<String, Set<String>> keywordToCategories = createCategoryMapping();
        
        // Tìm các thể loại có thể khớp với query
        Set<String> possibleCategories = new HashSet<>();
        
        // Kiểm tra toàn bộ query trước
        for (Map.Entry<String, Set<String>> entry : keywordToCategories.entrySet()) {
            if (queryLower.contains(entry.getKey())) {
                possibleCategories.addAll(entry.getValue());
            }
        }
        
        // Kiểm tra từng từ trong query
        for (String word : queryWords) {
            if (word.length() > 2) {
                for (Map.Entry<String, Set<String>> entry : keywordToCategories.entrySet()) {
                    String keyword = entry.getKey();
                    // Kiểm tra cả từ đầy đủ và từng từ
                    if (keyword.contains(word) || word.contains(keyword) || queryLower.contains(keyword)) {
                        possibleCategories.addAll(entry.getValue());
                    }
                }
            }
        }
        
        // Kiểm tra từng category của sách
        for (var category : book.getCategories()) {
            if (category != null && category.getName() != null) {
                String categoryName = category.getName().toLowerCase().trim();
                
                // 1. Kiểm tra exact match với tên thể loại (quan trọng nhất)
                if (queryLower.contains(categoryName) || categoryName.contains(queryLower)) {
                    log.debug("✅ Exact match: query '{}' khớp với thể loại '{}'", queryLower, categoryName);
                    return true;
                }
                
                // 2. Kiểm tra với possible categories từ mapping
                for (String possibleCategory : possibleCategories) {
                    if (categoryName.equals(possibleCategory) || categoryName.contains(possibleCategory) || possibleCategory.contains(categoryName)) {
                        log.debug("✅ Mapping match: query '{}' khớp với thể loại '{}' qua mapping", queryLower, categoryName);
                        return true;
                    }
                }
                
                // 3. Kiểm tra từng từ khóa trong query có khớp với tên thể loại không
                for (String word : queryWords) {
                    if (word.length() > 2) {
                        // Kiểm tra cả hai chiều
                        if (categoryName.contains(word) || word.contains(categoryName)) {
                            log.debug("✅ Word match: từ '{}' khớp với thể loại '{}'", word, categoryName);
                            return true;
                        }
                    }
                }
                
                // 4. Kiểm tra các từ trong tên thể loại có xuất hiện trong query không
                String[] categoryWords = categoryName.split("\\s+");
                for (String catWord : categoryWords) {
                    if (catWord.length() > 2 && queryLower.contains(catWord)) {
                        log.debug("✅ Category word match: từ '{}' trong thể loại '{}' xuất hiện trong query", catWord, categoryName);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Tạo mapping từ khóa tìm kiếm -> tên thể loại trong database
     */
    private Map<String, Set<String>> createCategoryMapping() {
        Map<String, Set<String>> mapping = new HashMap<>();
        
        // Văn học
        mapping.put("văn học", Set.of("văn học", "van hoc"));
        mapping.put("van hoc", Set.of("văn học", "van hoc"));
        mapping.put("literature", Set.of("văn học", "van hoc"));
        mapping.put("fiction", Set.of("văn học", "van hoc", "tiểu thuyết", "tieu thuyet"));
        
        // Tiểu thuyết
        mapping.put("tiểu thuyết", Set.of("tiểu thuyết", "tieu thuyet"));
        mapping.put("tieu thuyet", Set.of("tiểu thuyết", "tieu thuyet"));
        mapping.put("novel", Set.of("tiểu thuyết", "tieu thuyet"));
        
        // Lịch sử
        mapping.put("lịch sử", Set.of("lịch sử", "lich su"));
        mapping.put("lich su", Set.of("lịch sử", "lich su"));
        mapping.put("history", Set.of("lịch sử", "lich su", "sử liệu", "su lieu"));
        
        // Sử liệu
        mapping.put("sử liệu", Set.of("sử liệu", "su lieu"));
        mapping.put("su lieu", Set.of("sử liệu", "su lieu"));
        
        // Hồi ký - Tự truyện
        mapping.put("hồi ký", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        mapping.put("hoi ky", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        mapping.put("tự truyện", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        mapping.put("tu truyen", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        mapping.put("memoir", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        mapping.put("autobiography", Set.of("hồi ký - tự truyện", "hoi ky - tu truyen"));
        
        // Quân sự
        mapping.put("quân sự", Set.of("quân sự", "quan su"));
        mapping.put("quan su", Set.of("quân sự", "quan su"));
        mapping.put("military", Set.of("quân sự", "quan su"));
        
        // Chính trị
        mapping.put("chính trị", Set.of("chính trị", "chinh tri"));
        mapping.put("chinh tri", Set.of("chính trị", "chinh tri"));
        mapping.put("politics", Set.of("chính trị", "chinh tri"));
        mapping.put("political", Set.of("chính trị", "chinh tri"));
        
        // Truyện tranh
        mapping.put("truyện tranh", Set.of("truyện tranh", "truyen tranh"));
        mapping.put("truyen tranh", Set.of("truyện tranh", "truyen tranh"));
        mapping.put("comic", Set.of("truyện tranh", "truyen tranh"));
        mapping.put("manga", Set.of("truyện tranh", "truyen tranh"));
        
        // Sách tẩm đá
        mapping.put("sách tẩm đá", Set.of("sách tẩm đá", "sach tam da"));
        mapping.put("sach tam da", Set.of("sách tẩm đá", "sach tam da"));
        
        // Thiếu nhi
        mapping.put("thiếu nhi", Set.of("thiếu nhi", "thieu nhi"));
        mapping.put("thieu nhi", Set.of("thiếu nhi", "thieu nhi"));
        mapping.put("children", Set.of("thiếu nhi", "thieu nhi"));
        mapping.put("kid", Set.of("thiếu nhi", "thieu nhi"));
        
        // Công nghệ thông tin
        mapping.put("công nghệ thông tin", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("cong nghe thong tin", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("công nghệ", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("cong nghe", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("technology", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("IT", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        mapping.put("information technology", Set.of("công nghệ thông tin", "cong nghe thong tin"));
        
        // Kinh doanh
        mapping.put("kinh doanh", Set.of("kinh doanh"));
        mapping.put("business", Set.of("kinh doanh"));
        mapping.put("kinh tế", Set.of("kinh doanh"));
        mapping.put("kinh te", Set.of("kinh doanh"));
        mapping.put("economics", Set.of("kinh doanh"));
        
        // Khoa học
        mapping.put("khoa học", Set.of("khoa học", "khoa hoc"));
        mapping.put("khoa hoc", Set.of("khoa học", "khoa hoc"));
        mapping.put("science", Set.of("khoa học", "khoa hoc"));
        
        return mapping;
    }
    
    /**
     * Tính cosine similarity giữa 2 vector
     */
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return 0.0;
        }

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double a = v1.get(i);
            double b = v2.get(i);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dot / (denominator + 1e-10); // Thêm epsilon để tránh chia cho 0
    }
    
    /**
     * Tính text matching score dựa trên việc query xuất hiện trong title/author
     */
    private double calculateTextMatchScore(Book book, String queryLower, String[] queryWords) {
        double score = 0.0;
        
        String title = (book.getTitle() != null) ? book.getTitle().toLowerCase() : "";
        String author = (book.getAuthor() != null) ? book.getAuthor().toLowerCase() : "";
        
        // 1. Exact match trong title (quan trọng nhất)
        if (title.contains(queryLower)) {
            score += 0.8; // Rất cao nếu query xuất hiện chính xác trong title
        }
        
        // 2. Tất cả từ khóa xuất hiện trong title
        boolean allWordsInTitle = true;
        int wordsInTitle = 0;
        for (String word : queryWords) {
            if (title.contains(word)) {
                wordsInTitle++;
            } else {
                allWordsInTitle = false;
            }
        }
        if (allWordsInTitle && queryWords.length > 0) {
            score += 0.6; // Tất cả từ khóa có trong title
        } else if (wordsInTitle > 0) {
            score += (wordsInTitle * 0.2) / queryWords.length; // Một phần từ khóa có trong title
        }
        
        // 3. Từ khóa xuất hiện trong author
        for (String word : queryWords) {
            if (author.contains(word)) {
                score += 0.05;
                break; // Chỉ cộng 1 lần
            }
        }
        
        // Normalize về 0.0 - 1.0
        return Math.min(score, 1.0);
    }
    
    /**
     * Áp dụng boost cho exact/partial matches và category matching
     */
    private double applyBoosts(Book book, String queryLower, String[] queryWords, double baseScore) {
        double boostedScore = baseScore;
        
        String title = (book.getTitle() != null) ? book.getTitle().toLowerCase() : "";
        String author = (book.getAuthor() != null) ? book.getAuthor().toLowerCase() : "";
        
        // Boost 1: Exact match trong title (rất quan trọng)
        if (title.contains(queryLower)) {
            boostedScore += TITLE_EXACT_MATCH_BOOST;
        }
        
        // Boost 2: Tất cả từ khóa có trong title
        boolean allWordsInTitle = true;
        for (String word : queryWords) {
            if (!title.contains(word)) {
                allWordsInTitle = false;
                break;
            }
        }
        if (allWordsInTitle && queryWords.length > 0) {
            boostedScore += TITLE_PARTIAL_MATCH_BOOST;
        }
        
        // Boost 3: Query xuất hiện trong author
        if (author.contains(queryLower)) {
            boostedScore += AUTHOR_MATCH_BOOST;
        }
        
        // Boost 4: Category matching (rất quan trọng khi tìm theo thể loại)
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            boolean categoryMatched = false;
            for (var category : book.getCategories()) {
                if (category != null && category.getName() != null) {
                    String categoryName = category.getName().toLowerCase();
                    
                    // Kiểm tra exact match với tên thể loại
                    if (categoryName.contains(queryLower) || queryLower.contains(categoryName)) {
                        categoryMatched = true;
                        break;
                    }
                    
                    // Kiểm tra từng từ khóa trong query có khớp với tên thể loại không
                    for (String word : queryWords) {
                        if (word.length() > 2 && categoryName.contains(word)) {
                            categoryMatched = true;
                            break;
                        }
                    }
                    
                    if (categoryMatched) break;
                }
            }
            
            if (categoryMatched) {
                boostedScore += CATEGORY_MATCH_BOOST;
                log.debug("✅ Boost category cho sách '{}' vì khớp thể loại", book.getTitle());
            }
        }
        
        // Đảm bảo không vượt quá 1.0
        return Math.min(boostedScore, 1.0);
    }

    /**
     * Convert Book entity to BookDTO
     */
    private BookDTO toBookDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPrice(book.getPrice());
        dto.setQuantity(book.getQuantity());
        dto.setImageUrl(book.getImageUrl());
        if (book.getCategories() != null) {
            dto.setCategoryIds(book.getCategories().stream()
                    .map(cat -> cat.getId())
                    .collect(Collectors.toSet()));
            dto.setCategoryNames(book.getCategories().stream()
                    .map(cat -> cat.getName())
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
}

