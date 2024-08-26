package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.LedgerAccount;
import com.bitsvalley.micro.repositories.LedgerAccountRepository;
import com.bitsvalley.micro.utils.BVMicroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Service
public class LedgerAccountService {

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    public LedgerAccount extractedLedgerAccount(LedgerAccount ledgerAccount, long orgId) {
        LedgerAccount aLedgerAccount = ledgerAccountRepository.findById(ledgerAccount.getId()).get();
        aLedgerAccount.setName(ledgerAccount.getName());
        aLedgerAccount.setCode(ledgerAccount.getCode());
        aLedgerAccount.setDisplayName(ledgerAccount.getDisplayName());
        aLedgerAccount.setOrgId(orgId);
        aLedgerAccount.setInterAccountTransfer(ledgerAccount.getInterAccountTransfer());
        aLedgerAccount.setCreditBalance(ledgerAccount.getCreditBalance());
        aLedgerAccount.setCashTransaction(ledgerAccount.getCashTransaction());
        aLedgerAccount.setCashAccountTransfer(ledgerAccount.getCashAccountTransfer());
        aLedgerAccount.setStatus(ledgerAccount.getStatus().equals("INACTIVE")? BVMicroUtils.INACTIVE:BVMicroUtils.ACTIVE);
        aLedgerAccount.setActive(ledgerAccount.getStatus().equals("INACTIVE")?false:true);
        ledgerAccountRepository.save(aLedgerAccount);
        return aLedgerAccount;
    }


//    public LedgerAccount createLedger(long orgId, String ledgerName, String creditBalance, String category) {
//        LedgerAccount aLedgerAccount = new LedgerAccount();
//        aLedgerAccount.setActive(true);
//        aLedgerAccount.setName(ledgerName);
//        aLedgerAccount.setCode(ledgerName.toUpperCase());
//        aLedgerAccount.setDisplayName(ledgerName.toUpperCase());
//        aLedgerAccount.setOrgId(orgId);
//        aLedgerAccount.setInterAccountTransfer("true");
//        aLedgerAccount.setCreditBalance(creditBalance);
//        aLedgerAccount.setCashTransaction("true");
//        aLedgerAccount.setCashAccountTransfer("true");
//        aLedgerAccount.setStatus(BVMicroUtils.ACTIVE);
//        aLedgerAccount.setCreatedBy("SYSTEM");
//        aLedgerAccount.setCreatedDate(new Date());
//        aLedgerAccount.setActive(true);
//        aLedgerAccount.setCategory(category);
//        ledgerAccountRepository.save(aLedgerAccount);
//        return aLedgerAccount;
//    }

}
