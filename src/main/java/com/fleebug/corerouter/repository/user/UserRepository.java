package com.fleebug.corerouter.repository.user;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query("SELECT COALESCE(SUM(u.balance), 0) FROM User u WHERE u.role = :role")
    BigDecimal sumAllBalancesByRole(@Param("role") UserRole role);

    // Analytics queries for date range
    long countByStatusAndCreatedAtBetween(UserStatus status, LocalDateTime from, LocalDateTime to);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Pagination with role and status
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    // Pagination with role only
    Page<User> findByRole(UserRole role, Pageable pageable);

    // Pagination with status only
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Integer userId);
}
