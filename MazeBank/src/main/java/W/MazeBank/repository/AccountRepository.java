package w.mazebank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import w.mazebank.models.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
