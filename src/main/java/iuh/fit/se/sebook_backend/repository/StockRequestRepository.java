package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.StockRequest;
import iuh.fit.se.sebook_backend.entity.StockRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {
    List<StockRequest> findByCreatedByIdOrderByCreatedAtDesc(Long accountId);
    List<StockRequest> findAllByOrderByCreatedAtDesc();
    List<StockRequest> findByStatusOrderByCreatedAtDesc(StockRequestStatus status);
}

