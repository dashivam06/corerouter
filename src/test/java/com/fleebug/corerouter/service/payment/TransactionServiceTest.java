package com.fleebug.corerouter.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleebug.corerouter.entity.payment.Transaction;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.payment.TransactionStatus;
import com.fleebug.corerouter.enums.payment.TransactionType;
import com.fleebug.corerouter.repository.payment.TransactionRepository;
import com.fleebug.corerouter.repository.task.TaskRepository;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.service.redis.RedisService;
import com.fleebug.corerouter.util.HttpClientUtil;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private HttpClientUtil httpClientUtil;
    
    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1)
                .fullName("Test User")
                .email("test@example.com")
                .build();

        testTransaction = Transaction.builder()
                .transactionId(10)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.WALLET_TOPUP)
                .user(testUser)
                .build();
    }

    @Test
    // Tests that top up aggregations accurately parse through global platform income filters
    void getTopUpAmountByPeriod_ReturnsSummedAmount() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        BigDecimal expectedAmount = new BigDecimal("4500.50");

        when(transactionRepository.sumAmountByTypeAndStatusAndCompletedAtBetween(
                eq(TransactionType.WALLET_TOPUP),
                eq(TransactionStatus.COMPLETED),
                eq(from),
                eq(to)))
                .thenReturn(expectedAmount);

        BigDecimal actualAmount = transactionService.getTopUpAmountByPeriod(from, to);

        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    // Tests that total all-time incoming money from total global wallets calculates correctly
    void getTopUpAmountAllTime_ReturnsTotalAmount() {
        BigDecimal expectedAmount = new BigDecimal("150000.00");

        when(transactionRepository.sumAmountByTypeAndStatus(
                eq(TransactionType.WALLET_TOPUP),
                eq(TransactionStatus.COMPLETED)))
                .thenReturn(expectedAmount);

        BigDecimal actualAmount = transactionService.getTopUpAmountAllTime();

        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    // Tests that the admin transaction filtered page accurately executes filtering operations based on type and status logic
    void getTransactionsByFilters_ReturnsPaginatedTransactions() {
        TransactionStatus filterStatus = TransactionStatus.COMPLETED;
        TransactionType filterType = TransactionType.WALLET_TOPUP;
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transaction> expectedPage = new PageImpl<>(List.of(testTransaction));

        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Transaction> resultPage = transactionService.getTransactionsByFilters(
                filterType, filterStatus, null, null, null, 0, 10);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        assertEquals(TransactionStatus.COMPLETED, resultPage.getContent().get(0).getStatus());
        assertEquals(TransactionType.WALLET_TOPUP, resultPage.getContent().get(0).getType());
    }
}
