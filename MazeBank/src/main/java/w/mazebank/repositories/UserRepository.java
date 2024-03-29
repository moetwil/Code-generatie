package w.mazebank.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import w.mazebank.models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long, JpaSpecificationExecutor<User>> {
    Optional<User> findByEmail(String email);
    Optional<User> findByBsn(int bsn);

    @Query("""
    SELECT u FROM User u WHERE (u.id <> 1)
    AND (u.firstName LIKE %?1% OR u.lastName LIKE %?1%
    OR u.email LIKE %?1% OR CAST(u.bsn AS string) LIKE %?1%
    OR u.phoneNumber LIKE %?1% OR TO_CHAR(u.dateOfBirth, 'YYYY-MM-DD') LIKE %?1%)
    """)
    List<User> findBySearchString(String search, Pageable pageable);

    @Override
    @Query("SELECT u FROM User u WHERE u.id <> 1")
    Page<User> findAll(Pageable pageable);
}