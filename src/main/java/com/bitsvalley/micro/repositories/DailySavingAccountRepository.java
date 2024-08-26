package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.DailySavingAccount;
import com.bitsvalley.micro.domain.SavingAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailySavingAccountRepository extends CrudRepository<DailySavingAccount, Long> {

    DailySavingAccount findByAccountNumberAndOrgId(String accountNumber, long orgId);

    String USER_ID_ORG_ID = "SELECT * FROM DailySavingAccount sa WHERE sa.user_id = :userId";
    @Query(value = USER_ID_ORG_ID, nativeQuery = true)
    List<DailySavingAccount> findByUserIdAndOrgId(@Param("userId") long userId);

    @Query(value = "SELECT COUNT(*) AS numberOfSavingAccount FROM DailySavingAccount sa where sa.branch_code = :branchCode AND sa.org_id = :orgId", nativeQuery = true)
    int countNumberOfProductsCreatedInBranch(String branchCode, long orgId);

    List<DailySavingAccount> findByUserId(long id);
    DailySavingAccount findByIdAndUserId(long accountId, long userId);
}
