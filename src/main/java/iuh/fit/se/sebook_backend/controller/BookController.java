package iuh.fit.se.sebook_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.service.BookService;
import iuh.fit.se.sebook_backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;
    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<BookDTO> createBook(@RequestParam("book") String bookDtoString,
                                              @RequestParam("image") MultipartFile image) throws IOException {
        // Chuyển đổi chuỗi JSON thành BookDTO
        BookDTO bookDTO = new ObjectMapper().readValue(bookDtoString, BookDTO.class);

        // Lưu file ảnh và lấy đường dẫn
        String imageUrl = fileStorageService.save(image);

        // Tạo sách với thông tin và đường dẫn ảnh
        BookDTO createdBook = bookService.createBook(bookDTO, imageUrl);

        return ResponseEntity.ok(createdBook);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id,
                                              @RequestParam("book") String bookDtoString,
                                              @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        BookDTO bookDTO = new ObjectMapper().readValue(bookDtoString, BookDTO.class);
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.save(image);
        }

        BookDTO updatedBook = bookService.updateBook(id, bookDTO, imageUrl);
        return ResponseEntity.ok(updatedBook);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok("Book deleted successfully");
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchBooks(@RequestParam String keyword) {
        return ResponseEntity.ok(bookService.searchBooks(keyword));
    }

    @GetMapping("/sorted")
    public ResponseEntity<List<BookDTO>> getSortedBooks(
            @RequestParam String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        return ResponseEntity.ok(bookService.getAllBooksSorted(sortBy, order));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<BookDTO>> filterBooks(@RequestParam Long categoryId) {
        return ResponseEntity.ok(bookService.filterBooksByCategory(categoryId));
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }
}