package w.mazebank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import w.mazebank.models.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByIban(String iban);
}