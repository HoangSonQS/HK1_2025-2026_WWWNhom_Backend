package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Address;
import iuh.fit.se.sebook_backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByAccount(Account account);
    
    List<Address> findByAccountId(Long accountId);
    
    Optional<Address> findByAccountAndIsDefaultTrue(Account account);
    
    Optional<Address> findByAccountIdAndIsDefaultTrue(Long accountId);
}

