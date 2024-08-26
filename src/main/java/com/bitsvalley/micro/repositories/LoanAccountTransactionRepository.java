package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.LoanAccountTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LoanAccountTransactionRepository extends CrudRepository<LoanAccountTransaction, Long> {

    List<LoanAccountTransaction> findByLoanAccountAndOrgId(String loanAccount, long orgId);

    Optional<LoanAccountTransaction> findByReferenceAndOrgId(String reference, long orgId);

    @Query(value = "SELECT * FROM loan_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.org_id = :orgId", nativeQuery = true)
    List<LoanAccountTransaction> searchStartEndDate(String startDate, String endDate, long orgId);

    @Query(value = "SELECT * FROM loan_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.created_by = :userName AND ca.org_id = :orgId", nativeQuery = true)
    List<LoanAccountTransaction> searchStartEndDate(String startDate, String endDate, String userName, long orgId);

}
