package w.mazebank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import w.mazebank.models.User;

public interface UserRepository extends JpaRepository<User, Long>{

}
