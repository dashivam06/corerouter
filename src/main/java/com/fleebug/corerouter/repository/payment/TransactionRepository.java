package com.fleebug.corerouter.repository.payment;

import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByEsewaTransactionId(String esewaTransactionId);

        List<Transaction> findTop10ByOrderByCreatedAtDesc();

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, LocalDateTime cutoff);

    List<Transaction> findByTypeAndStatusAndCompletedAtBetween(TransactionType type, TransactionStatus status, LocalDateTime from, LocalDateTime to);

    List<Transaction> findAllByTypeAndStatusAndCompletedAtBetween(TransactionType type, TransactionStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.userId = :userId AND t.type = :type AND t.status = :status AND t.completedAt BETWEEN :from AND :to")
    BigDecimal sumAmountByUserAndTypeAndStatusAndCompletedAtBetween(
            @Param("userId") Integer userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.status = :status AND t.completedAt BETWEEN :from AND :to")
    BigDecimal sumAmountByTypeAndStatusAndCompletedAtBetween(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.status = :status")
    BigDecimal sumAmountByTypeAndStatus(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status
    );

    @Query("SELECT HOUR(t.completedAt) as hour, COALESCE(SUM(t.amount), 0) as total FROM Transaction t WHERE t.type = :type AND t.status = :status AND t.completedAt BETWEEN :from AND :to GROUP BY HOUR(t.completedAt) ORDER BY HOUR(t.completedAt)")
    List<Object[]> sumAmountByTypeAndStatusAndCompletedAtBetweenGroupedByHour(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    Page<Transaction> findAll(org.springframework.data.jpa.domain.Specification<Transaction> spec, Pageable pageable);
}
