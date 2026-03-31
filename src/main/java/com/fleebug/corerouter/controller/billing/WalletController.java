package com.fleebug.corerouter.controller.billing;

import com.fleebug.corerouter.dto.common.ApiResponse;
import com.fleebug.corerouter.entity.user.User;
import com.fleebug.corerouter.enums.activity.ActivityAction;
import com.fleebug.corerouter.repository.user.UserRepository;
import com.fleebug.corerouter.exception.user.UserNotFoundException;
import com.fleebug.corerouter.service.activity.ActivityLogService;
import com.fleebug.corerouter.service.payment.TransactionService;
import com.fleebug.corerouter.entity.payment.Transaction;

import com.fleebug.corerouter.dto.billing.InitiateTopUpRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet/topup")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management and eSewa top-up APIs")public class WalletController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateTopUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InitiateTopUpRequest topUpRequest,
            HttpServletRequest request) {
        
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        BigDecimal amount = topUpRequest.getAmount();

        Map<String, String> paymentData = transactionService.initiateTopUp(user, amount);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Top-up initiated", paymentData, request));
    }

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSuccess(@RequestParam("data") String data, HttpServletRequest request) {
        Transaction transaction = transactionService.verifyAndCompleteTransaction(data);
        activityLogService.log(
            transaction.getUser(),
            ActivityAction.WALLET_TOPUP_SUCCESS,
            "Wallet top-up successful. NPR " + transaction.getAmount() + " was added to your balance.",
            request.getRemoteAddr()
        );
        
        Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("amount", transaction.getAmount());
        responseData.put("transaction_uuid", transaction.getEsewaTransactionId());
        
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Wallet credited successfully", responseData, request));
    }

    @GetMapping("/failure")
    public ResponseEntity<ApiResponse<String>> handleFailure(@RequestParam(value = "data", required = false) String data, 
                                                             @RequestParam(value = "transaction_uuid", required = false) String transactionUuid,
                                                             HttpServletRequest request) {
        if ((data == null || data.isEmpty()) && (transactionUuid == null || transactionUuid.isEmpty())) {
             return ResponseEntity.ok(ApiResponse.error(HttpStatus.BAD_REQUEST, "Missing transaction data", request));
        }
        transactionService.markTransactionFailed(data, transactionUuid);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Transaction marked as failed", null, request));
    }
}