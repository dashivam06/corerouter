package com.fleebug.corerouter.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByEsewaTransactionId(String esewaTransactionId);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, java.time.LocalDateTime cutoff);

        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.userId = :userId " +
            "AND t.type = :type " +
            "AND t.status = :status " +
            "AND t.completedAt BETWEEN :from AND :to")
        BigDecimal sumAmountByUserAndTypeAndStatusAndCompletedAtBetween(
            @Param("userId") Integer userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

            @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                "WHERE t.type = :type " +
                "AND t.status = :status " +
                "AND t.completedAt BETWEEN :from AND :to")
            BigDecimal sumAmountByTypeAndStatusAndCompletedAtBetween(
                @Param("type") TransactionType type,
                @Param("status") TransactionStatus status,
                @Param("from") java.time.LocalDateTime from,
                @Param("to") java.time.LocalDateTime to);

    // ---- Earnings by Date Queries (Admin) ----

    /**
     * Get daily earnings (sum of completed WALLET_TOPUP transactions by date)
     * Returns: [date, totalAmount, count]
     */
    @Query("SELECT CAST(t.completedAt AS DATE) as date, COALESCE(SUM(t.amount), 0) as totalAmount, COUNT(t) as count " +
           "FROM Transaction t " +
           "WHERE t.type = :type " +
           "AND t.status = :status " +
           "AND t.completedAt BETWEEN :from AND :to " +
           "GROUP BY CAST(t.completedAt AS DATE) " +
           "ORDER BY CAST(t.completedAt AS DATE) DESC")
    List<Object[]> getEarningsByDateBetween(
           @Param("type") TransactionType type,
           @Param("status") TransactionStatus status,
           @Param("from") java.time.LocalDateTime from,
           @Param("to") java.time.LocalDateTime to);

    /**
     * Get transactions filtered by type, status, and date range with pagination
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.type = :type " +
           "AND t.status = :status " +
           "AND t.completedAt BETWEEN :from AND :to " +
           "ORDER BY t.completedAt DESC")
    Page<Transaction> findByTypeAndStatusAndCompletedAtBetween(
           @Param("type") TransactionType type,
           @Param("status") TransactionStatus status,
           @Param("from") java.time.LocalDateTime from,
           @Param("to") java.time.LocalDateTime to,
           Pageable pageable);

    /**
     * Get transactions filtered by type and status only
     */
    List<Transaction> findByTypeAndStatus(TransactionType type, TransactionStatus status);

    /**
     * Get transactions by type only
     */
    List<Transaction> findByType(TransactionType type);

    /**
     * Get transactions within a date range
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.completedAt BETWEEN :from AND :to " +
           "ORDER BY t.completedAt DESC")
    List<Transaction> findByCompletedAtBetween(
           @Param("from") java.time.LocalDateTime from,
           @Param("to") java.time.LocalDateTime to);

    /**
     * Get transactions by type, status and completion range.
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.type = :type " +
           "AND t.status = :status " +
           "AND t.completedAt BETWEEN :from AND :to " +
           "ORDER BY t.completedAt DESC")
    List<Transaction> findAllByTypeAndStatusAndCompletedAtBetween(
           @Param("type") TransactionType type,
           @Param("status") TransactionStatus status,
           @Param("from") java.time.LocalDateTime from,
           @Param("to") java.time.LocalDateTime to);
}
