package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.ProductCategory;
import com.bitsvalley.micro.domain.ShopProduct;
import com.bitsvalley.micro.repositories.ProductCategoryRepository;
import com.bitsvalley.micro.repositories.ShopProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author Fru Chifen
 * 25.02.2023
 */
@Service
public class ProductCategoryService extends SuperService{

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ShopProductRepository shopProductRepository;

    public Iterable<ProductCategory> findAll( long orgId ){
        Iterable<ProductCategory> all = productCategoryRepository.findByOrgId(orgId);
        return all;
    }

    public Iterable<ShopProduct> findByOrgIdProductCategory( long orgId, ProductCategory productCategory){
        Iterable<ShopProduct> all = shopProductRepository.findByOrgIdAndProductCategory (orgId, productCategory);
        return all;
    }

    public void save(ProductCategory productCategory){
        productCategoryRepository.save(productCategory);
    }

}
