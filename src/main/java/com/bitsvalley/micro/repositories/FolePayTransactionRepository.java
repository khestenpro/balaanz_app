package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.FolePayTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface FolePayTransactionRepository extends CrudRepository<FolePayTransaction, Long> {

    @Query(value = "SELECT SUM(amount) from folepay_transaction pt WHERE pt.date BETWEEN :startDate AND :endDate AND pt.org_id = :orgId AND pt.user_id = :userId", nativeQuery = true)
    Double sumDailyLimit(String startDate, String endDate, long orgId, long userId);

}
