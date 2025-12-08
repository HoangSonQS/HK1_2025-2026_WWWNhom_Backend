package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.ReturnToWarehouseRequest;
import iuh.fit.se.sebook_backend.entity.ReturnToWarehouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnToWarehouseRequestRepository extends JpaRepository<ReturnToWarehouseRequest, Long> {
    List<ReturnToWarehouseRequest> findByCreatedByIdOrderByCreatedAtDesc(Long accountId);
    List<ReturnToWarehouseRequest> findAllByOrderByCreatedAtDesc();
    List<ReturnToWarehouseRequest> findByStatusOrderByCreatedAtDesc(ReturnToWarehouseStatus status);
}

