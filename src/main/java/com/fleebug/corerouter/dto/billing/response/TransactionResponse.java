package com.fleebug.corerouter.dto.billing.response;

import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a transaction in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Integer transactionId;
    private Integer userId;
    private String userName;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String esewaTransactionId;
    private String relatedTaskId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
