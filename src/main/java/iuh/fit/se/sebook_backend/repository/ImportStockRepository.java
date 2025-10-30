package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.ImportStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportStockRepository extends JpaRepository<ImportStock, Long> {
    List<ImportStock> findByCreatedById(Long accountId);
    List<ImportStock> findBySupplierId(Long supplierId);
}