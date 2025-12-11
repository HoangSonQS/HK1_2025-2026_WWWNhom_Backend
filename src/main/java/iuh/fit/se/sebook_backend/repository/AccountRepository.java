package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByEmail(String email);
    Optional<Account> findByUsernameIgnoreCase(String username);
    Optional<Account> findByEmailIgnoreCase(String email);
    Optional<Account> findByUsernameOrEmailIgnoreCase(String username, String email);
    List<Account> findByPhoneNumber(String phoneNumber); // Đổi thành List để xử lý trường hợp có nhiều account
    List<Account> findByIsActiveTrue();

    @Query("SELECT DISTINCT a FROM Account a JOIN a.roles r WHERE UPPER(r.name) = 'CUSTOMER'")
    List<Account> findCustomers();
}