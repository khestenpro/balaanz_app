package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.SavingAccount;
import com.bitsvalley.micro.domain.SavingAccountTransaction;
import com.bitsvalley.micro.repositories.SavingAccountRepository;
import com.bitsvalley.micro.repositories.SavingAccountTransactionRepository;
import com.bitsvalley.micro.repositories.AccountTypeRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SavingAccountTransactionService extends SuperService{

    @Autowired
    private SavingAccountRepository savingAccountRepository;

    @Autowired
    private SavingAccountTransactionRepository savingAccountTransactionRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository repository;

    private double minimumSaving;

    public SavingAccount findByAccountNumberAndOrg(String accountNumber, long orgId){
        return savingAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public Optional<SavingAccountTransaction> findById(long id){
        Optional<SavingAccountTransaction> savingAccountTransaction = savingAccountTransactionRepository.findById(id);
        return savingAccountTransaction;
    }

    public Optional<SavingAccountTransaction> findByReferenceAndOrg(String reference, long orgId){
        Optional<SavingAccountTransaction> savingAccountTransaction = savingAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);
        return savingAccountTransaction;
    }

}
