package com.fleebug.corerouter.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByEsewaTransactionId(String esewaTransactionId);

    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus status, java.time.LocalDateTime cutoff);
}
