package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.LoanAccount;
import com.bitsvalley.micro.domain.ShareAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ShareAccountRepository extends CrudRepository<ShareAccount, Long> {

    ShareAccount findByAccountNumberAndOrgId(String accountNumber, long orgId);

    @Query(value = "SELECT COUNT(*) AS numberOfShareAccount FROM shareaccount sa where sa.branch_code = :branchCode and sa.org_id = :orgId", nativeQuery = true)
    int countNumberOfProductsCreatedInBranch(String branchCode, long orgId);


//    @Query(value = "SELECT *  FROM shareaccount la WHERE la.Account_Status NOT IN (0,8) and la.org_id = :orgId", nativeQuery = true)
    List<ShareAccount> findByOrgId(long orgId);

}
