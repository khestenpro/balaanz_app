package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.AccountType;
import com.bitsvalley.micro.domain.SavingAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SavingAccountRepository extends CrudRepository<SavingAccount, Long> {

    SavingAccount findByAccountNumberAndOrgId(String accountNumber, long orgId);

    @Query(value = "SELECT COUNT(*) AS numberOfSavingAccount FROM SavingAccount sa where sa.branchCode = :branchCode AND sa.orgId = :orgId")
    int countNumberOfProductsCreatedInBranch(String branchCode, long orgId);

    List<SavingAccount> findByOrgId(long orgId);

    List<SavingAccount> findByOrgIdAndAccountType(long orgId, AccountType accountType);

}
