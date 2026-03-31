package com.fleebug.corerouter.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserRole;
import com.fleebug.corerouter.enums.user.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);



    List<User> findByStatus(UserStatus status);

    long countByStatus(UserStatus status);

    long countByRole(UserRole role);

    long countByCreatedAtBefore(LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(u.balance), 0) FROM User u")
    BigDecimal sumAllBalances();
    
}
