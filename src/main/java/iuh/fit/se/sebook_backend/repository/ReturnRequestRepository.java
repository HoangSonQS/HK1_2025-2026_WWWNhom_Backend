package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.ReturnRequest;
import iuh.fit.se.sebook_backend.entity.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByCreatedByIdOrderByCreatedAtDesc(Long accountId);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(ReturnRequestStatus status);
}

