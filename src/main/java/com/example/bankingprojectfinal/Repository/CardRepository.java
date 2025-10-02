package com.example.bankingprojectfinal.Repository;

import com.example.bankingprojectfinal.Model.Entity.CardEntity;
import com.example.bankingprojectfinal.Model.Enums.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.smartcardio.Card;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, String> {

    boolean existsByCardNumber(String cardNumber);
    List<CardEntity> findByAccount_Customer_Id(Integer customerId);
    // Counts active/new cards for a specific account.
    Integer countByAccount_AccountNumber(String accountNumber);
    Optional<CardEntity> findByCardNumber(String cardNumber);
    // New: Find cards associated with a specific account number, with pagination.
    Page<CardEntity> findByAccount_AccountNumber(String accountNumber, Pageable pageable);

    // New: Find cards associated with a specific customer ID (via account), with pagination.
    Page<CardEntity> findByAccount_Customer_Id(Integer customerId, Pageable pageable);

    // New: Find all cards with a specific status, with pagination.
    Page<CardEntity> findByStatus(CardStatus status, Pageable pageable);

    // New: Get all cards by account number as a List (non-paginated).
    // Use with caution for large result sets.
    List<CardEntity> findByAccount_AccountNumber(String accountNumber);

    Integer countByAccount_Id(Integer id);
}