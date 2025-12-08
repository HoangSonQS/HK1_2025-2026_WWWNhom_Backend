package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.StockCheckResultDTO;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockCheckService {

    private final BookRepository bookRepository;

    public StockCheckService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Upload CSV: header bookId,countedQuantity
     */
    @Transactional(readOnly = true)
    public List<StockCheckResultDTO> compare(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống");
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            List<long[]> items = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (!headerSkipped) { headerSkipped = true; continue; }
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                long bookId = Long.parseLong(parts[0].trim());
                int counted = Integer.parseInt(parts[1].trim());
                items.add(new long[]{bookId, counted});
            }

            List<Long> ids = items.stream().map(arr -> arr[0]).collect(Collectors.toList());
            Map<Long, Book> bookMap = bookRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(Book::getId, b -> b));

            List<StockCheckResultDTO> result = new ArrayList<>();
            for (long[] arr : items) {
                long bookId = arr[0];
                int counted = (int) arr[1];
                Book book = bookMap.get(bookId);
                int systemQty = book != null ? book.getQuantity() : 0;
                String title = book != null ? book.getTitle() : "N/A";
                result.add(new StockCheckResultDTO(bookId, title, systemQty, counted, counted - systemQty));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage(), e);
        }
    }
}

