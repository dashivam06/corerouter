package com.fleebug.corerouter.repository.payment;

import com.fleebug.corerouter.model.payment.Payment;
import com.fleebug.corerouter.model.user.User;
import com.fleebug.corerouter.model.request.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByUser(User user);

    List<Payment> findByRelatedRequest(Request request);

    List<Payment> findByStatus(String status);

    Payment findByTransactionId(String transactionId);

    List<Payment> findByProductCode(String productCode);
}
