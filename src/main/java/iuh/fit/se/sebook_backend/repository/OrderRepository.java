package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountId(Long accountId);
}