package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.SavingAccountTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavingAccountTransactionRepository extends CrudRepository<SavingAccountTransaction, Long> {

    List<SavingAccountTransaction> findBySavingAccount(String savingAccount);

    Optional<SavingAccountTransaction> findByReferenceAndOrgId(String reference, long orgId);

    @Query(value = "SELECT * FROM saving_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.org_id = :orgId", nativeQuery = true)
    List<SavingAccountTransaction> searchStartEndDate(String startDate, String endDate, long orgId);

    @Query(value = "SELECT * FROM saving_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND saving_account_id = :id AND ca.org_id = :orgId", nativeQuery = true)
    List<SavingAccountTransaction> searchStartEndDateFilter(String startDate, String endDate, long id, long orgId);

    @Query(value = "SELECT * FROM saving_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.created_by = :userName AND ca.org_id = :orgId", nativeQuery = true)
    List<SavingAccountTransaction> searchStartEndDate(String startDate, String endDate, String userName, long orgId);

//    @Query(value = "SELECT * FROM saving_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND saving_account_id = :id", nativeQuery = true)
//    List<SavingAccountTransaction> searchStartEndDateFilter(String startDate, String endDate, long id);
//    @Query(value = "SELECT * FROM SAVING_ACCOUNT_TRANSACTION ca WHERE saving_account_id = :savingAccountId", nativeQuery = true)
//    List<SavingAccountTransaction> searchStartEndDateAccount(String startDate, String endDate, Long savingAccountId);

    String SAVING_ACC_TRANSACTIONS = "select count(*) from saving_account_transaction\n" +
      "where created_date >=:fromDate AND created_date <=:toDate\n" +
      "AND org_id=:orgId";
    @Query(value = SAVING_ACC_TRANSACTIONS, nativeQuery = true)
    Integer findSavingAccountTransactions(@Param(value = "fromDate")LocalDateTime fromDate,
                                          @Param(value = "toDate") LocalDateTime toDate,
                                          @Param(value = "orgId") int orgId);

    String LOAN_ACC_TRANSACTIONS = "select count(*) from saving_account_transaction\n" +
      "where created_date >=:fromDate AND created_date <=:toDate\n" +
      "AND org_id=:orgId";
    @Query(value = LOAN_ACC_TRANSACTIONS, nativeQuery = true)
    Integer findLoanAccountTransactions(@Param(value = "fromDate")LocalDateTime fromDate,
                                          @Param(value = "toDate") LocalDateTime toDate,
                                          @Param(value = "orgId") int orgId);
    String CURRENT_ACC_TRANSACTIONS = "select count(*) from current_account_transaction\n" +
      "where created_date >=:fromDate AND created_date <=:toDate\n" +
      "AND org_id=:orgId";
    @Query(value = CURRENT_ACC_TRANSACTIONS, nativeQuery = true)
    Integer findCurrentAccountTransactions(@Param(value = "fromDate")LocalDateTime fromDate,
                                        @Param(value = "toDate") LocalDateTime toDate,
                                        @Param(value = "orgId") int orgId);
}
