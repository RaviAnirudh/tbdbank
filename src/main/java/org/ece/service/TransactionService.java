package org.ece.service;

import org.ece.dto.*;
import org.ece.repository.CustomerOperations;
import org.ece.repository.TransactionOperations;
import org.ece.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TransactionService {
    private static final String INSUFFICIENT_BALANCE_ERROR = "Insufficient Balance";
    private static final String SUCCESS_MESSAGE = "Transaction Successful";
    private static final String INVALID_SESSION_ERROR = "Invalid Session";
    private CacheService cacheService;
    private CustomerOperations customerOperations;
    private TransactionOperations transactionOperations;

    public TransactionService(final CacheService cacheService, final TransactionOperations transactionOperations,
    final CustomerOperations customerOperations) {
        this.cacheService = cacheService;
        this.transactionOperations = transactionOperations;
        this.customerOperations = customerOperations;
    }

    public TransactionResponse validateTransactionRequest(final TransactionRequest transactionRequest) {
        double transactionAmount = Double.parseDouble(transactionRequest.getAmount());
        String oldSessionId = transactionRequest.getSessionId();
        SessionData sessionData = cacheService.validateSession(oldSessionId);
        final String newSessionId = SecurityUtils.generateSessionUUID();
        if (!ObjectUtils.isEmpty(sessionData)) {
            cacheService.createSession(newSessionId, sessionData);
            cacheService.killSession(oldSessionId);
            Double accountBalance = (double) customerOperations.findAccountBalanceByCustomerId(sessionData.getUserId());
            return transactionRequest.getTransactionType().equals(TransactionType.CREDIT)
                    ? handleCreditTransactionRequest(accountBalance, newSessionId,
                    sessionData.getUserId(), transactionRequest)
                    : handleDebitTransactionRequest(accountBalance, newSessionId,
                    sessionData.getUserId(), transactionRequest);
        }
        return new TransactionResponse(false, INVALID_SESSION_ERROR);
    }

    private TransactionResponse handleDebitTransactionRequest(double accountBalance, final String newSessionId,
                                                              final String customerId,
                                                              final TransactionRequest transactionRequest) {
        if (accountBalance > Double.parseDouble(transactionRequest.getAmount())) {
            accountBalance = accountBalance - Double.parseDouble(transactionRequest.getAmount());
            saveTransaction(customerId, transactionRequest, accountBalance);
            return new TransactionResponse(true, String.valueOf(accountBalance), newSessionId, SUCCESS_MESSAGE);
        }
        return new TransactionResponse(false, String.valueOf(accountBalance),
                INSUFFICIENT_BALANCE_ERROR, newSessionId);
    }

    private TransactionResponse handleCreditTransactionRequest(double accountBalance, final String newSessionId,
                                                               final String customerId,
                                                               final TransactionRequest transactionRequest) {
        accountBalance = accountBalance + Double.parseDouble(transactionRequest.getAmount());
        accountBalance = accountBalance + Double.parseDouble(transactionRequest.getAmount());
        saveTransaction(customerId, transactionRequest, accountBalance);
        return new TransactionResponse(true, String.valueOf(accountBalance), newSessionId, SUCCESS_MESSAGE);

    }
    public void saveTransaction(final String customerId, final TransactionRequest transactionRequest,
                                double accountBalance) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionRequest.getTransactionType());
        transaction.setCustomerId(customerId);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setTransactionTime(LocalTime.now());
        transaction.setBalance(String.valueOf(accountBalance));
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDetails(transactionRequest.getDetails());
        transactionOperations.save(transaction);
    }

    public StatementResponse validateStatementRequest(final StatementRequest statementRequest) {
        String oldSessionId = statementRequest.getSessioinId();
        SessionData sessionData = cacheService.validateSession(oldSessionId);
        final String newSessionId = SecurityUtils.generateSessionUUID();
        if (!ObjectUtils.isEmpty(sessionData)) {
            cacheService.createSession(newSessionId, sessionData);
            cacheService.killSession(oldSessionId);

        }
        List<Transaction> transactionList = transactionOperations.findByLevelBetween(sessionData.getUserId(),
                statementRequest.getFromDate(), statementRequest.getToDate());
        return new StatementResponse(true, "", newSessionId, transactionList);
    }
}
