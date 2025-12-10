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
    private String description;
    private Integer publicationYear;
    private Integer weightGrams;
    private String packageDimensions;
    private Integer pageCount;
    private String format;
    private double price;
    private int quantity;
    private String imageUrl;
    private Set<Long> categoryIds;
    private Set<String> categoryNames;
}