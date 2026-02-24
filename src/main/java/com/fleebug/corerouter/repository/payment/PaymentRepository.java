package com.fleebug.corerouter.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fleebug.corerouter.entity.payment.Payment;
import com.fleebug.corerouter.entity.request.Request;
import com.fleebug.corerouter.entity.user.User;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByUser(User user);

    List<Payment> findByRelatedRequest(Request request);

    List<Payment> findByStatus(String status);

    Payment findByTransactionId(String transactionId);

    List<Payment> findByProductCode(String productCode);
}
