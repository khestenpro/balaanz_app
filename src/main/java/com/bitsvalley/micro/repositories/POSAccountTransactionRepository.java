package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.POSAccountTransaction;
import com.bitsvalley.micro.domain.PaymentTransaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface POSAccountTransactionRepository extends CrudRepository<POSAccountTransaction, Long> {

}
