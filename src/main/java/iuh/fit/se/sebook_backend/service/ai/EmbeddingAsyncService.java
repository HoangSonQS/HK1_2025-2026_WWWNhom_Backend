package iuh.fit.se.sebook_backend.service.ai;

import iuh.fit.se.sebook_backend.entity.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingAsyncService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingAsyncService.class);

    private final BookEmbeddingGenerator embeddingGenerator;

    public EmbeddingAsyncService(BookEmbeddingGenerator embeddingGenerator) {
        this.embeddingGenerator = embeddingGenerator;
    }

    /**
     * Method async để tạo embedding trong background
     * Sử dụng executor "embeddingTaskExecutor" được cấu hình trong AsyncConfig
     */
    @Async("embeddingTaskExecutor")
    public void generateEmbeddingsAsync() {
        try {
            log.info("🔄 Bắt đầu xử lý async: tạo embedding cho các sách");
            embeddingGenerator.generateAllEmbeddings();
            log.info("✅ Hoàn tất xử lý async: đã tạo embedding cho tất cả sách");
        } catch (Exception e) {
            log.error("❌ Lỗi khi sinh embedding trong async thread: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo embedding cho một cuốn sách cụ thể trong background thread
     * @param book Sách cần tạo embedding
     * @param forceRegenerate Nếu true, xóa embedding cũ và tạo lại (dùng khi update sách)
     */
    @Async("embeddingTaskExecutor")
    public void generateEmbeddingForBookAsync(Book book, boolean forceRegenerate) {
        try {
            log.info("🔄 Bắt đầu tạo embedding cho sách: '{}' (ID: {})", book.getTitle(), book.getId());
            boolean success = embeddingGenerator.generateEmbeddingForBook(book, forceRegenerate);
            if (success) {
                log.info("✅ Đã tạo embedding thành công cho sách: '{}' (ID: {})", book.getTitle(), book.getId());
            } else {
                log.warn("⚠️ Không thể tạo embedding cho sách: '{}' (ID: {}). Sẽ thử lại sau.", 
                        book.getTitle(), book.getId());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo embedding cho sách '{}' (ID: {}): {}", 
                    book.getTitle(), book.getId(), e.getMessage(), e);
        }
    }

    /**
     * Tạo embedding cho một cuốn sách cụ thể trong background thread (không force regenerate)
     * @param book Sách cần tạo embedding
     */
    @Async("embeddingTaskExecutor")
    public void generateEmbeddingForBookAsync(Book book) {
        generateEmbeddingForBookAsync(book, false);
    }

    /**
     * Xóa embedding của một cuốn sách trong background thread
     * @param bookId ID của sách cần xóa embedding
     */
    @Async("embeddingTaskExecutor")
    public void deleteEmbeddingForBookAsync(Long bookId) {
        try {
            log.info("🔄 Bắt đầu xóa embedding cho sách ID: {}", bookId);
            boolean success = embeddingGenerator.deleteEmbeddingForBook(bookId);
            if (success) {
                log.info("✅ Đã xóa embedding thành công cho sách ID: {}", bookId);
            } else {
                log.warn("⚠️ Không tìm thấy embedding để xóa cho sách ID: {}", bookId);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa embedding cho sách ID {}: {}", bookId, e.getMessage(), e);
        }
    }
}

