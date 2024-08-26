package com.bitsvalley.micro.repositories;

 import com.bitsvalley.micro.domain.DisbursementRequestStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DisbursementRequestStatusRepository extends CrudRepository<DisbursementRequestStatus, Long> {

    DisbursementRequestStatus findByRequestId(String requestId);

}
