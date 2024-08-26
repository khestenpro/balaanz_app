package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.CurrentAccountTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CurrentAccountTransactionRepository extends CrudRepository<CurrentAccountTransaction, Long> {

    List<CurrentAccountRepository> findByCurrentAccountAndOrgId(String currentAccount, long orgId);

    Optional<CurrentAccountTransaction> findByReferenceAndOrgId(String reference, long orgId);

    @Query(value = "SELECT * FROM current_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND org_id = :orgId", nativeQuery = true)
    List<CurrentAccountTransaction> searchStartEndDate(String startDate, String endDate, long orgId);

    @Query(value = "SELECT * FROM current_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND current_account_id = :id", nativeQuery = true)
    List<CurrentAccountTransaction> searchStartEndDateCurrentAccount(String startDate, String endDate, long id);
    @Query(value = "SELECT * FROM current_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.created_by = :userName AND org_id = :orgId", nativeQuery = true)
    List<CurrentAccountTransaction> searchStartEndDate(String startDate, String endDate, String userName, long orgId);


}
