package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.BookEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookEmbeddingRepository extends JpaRepository<BookEmbedding, Long> {
    Optional<BookEmbedding> findByBookId(Long bookId);
}

