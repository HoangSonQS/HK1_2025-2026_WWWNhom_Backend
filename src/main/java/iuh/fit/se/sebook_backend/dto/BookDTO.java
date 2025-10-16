package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class BookDTO {
    private Long id;
    private String title;
    private String author;
    private double price;
    private int quantity;
    private String imageUrl;
    private Set<Long> categoryIds;
}