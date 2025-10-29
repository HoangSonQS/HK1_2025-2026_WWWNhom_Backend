package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByAccountId(Long accountId);

    Optional<Cart> findByAccountIdAndBookId(Long accountId, Long bookId);
}