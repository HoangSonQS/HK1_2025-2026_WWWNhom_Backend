package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.ImportStockDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportStockDetailRepository extends JpaRepository<ImportStockDetail, Long> {
    List<ImportStockDetail> findByImportStockId(Long importStockId);
    List<ImportStockDetail> findByBookIdOrderByImportStockImportDateDesc(Long bookId);
}