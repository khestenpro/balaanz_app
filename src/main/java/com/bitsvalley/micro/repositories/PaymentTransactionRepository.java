package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Branch;
import com.bitsvalley.micro.domain.CurrentAccountTransaction;
import com.bitsvalley.micro.domain.GeneralLedger;
import com.bitsvalley.micro.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PaymentTransactionRepository extends CrudRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrgId( long orgID);


//    @Query(value = "SELECT * FROM daily_saving_account_transaction ca WHERE ca.created_date BETWEEN :startDate AND :endDate AND ca.created_by = :userName AND ca.org_id = :orgId", nativeQuery = true)

    @Query(value = "SELECT * payment_transaction pt WHERE AND pt.username = :username pt.org_id = :orgId", nativeQuery = true)
    List<PaymentTransaction> a( long orgId, String username);

}
