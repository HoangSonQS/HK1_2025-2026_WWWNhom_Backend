package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @Column(nullable = false, unique = true)
    private String token; // Token để reset password

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate; // Thời gian hết hạn (thường là 1 giờ)
}

