package com.bitsvalley.micro.services;

import com.bitsvalley.micro.repositories.AccountTypeRepository;
import com.bitsvalley.micro.domain.AccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Fru Chifen
 * 09.07.2021
 */

@Service
public class AccountTypeService extends SuperService{

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    public AccountType getAccountType(String name, long orgId){
        AccountType byName = accountTypeRepository.findByNameAndOrgIdAndActiveTrue(name, orgId);
        return byName;
    }

    public AccountType getAccountTypeByProductCode(String productCode, long orgId){
        AccountType byName = accountTypeRepository.findByNumberAndOrgIdAndActiveTrue(productCode, orgId);
        return byName;
    }

}
