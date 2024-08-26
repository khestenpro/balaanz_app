package com.bitsvalley.micro.repositories;

import com.bitsvalley.micro.domain.ProductCategory;
import com.bitsvalley.micro.domain.ShopProduct;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @author Fru Chifen
 * 25.02.2023
 */
public interface ShopProductRepository extends CrudRepository<ShopProduct, Long> {

    List<ShopProduct> findByOrgId( long orgID);

    List<ShopProduct> findByOrgIdAndProductCategory(long orgID, ProductCategory category);

}
