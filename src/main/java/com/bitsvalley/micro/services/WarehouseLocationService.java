package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.ProductCategory;
import com.bitsvalley.micro.domain.WarehouseLocation;
import com.bitsvalley.micro.repositories.ProductCategoryRepository;
import com.bitsvalley.micro.repositories.WarehouseLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author Fru Chifen
 * 25.02.2023
 */
@Service
public class WarehouseLocationService extends SuperService{

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    public Iterable<WarehouseLocation> findAll( long orgId ){
        Iterable<WarehouseLocation> all = warehouseLocationRepository.findByOrgId(orgId);
        return all;
    }

    public void save(WarehouseLocation warehouseLocation){
        warehouseLocationRepository.save(warehouseLocation);
    }

}
