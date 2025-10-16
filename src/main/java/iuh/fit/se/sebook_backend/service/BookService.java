package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.BookDTO;
import iuh.fit.se.sebook_backend.entity.Book;
import iuh.fit.se.sebook_backend.entity.Category;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

    public BookDTO createBook(BookDTO bookDTO, String imageUrl) {
        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setPrice(bookDTO.getPrice());
        book.setQuantity(bookDTO.getQuantity());
        book.setImageUrl(imageUrl);

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(bookDTO.getCategoryIds()));
        book.setCategories(categories);

        Book savedBook = bookRepository.save(book);
        return toDto(savedBook);
    }

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public BookDTO updateBook(Long id, BookDTO bookDTO, String imageUrl) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setPrice(bookDTO.getPrice());
        book.setQuantity(bookDTO.getQuantity());

        if (imageUrl != null) {
            book.setImageUrl(imageUrl);
        }

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(bookDTO.getCategoryIds()));
        book.setCategories(categories);

        Book updatedBook = bookRepository.save(book);
        return toDto(updatedBook);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new IllegalArgumentException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    public List<BookDTO> getAllBooksSorted(String sortBy, String order) {
        Sort sort = order.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return bookRepository.findAll(sort).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookDTO> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookDTO> filterBooksByCategory(Long categoryId) {
        return bookRepository.findByCategories_Id(categoryId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private BookDTO toDto(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setPrice(book.getPrice());
        dto.setQuantity(book.getQuantity());
        dto.setImageUrl(book.getImageUrl());
        dto.setCategoryIds(book.getCategories().stream().map(Category::getId).collect(Collectors.toSet()));
        return dto;
    }
}