package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Invoice;
import com.bitsvalley.micro.domain.InvoiceLineItemDetail;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface InvoiceLineItemDetailRepository extends CrudRepository<InvoiceLineItemDetail, Long> {

}
