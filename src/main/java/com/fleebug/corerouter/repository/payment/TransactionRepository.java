package com.fleebug.corerouter.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
