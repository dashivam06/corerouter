package com.fleebug.corerouter.repository.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.user.UserStatus;

import java.math.BigDecimal;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);



    List<User> findByStatus(UserStatus status);

    @Query("SELECT COALESCE(SUM(u.balance), 0) FROM User u")
    BigDecimal sumAllBalances();
    
}
