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

	Page<TransactionEntity> findByAccount_Customer_Id(Integer customerId, Pageable pageable);

	List<TransactionEntity> findByStatus(TransactionStatus status);

	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.account.customer.id = :customerId AND t.transactionDate BETWEEN :startDate AND :endDate")
	BigDecimal getMonthlyTotalByCustomer(
			@Param("customerId") Integer customerId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
	);
}