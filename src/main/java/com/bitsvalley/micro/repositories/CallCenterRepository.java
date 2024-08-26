package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.List;

public interface CallCenterRepository extends CrudRepository<CallCenter, Long> {

    List<CallCenter> findByAccountNumberAndOrgId(String accountNumber, long orgID);

    List<CallCenter> findByUserName(String userName);

    @Query(value = "SELECT * FROM callcenter cc WHERE cc.date BETWEEN :startDate AND :endDate AND cc.org_id = :orgId", nativeQuery = true)
    List<CallCenter> findByCreatedDateBetween(String startDate, String endDate, long orgId);
}
