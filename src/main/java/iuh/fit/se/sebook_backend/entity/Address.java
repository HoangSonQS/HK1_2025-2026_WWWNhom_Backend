package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "address_type", length = 50, nullable = false)
    private String addressType; // HOME, OFFICE, OTHER, etc.

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "street", length = 255)
    private String street; // Số nhà, tên đường

    @Column(name = "ward", length = 100)
    private String ward; // Phường/Xã

    @Column(name = "district", length = 100)
    private String district; // Quận/Huyện

    @Column(name = "city", length = 100)
    private String city; // Thành phố/Tỉnh

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // Số điện thoại liên hệ tại địa chỉ này

    @Column(name = "recipient_name", length = 100)
    private String recipientName; // Tên người nhận tại địa chỉ này
}

