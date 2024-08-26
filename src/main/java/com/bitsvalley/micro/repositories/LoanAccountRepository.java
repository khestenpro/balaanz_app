package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.LoanAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LoanAccountRepository extends CrudRepository<LoanAccount, Long> {

    LoanAccount findByAccountNumberAndOrgId(String accountNumber, long orgId);

    @Query(value = "SELECT *  FROM loanaccount la WHERE la.Account_Status NOT IN (0,8) and la.org_id = :orgId", nativeQuery = true)
    List<LoanAccount> findByStatusNotActiveAndOrgId(long orgId);

    @Query(value = "SELECT *  FROM loanaccount la WHERE la.Account_Status IN (0) and la.org_id = :orgId", nativeQuery = true)
    List<LoanAccount> findByStatusActiveAndOrgId(long orgId);

    @Query(value = "SELECT COUNT(*) AS numberOfLoanAccount FROM loanaccount la where la.branch_code = :branchCode and la.org_id = :orgId", nativeQuery = true)
    int countNumberOfProductsCreatedInBranch(String branchCode, long orgId);

}
