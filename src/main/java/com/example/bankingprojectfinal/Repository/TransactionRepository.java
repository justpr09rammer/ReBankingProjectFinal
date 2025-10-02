package com.example.bankingprojectfinal.Repository;

import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    // Query for today's total outgoing transfers for a specific customer
    // Sums amounts where the customer's account is the debit account and the transaction is completed on today's date.
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
            "WHERE t.debitAccount.customer.id = :customerId " +
            "AND DATE(t.transactionDate) = :transactionDate " +
            "AND t.status = 'COMPLETED'")
    BigDecimal getTodayTotalTransferAmountByDebitAccountCustomer(
            @Param("customerId") Integer customerId,
            @Param("transactionDate") LocalDate transactionDate);

    // Find all transactions where a customer is either the debit account holder or the credit account holder
    Page<TransactionEntity> findByDebitAccount_Customer_IdOrCreditAccount_Customer_Id(Integer debitCustomerId, Integer creditCustomerId, Pageable pageable);

    // Get monthly total for a customer (as either sender or receiver) for suspicion checks
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t " +
            "WHERE (t.debitAccount.customer.id = :customerId OR t.creditAccount.customer.id = :customerId) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.status = 'COMPLETED'")
    BigDecimal getMonthlyTotalByCustomer(
            @Param("customerId") Integer customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find pending transactions (used by the scheduler)
    List<TransactionEntity> findByStatus(TransactionStatus status);
}