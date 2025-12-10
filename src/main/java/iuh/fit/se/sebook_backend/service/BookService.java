package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.Category;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.CategoryRepository;
import iuh.fit.se.sebook_backend.service.ai.EmbeddingAsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired(required = false)
    private EmbeddingAsyncService embeddingAsyncService;

    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));
        if (!book.isActive()) {
            throw new IllegalStateException("Book has been hidden");
        }
        return toDto(book);
    }

    public BookDTO createBook(BookDTO bookDTO, String imageUrl) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setDescription(bookDTO.getDescription());
        book.setPublicationYear(bookDTO.getPublicationYear());
        book.setWeightGrams(bookDTO.getWeightGrams());
        book.setPackageDimensions(bookDTO.getPackageDimensions());
        book.setPageCount(bookDTO.getPageCount());
        book.setFormat(bookDTO.getFormat());
        book.setPrice(bookDTO.getPrice());
        book.setQuantity(bookDTO.getQuantity());
        book.setImageUrl(imageUrl);

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(bookDTO.getCategoryIds()));
        book.setCategories(categories);
        book.setActive(true);

        Book savedBook = bookRepository.save(book);
        
        // Tự động tạo embedding cho sách mới (async)
        if (embeddingAsyncService != null) {
            embeddingAsyncService.generateEmbeddingForBookAsync(savedBook);
        }
        
        return toDto(savedBook);
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findByIsActiveTrue().stream().map(this::toDto).collect(Collectors.toList());
    }

    public BookDTO updateBook(Long id, BookDTO bookDTO, String imageUrl) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));
        if (!book.isActive()) {
            throw new IllegalStateException("Book has been hidden");
        }

        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setDescription(bookDTO.getDescription());
        book.setPublicationYear(bookDTO.getPublicationYear());
        book.setWeightGrams(bookDTO.getWeightGrams());
        book.setPackageDimensions(bookDTO.getPackageDimensions());
        book.setPageCount(bookDTO.getPageCount());
        book.setFormat(bookDTO.getFormat());
        book.setPrice(bookDTO.getPrice());
        book.setQuantity(bookDTO.getQuantity());

        if (imageUrl != null) {
            book.setImageUrl(imageUrl);
        }

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(bookDTO.getCategoryIds()));
        book.setCategories(categories);

        Book updatedBook = bookRepository.save(book);
        
        // Tự động tạo/cập nhật embedding cho sách đã sửa (async)
        // Xóa embedding cũ và tạo lại để đảm bảo embedding phù hợp với nội dung mới
        if (embeddingAsyncService != null) {
            embeddingAsyncService.generateEmbeddingForBookAsync(updatedBook, true);
        }
        
        return toDto(updatedBook);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("Book not found with id: " + id);
        }
        try {
            Book book = bookRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));
            book.setActive(false);
            bookRepository.save(book);
        } catch (DataIntegrityViolationException ex) {
            // Phòng trường hợp còn ràng buộc khóa ngoại khác
            throw new IllegalStateException("Sách đang được tham chiếu, không thể xóa");
        }
    }

    public List<BookDTO> getAllBooksSorted(String sortBy, String order) {
        Sort sort = order.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return bookRepository.findByIsActiveTrue(sort).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookDTO> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseAndIsActiveTrue(keyword, keyword)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookDTO> filterBooksByCategory(Long categoryId) {
        return bookRepository.findByCategories_IdAndIsActiveTrue(categoryId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookDTO> getBooksByCategoryWithLimit(Long categoryId, int limit) {
        return bookRepository.findByCategories_IdAndIsActiveTrue(categoryId)
                .stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BookDTO toDto(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setDescription(book.getDescription());
        dto.setPublicationYear(book.getPublicationYear());
        dto.setWeightGrams(book.getWeightGrams());
        dto.setPackageDimensions(book.getPackageDimensions());
        dto.setPageCount(book.getPageCount());
        dto.setFormat(book.getFormat());
        dto.setPrice(book.getPrice());
        dto.setQuantity(book.getQuantity());
        dto.setImageUrl(book.getImageUrl());
        dto.setCategoryIds(book.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        dto.setCategoryNames(book.getCategories().stream().map(Category::getName).collect(Collectors.toSet()));
        return dto;
    }
}