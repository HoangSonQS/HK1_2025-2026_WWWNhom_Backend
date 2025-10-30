package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // Dùng AllArgsConstructor để dễ tạo trong truy vấn
public class TopSellingProductDTO {
    private Long bookId;
    private String bookTitle;
    private long totalSold; // Tổng số lượng đã bán
}