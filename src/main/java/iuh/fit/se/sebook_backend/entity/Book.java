package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 100, nullable = false)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "package_dimensions", length = 100)
    private String packageDimensions;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(length = 50)
    private String format;

    private double price;

    private int quantity;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;
}