// Thêm vào file src/main/java/iuh/fit/se/sebook_backend/repository/BookRepository.java
package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Book;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

    List<Book> findByCategories_Id(Long categoryId);
}