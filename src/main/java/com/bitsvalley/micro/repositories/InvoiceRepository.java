package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.Invoice;
import com.bitsvalley.micro.domain.Issue;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface InvoiceRepository extends CrudRepository<Invoice, Long> {

    List<Invoice> findByOrgId(long orgID);

}
