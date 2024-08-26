package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.ShopProduct;
import com.bitsvalley.micro.domain.WarehouseLocation;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @author Fru Chifen
 * 25.02.2023
 */
public interface WarehouseLocationRepository extends CrudRepository<WarehouseLocation, Long> {

    List<WarehouseLocation> findByOrgId(long orgID);

}
