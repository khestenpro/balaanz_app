package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.CurrentAccount;
import com.bitsvalley.micro.domain.GeneralLedger;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CurrentAccountRepository extends CrudRepository<CurrentAccount, Long> {

    CurrentAccount findByAccountNumberAndOrgId(String accountNumber, long orgId);

//    @Query(value = "SELECT * FROM currentaccount", nativeQuery = true)
//    List<CurrentAccount> findAll();

    @Query(value = "SELECT COUNT(*) AS numberOfCurrentAccount FROM currentaccount ca where ca.branch_code = :branchCode and ca.org_id = :orgId", nativeQuery = true)
    int countNumberOfProductsCreatedInBranch(String branchCode, long orgId);


}
