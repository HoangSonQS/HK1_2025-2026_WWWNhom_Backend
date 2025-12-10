// Thêm vào file src/main/java/iuh/fit/se/sebook_backend/repository/BookRepository.java
package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByIsActiveTrue();
    List<Book> findByIsActiveTrue(org.springframework.data.domain.Sort sort);

    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseAndIsActiveTrue(String title, String author);

    List<Book> findByCategories_IdAndIsActiveTrue(Long categoryId);

    @Query("SELECT b FROM Book b WHERE b.quantity <= :threshold ORDER BY b.quantity ASC")
    List<Book> findLowStock(@Param("threshold") int threshold);

    @Query("SELECT SUM(b.quantity), SUM(b.quantity * b.price) FROM Book b")
    List<Object[]> sumInventory();

    @Query("SELECT c.id, c.name, SUM(b.quantity), SUM(b.quantity * b.price) " +
            "FROM Book b JOIN b.categories c GROUP BY c.id, c.name")
    List<Object[]> sumInventoryByCategory();
}