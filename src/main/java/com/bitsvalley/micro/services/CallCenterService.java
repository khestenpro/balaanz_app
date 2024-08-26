package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.AccountTypeRepository;
import com.bitsvalley.micro.repositories.CallCenterRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Service
public class CallCenterService extends SuperService{


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private CallCenterRepository callCenterRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    public List<CallCenter> findByAccountNumber(String accountNumber, Long orgId) {
        return callCenterRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public CallCenter saveCallCenterLog(String reference, String username, String accountNumber, String notes) {

        User byUserName = userRepository.findByUserName(username);
        CallCenter callCenter = new CallCenter();
        callCenter.setReference(reference);
        callCenter.setAccountNumber(accountNumber);
        callCenter.setDate(new Date(System.currentTimeMillis()));
        callCenter.setNotes(notes);
        callCenter.setUserName(username);

        if(null != byUserName){
            callCenter.setOrgId(byUserName.getOrgId());
        }else{
            callCenter.setOrgId(-1);
        }

        callCenterRepository.save(callCenter);
        return callCenter;
    }


    public void callCenterShorteeUpdate(SavingAccount savingAccount, int guarantorAmount,String countryCode) {

//        TODO Add comment in statement for minbalance available raised with 0 transaction amount

        CallCenter callCenter = new CallCenter();
        callCenter.setReference("");
        callCenter.setDate(new Date(System.currentTimeMillis()));
        callCenter.setNotes(BVMicroUtils.SAVINGS_MINIMUM_BALANCE_ADDED_BY + BVMicroUtils.formatCurrency(guarantorAmount,countryCode));
        callCenter.setAccountNumber(savingAccount.getAccountNumber());
        callCenter.setUserName(getLoggedInUserName());
        callCenter.setOrgId(savingAccount.getOrgId());
        callCenterRepository.save(callCenter);

    }

    public void callCenterSavingAccount(SavingAccount savingAccount) {
        saveCallCenterLog("", savingAccount.getUser().getUserName(),
                savingAccount.getAccountNumber(), savingAccount.getAccountType().getDisplayName()+" created by "+savingAccount.getCreatedBy() );
    }

    public void callCenterSavingAccountTransaction(SavingAccountTransaction savingAccountTransaction, RuntimeSetting rt) {
        saveCallCenterLog(savingAccountTransaction.getReference(), savingAccountTransaction.getSavingAccount().getUser().getUserName(),
                savingAccountTransaction.getSavingAccount().getAccountNumber(), BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()));
    }

    public void callCenterDailySavingAccount(DailySavingAccount savingAccount) {
        saveCallCenterLog("", savingAccount.getUser().getUserName(),
                savingAccount.getAccountNumber(), savingAccount.getAccountType().getDisplayName());
    }

    public void callCenterCurrentAccount(CurrentAccount currentAccount) {
        saveCallCenterLog("", currentAccount.getUser().getUserName(),
                currentAccount.getAccountNumber(), currentAccount.getAccountType().getDisplayName()+" created by "+currentAccount.getCreatedBy() );
    }

    public void callCenterUserAccount(User user, String notes) {
        CallCenter callCenter = new CallCenter();
        callCenter.setReference("");
        callCenter.setNotes(notes);
        callCenter.setUserName(user.getUserName());
        callCenter.setDate(new Date());
        callCenter.setOrgId(user.getOrgId());
        callCenterRepository.save(callCenter);
    }

}