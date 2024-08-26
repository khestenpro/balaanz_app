package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.ShareAccountBilanz;
import com.bitsvalley.micro.webdomain.ShareAccountBilanzList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Service
public class ShareAccountService extends SuperService{

    @Autowired
    private CallCenterService callCenterService;

    @Autowired
    private ShareAccountRepository shareAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private SavingAccountRepository savingAccountRepository;

    @Autowired
    private ShareAccountTransactionRepository shareAccountTransactionRepository;

    @Autowired
    private SavingAccountService savingAccountService;

    @Autowired
    private CurrentAccountService currentAccountService;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    public void createShareAccount(ShareAccount shareAccount, User user) {

        //TODO: REPLACE WITH DB QUERY
        ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
        UserRole customer = userRoleService.findUserRoleByName("ROLE_CUSTOMER", user.getOrgId());
        userRoleList.add(customer);

        int countNumberOfProductsInBranch = shareAccountRepository.countNumberOfProductsCreatedInBranch(user.getBranch().getCode(), user.getOrgId());
        shareAccount.setAccountNumber(BVMicroUtils.getCobacSavingsAccountNumber(shareAccount.getCountry(),
                shareAccount.getProductCode(),countNumberOfProductsInBranch, user.getCustomerNumber(), shareAccount.getBranchCode())); //TODO: Collision

        shareAccount.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        shareAccount.setCreatedBy(getLoggedInUserName());
        Date date = new Date(System.currentTimeMillis());
        shareAccount.setLastUpdatedDate(date);
        shareAccount.setCreatedDate(date);
        shareAccount.setLastUpdatedBy(getLoggedInUserName());
        shareAccount.setAccountBalance(shareAccount.getUnitSharePrice() * shareAccount.getQuantity());
        shareAccount.setCountry(user.getBranch().getCountry());
        shareAccount.setBranchCode(user.getBranch().getCode());
        shareAccount.setOrgId(user.getOrgId());
        shareAccount.setUser(user);
        shareAccountRepository.save(shareAccount);

        user = userRepository.findById(user.getId()).get();
        user.getShareAccount().add(shareAccount);
        userRepository.save(user);
    }

    public ShareAccount findByAccountNumberAndOrgId(String accountNumber, long orgId) {
        return shareAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public ShareAccountBilanzList getShareAccountBilanzByUser(User user, String countryCode) {
        User aUser = null;
        if (null != user.getUserName()) {
            aUser = userRepository.findByUserName(user.getUserName());
        } else {
            aUser = userRepository.findById(user.getId()).get();
        }
        ArrayList<User> userList = new ArrayList<User>();
        userList.add(aUser);
        return calculateTotalShares(userList, countryCode);
    }

    private ShareAccountBilanzList calculateTotalShares(ArrayList<User> users, String countryCode) {
        double totalSaved = 0.0;
        ShareAccountBilanzList shareAccountBilanzList = new ShareAccountBilanzList();
        for (int i = 0; i < users.size(); i++) {
            List<ShareAccount> shareAccounts = users.get(i).getShareAccount();
            List<ShareAccountTransaction> shareAccountTransactions = new ArrayList<ShareAccountTransaction>();
            ShareAccountBilanz shareAccountBilanz = new ShareAccountBilanz();
            double accountTotalSaved = 0.0;
            for (int j = 0; j < shareAccounts.size(); j++) {
                ShareAccount shareAccount = shareAccounts.get(j);
//                List<ShareAccountTransaction> shareAccountTransaction = shareAccount.getShareAccountTransaction();
                if(shareAccount.getAccountStatus().equals(AccountStatus.ACTIVE)){
                    accountTotalSaved = accountTotalSaved + shareAccount.getAccountBalance();
                }
                shareAccountRepository.save(shareAccount);
//                shareAccountBilanz.
            }
            shareAccountBilanzList.setTotalShare(BVMicroUtils.formatCurrency(accountTotalSaved, countryCode));
        }

//        Collections.reverse(shareAccountBilanzList.getShareAccountBilanz());
        return shareAccountBilanzList;
    }

    @Transactional
    public void transferFromCurrentToShareAccount(CurrentAccount currentAccount,
                                        ShareAccount shareAccount,
                                        double transferAmount,
                                        String notes, String currentName) {
        LocalDateTime now = LocalDateTime.now();
        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

//        ShareAccount shareAccount = shareAccountRepository.findByAccountNumber(toAccountNumber);
        ShareAccountTransaction shareAccountTransaction = getShareAccountTransaction(transferAmount, notes, branchInfo, shareAccount);
        shareAccount.getShareAccountTransaction().add( shareAccountTransaction );
        shareAccount.setAccountStatus(AccountStatus.ACTIVE);
        shareAccountRepository.save(shareAccount);

//        SavingAccount savingAccount = savingAccountService.findByAccountNumber(fromAccountNumber);
        CurrentAccountTransaction currentAccountTransaction = savingAccountService.getCurrentAccountTransaction(notes, branchInfo, currentAccount, transferAmount * -1, BVMicroUtils.SAVING_SHARE_TRANSFER);
        currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        currentAccountRepository.save(currentAccount);

        generalLedgerService.updateGLAfterSharePurchaseFromCurrent(shareAccountTransaction, currentName );

    }

    @NotNull
    private ShareAccountTransaction getShareAccountTransaction(double transferAmount, String notes, Branch branchInfo, ShareAccount shareAccount) {
        ShareAccountTransaction shareAccountTransaction = new ShareAccountTransaction();
        shareAccountTransaction.setNotes(notes);
        shareAccountTransaction.setOrgId(shareAccount.getOrgId());
        shareAccountTransaction.setShareAccount(shareAccount);
        shareAccountTransaction.setShareAmount(transferAmount);
        shareAccountTransaction.setModeOfPayment(BVMicroUtils.TRANSFER);
        shareAccountTransaction.setBranch(branchInfo.getId());
        shareAccountTransaction.setBranchCode(branchInfo.getCode());
        shareAccountTransaction.setBranchCountry(branchInfo.getCountry());
        createShareAccountTransaction( shareAccountTransaction );
        return shareAccountTransaction;
    }

    @Transactional
    public void createShareAccountTransaction(ShareAccountTransaction savingAccountTransaction) {
        //Get id of savingAccount transaction
        savingAccountTransaction.setReference(BVMicroUtils.getSaltString()); //Collision
        savingAccountTransaction.setCreatedBy(getLoggedInUserName());
        savingAccountTransaction.setCreatedDate(LocalDateTime.now());
        shareAccountTransactionRepository.save(savingAccountTransaction);
    }
}
