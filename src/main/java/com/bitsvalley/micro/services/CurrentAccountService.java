package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.CurrentBilanz;
import com.bitsvalley.micro.webdomain.CurrentBilanzList;
import com.bitsvalley.micro.webdomain.GLSearchDTO;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CurrentAccountService extends SuperService {

//
//    @Autowired
//    private SavingAccountRepository savingAccountRepository;

    @Autowired
    CurrentAccountTransactionRepository currentAccountTransactionRepository;

    @Autowired
    CurrentAccountRepository currentAccountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private SavingAccountTransactionRepository savingAccountTransactionRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CallCenterRepository callCenterRepository;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Autowired
    private InterestService interestService;

    @Autowired
    private CallCenterService callCenterService;

    @Autowired
    private LoanAccountService loanAccountService;

    @Autowired
    private LoanAccountTransactionService loanAccountTransactionService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private
    CurrentAccountService currentAccountService;

    private double minimumSaving;

    public CurrentAccount findByAccountNumberAndOrgId(String accountNumber, long orgId) {
        return currentAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public CurrentAccount createCurrentAccount(CurrentAccount currentAccount, User user, Branch branchInfo) {

        currentAccount.setBranchCode(branchInfo.getCode());
        currentAccount.setCountry(branchInfo.getCountry());

//        AccountType accountType = new AccountType();
//        accountType.setName(BVMicroUtils.CURRENT);
//        accountType.setNumber("20");
//        accountType.setCategory("CURRENT");
//        currentAccount.setAccountType(accountType);

        int countNumberOfProductsInBranch =  1 + currentAccountRepository.countNumberOfProductsCreatedInBranch(user.getBranch().getCode(), user.getOrgId());
        currentAccount.setAccountNumber(BVMicroUtils.getCobacSavingsAccountNumber( currentAccount.getCountry(), currentAccount.getProductCode(), countNumberOfProductsInBranch, user.getCustomerNumber(), currentAccount.getBranchCode())); //TODO: Collision

        currentAccount.setAccountStatus(AccountStatus.ACTIVE);
        currentAccount.setCreatedBy(getLoggedInUserName());
        currentAccount.setCreatedDate(new Date(System.currentTimeMillis()));
        currentAccount.setLastUpdatedBy(getLoggedInUserName());
        currentAccount.setAccountLocked(false);
        currentAccount.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        currentAccount.setAccountMinBalance(0);

        AccountType currentAccountType = accountTypeRepository.findByNumberAndOrgIdAndActiveTrue(currentAccount.getProductCode(), user.getOrgId());
        if(currentAccountType == null){ //TODO PATCH DB
            currentAccount.setAccountType(createCurrentAccountType(user.getOrgId()));
        }else{
            currentAccount.setAccountType(currentAccountType);
        }
        currentAccount.setOrgId(user.getOrgId());
        currentAccount.setUser(user);
        currentAccountRepository.save(currentAccount);

        user = userRepository.findById(user.getId()).get();
        user.getCurrentAccount().add(currentAccount);
        userService.saveUser(user);

        callCenterService.callCenterCurrentAccount(currentAccount);
        return currentAccount;
    }

    private AccountType createCurrentAccountType(long orgId) {
        AccountType currentType = new AccountType();
        currentType.setName(BVMicroUtils.CURRENT);
        currentType.setDisplayName(BVMicroUtils.CURRENT);
        currentType.setNumber("20");
        currentType.setCategory("CURRENT");
        currentType.setOrgId(orgId);
        currentType.setActive(true);
        accountTypeRepository.save(currentType);
        return currentType;

    }



    @Transactional
    public CurrentAccountTransaction createCurrentAccountTransaction(CurrentAccountTransaction currentAccountTransaction, CurrentAccount currentAccount) {

        currentAccountTransaction.setCreatedBy(getLoggedInUserName());
        if(currentAccountTransaction.getCreatedDate() == null ){
            currentAccountTransaction.setCreatedDate(LocalDateTime.now());
        }

        currentAccountTransaction.setReference(BVMicroUtils.getSaltString());
        currentAccountTransaction.setAccountBalance(calculateAccountBalance(currentAccountTransaction.getCurrentAmount(),currentAccount));
        currentAccountTransaction.setOrgId(currentAccount.getOrgId());
        currentAccountTransactionRepository.save(currentAccountTransaction);
        currentAccountService.save(currentAccount);
        return currentAccountTransaction;
    }

    private double calculateAccountBalance(double currentAmount, CurrentAccount currentAccount) {
        Double balance = 0.0;
        for (CurrentAccountTransaction transaction: currentAccount.getCurrentAccountTransaction() ) {
            balance = transaction.getCurrentAmount() + balance;
        }
        return currentAmount + balance;
    }

    @Transactional
    public void createCurrentAccountTransaction(CurrentAccount currentAccount, LoanAccountTransaction loanAccountTransaction, String modeOfPayment) {

        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

        CurrentAccountTransaction currentAccountTransaction = new CurrentAccountTransaction();
        currentAccountTransaction.setCurrentAmount(loanAccountTransaction.getLoanAmount());
        currentAccountTransaction.setCurrentAmountInLetters(BVMicroUtils.TRANSFER);
        currentAccountTransaction.setModeOfPayment(modeOfPayment);
        currentAccountTransaction.setBranch(branchInfo.getId());
        currentAccountTransaction.setBranchCode(branchInfo.getCode());
        currentAccountTransaction.setBranchCountry(branchInfo.getCountry());
        currentAccountTransaction.setCurrentAccount(currentAccount);
        currentAccountTransaction.setNotes("Transfer, loan init payment "+ loanAccountTransaction.getLoanAccount().getAccountNumber());
        currentAccountTransaction.setReference(BVMicroUtils.getSaltString());
        currentAccountTransaction.setCreatedBy(getLoggedInUserName());

        currentAccountTransaction.setCreatedDate(LocalDateTime.now());

        currentAccountTransaction.setOrgId(currentAccount.getOrgId());
        currentAccountTransactionRepository.save(currentAccountTransaction);
        currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        currentAccountRepository.save(currentAccount);

    }


    public Optional<CurrentAccount> findById(long id) {
        Optional<CurrentAccount> currentAccount = currentAccountRepository.findById(id);
        return currentAccount;
    }

    public Iterable<CurrentAccount> findAll(){
        return currentAccountRepository.findAll();
    }

    public void save(CurrentAccount save) {
        currentAccountRepository.save(save);
    }


    public CurrentBilanzList getCurrentBilanzByUser(User user, boolean calculateInterest, RuntimeSetting rt) {
        User aUser = null;
        if (null != user.getUserName()) {
            aUser = userRepository.findByUserName(user.getUserName());
        } else {
            aUser = userRepository.findById(user.getId()).get();
        }
        ArrayList<User> userList = new ArrayList<User>();
        userList.add(aUser);
        return calculateUsersInterest(userList, calculateInterest, rt);
    }


    public CurrentBilanzList calculateAccountBilanz(
            List<CurrentAccountTransaction> currentAccountTransactions,
            boolean calculateInterest, RuntimeSetting rt) {
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double savingAccountTransactionInterest = 0.0;

        CurrentBilanzList currentBilanzsList = new CurrentBilanzList();

        for (int k = 0; k < currentAccountTransactions.size(); k++) {
            final CurrentAccountTransaction currentAccountTransaction = currentAccountTransactions.get(k);
            CurrentBilanz currentBilanz = new CurrentBilanz();
            currentBilanz = calculateInterest(currentAccountTransaction, calculateInterest, rt.getCountryCode());
            currentSaved = currentSaved + currentAccountTransaction.getCurrentAmount();
            currentBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, rt.getCountryCode()));
            currentBilanzsList.getCurrentBilanzList().add(currentBilanz);
            totalSaved = totalSaved + currentAccountTransaction.getCurrentAmount();
            if (calculateInterest) {
                savingAccountTransactionInterest = savingAccountTransactionInterest +
                        interestService.calculateInterestAccruedMonthCompounded(
                                currentAccountTransaction.getCurrentAccount().getInterestRate(),
                                currentAccountTransaction.getCreatedDate(),
                                currentAccountTransaction.getCurrentAmount());
                currentBilanzsList.setTotalCurrentInterest(BVMicroUtils.formatCurrency(savingAccountTransactionInterest, rt.getCountryCode()));
            }
        }
        currentBilanzsList.setTotalCurrent(BVMicroUtils.formatCurrency(totalSaved, rt.getCountryCode()));

        Collections.reverse(currentBilanzsList.getCurrentBilanzList());
        return currentBilanzsList;
    }

    public CurrentBilanzList calculateAccountBilanzInterval(boolean calculateInterest, GLSearchDTO glSearchDTO, CurrentAccount currentAccount, RuntimeSetting rt) {
//        String user = currentAccount.getUser().getGender() +" " + currentAccount.getUser().getFirstName() +" " + currentAccount.getUser().getLastName();
        List<CurrentAccountTransaction> currentAccountTransactions = currentAccountTransactionRepository.searchStartEndDateCurrentAccount(glSearchDTO.getStartDate()+ " 00:00:00.000", glSearchDTO.getEndDate()+ " 23:59:59.999", currentAccount.getId());
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double savingAccountTransactionInterest = 0.0;

        CurrentBilanzList currentBilanzsList = new CurrentBilanzList();

        for (int k = 0; k < currentAccountTransactions.size(); k++) {
            final CurrentAccountTransaction currentAccountTransaction = currentAccountTransactions.get(k);
            CurrentBilanz currentBilanz = new CurrentBilanz();
            currentBilanz = calculateInterest(currentAccountTransaction, calculateInterest, rt.getCountryCode());
            currentSaved = currentSaved + currentAccountTransaction.getCurrentAmount();
            currentBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, rt.getCountryCode()));
            currentBilanzsList.getCurrentBilanzList().add(currentBilanz);
            totalSaved = totalSaved + currentAccountTransaction.getCurrentAmount();
            if (calculateInterest) {
                savingAccountTransactionInterest = savingAccountTransactionInterest +
                        interestService.calculateInterestAccruedMonthCompounded(
                                currentAccountTransaction.getCurrentAccount().getInterestRate(),
                                currentAccountTransaction.getCreatedDate(),
                                currentAccountTransaction.getCurrentAmount());
                currentBilanzsList.setTotalCurrentInterest(BVMicroUtils.formatCurrency(savingAccountTransactionInterest, rt.getCountryCode()));
            }
        }
        currentBilanzsList.setTotalCurrent(BVMicroUtils.formatCurrency(totalSaved, rt.getCountryCode()));
        return currentBilanzsList;
    }


    private CurrentBilanzList calculateUsersInterest(ArrayList<User> users, boolean calculateInterest, RuntimeSetting rt) {
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double currentAccountTransactionInterest = 0.0;
        CurrentBilanzList currentBilanzList = new CurrentBilanzList();
        for (int i = 0; i < users.size(); i++) {
            List<CurrentAccount> currentAccounts = users.get(i).getCurrentAccount();
            List<CurrentAccountTransaction> currentAccountTransactions = new ArrayList<CurrentAccountTransaction>();
            for (int j = 0; j < currentAccounts.size(); j++) {
                CurrentAccount currentAccount = currentAccounts.get(j);
                currentAccountTransactions = currentAccount.getCurrentAccountTransaction();
                double accountTotalSaved = 0.0;
                for (int k = 0; k < currentAccountTransactions.size(); k++) {
                    final CurrentAccountTransaction currentAccountTransaction = currentAccountTransactions.get(k);
                    CurrentBilanz currentBilanz = calculateInterest(currentAccountTransaction, calculateInterest, rt.getCountryCode());
                    currentSaved = currentSaved + currentAccountTransaction.getCurrentAmount();
                    currentBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, rt.getCountryCode()));
                    currentBilanzList.getCurrentBilanzList().add(currentBilanz);
                    totalSaved = totalSaved + currentAccountTransaction.getCurrentAmount();
                    accountTotalSaved = accountTotalSaved + currentAccountTransaction.getCurrentAmount();
                    currentAccountTransactionInterest = currentAccountTransactionInterest +
                            interestService.calculateInterestAccruedMonthCompounded(
                                    currentAccountTransaction.getCurrentAccount().getInterestRate(),
                                    currentAccountTransaction.getCreatedDate(),
                                    currentAccountTransaction.getCurrentAmount());
                }
                currentAccount.setAccountBalance(accountTotalSaved);
                currentAccountRepository.save(currentAccount);
            }
        }

        currentBilanzList.setTotalCurrent(BVMicroUtils.formatCurrency(totalSaved, rt.getCountryCode()));
        currentBilanzList.setTotalCurrentInterest(BVMicroUtils.formatCurrency(currentAccountTransactionInterest, rt.getCountryCode()));
        return currentBilanzList;
    }

    private boolean checkMinBalanceLogin( SavingAccount savingAccount) {

        if (savingAccount.getAccountMinBalance() > savingAccount.getAccountBalance()) {
            callCenterService.saveCallCenterLog("",savingAccount.getUser().getUserName(),savingAccount.getAccountNumber(),BVMicroUtils.MINIMUM_BALANCE_NOT_MET_FOR_THIS_ACCOUNT + savingAccount.getAccountNumber());
            return true;
        }

        return false;
    }


    private CurrentBilanz calculateInterest(CurrentAccountTransaction currentAccountTransaction, boolean calculateInterest, String countrCode) {
        CurrentBilanz currentBilanz = new CurrentBilanz();
        currentBilanz.setId("" + currentAccountTransaction.getId());
        currentBilanz.setCreatedBy(currentAccountTransaction.getCreatedBy());
        currentBilanz.setReference(currentAccountTransaction.getReference());
        currentBilanz.setAgent(currentAccountTransaction.getCreatedBy());
        currentBilanz.setInterestRate((currentAccountTransaction.getCurrentAccount()==null)?"0":""+currentAccountTransaction.getCurrentAccount().getInterestRate());
        currentBilanz.setCurrentAmount(currentAccountTransaction.getCurrentAmount());
        currentBilanz.setCreatedDate(BVMicroUtils.formatDateTime(currentAccountTransaction.getCreatedDate()));
        currentBilanz.setNotes(currentAccountTransaction.getNotes());
        currentBilanz.setAccountNumber(currentAccountTransaction.getCurrentAccount().getAccountNumber());
        currentBilanz.setNoOfDays(calculateNoOfDays(currentAccountTransaction.getCreatedDate()));
        currentBilanz.setModeOfPayment(currentAccountTransaction.getModeOfPayment());
        currentBilanz.setAccountOwner(currentAccountTransaction.getAccountOwner());
        currentBilanz.setBranch(currentAccountTransaction.getCurrentAccount().getBranchCode());
        currentBilanz.setRepresentative(currentAccountTransaction.getRepresentative());

        if (calculateInterest) {
            currentBilanz.setInterestAccrued(
                    BVMicroUtils.formatCurrency(
                            interestService.calculateInterestAccruedMonthCompounded(
                                    currentAccountTransaction.getCurrentAccount().getInterestRate(),
                                    currentAccountTransaction.getCreatedDate(),
                                    currentAccountTransaction.getCurrentAmount()),countrCode));
        }
        return currentBilanz;
    }

    private String calculateNoOfDays(LocalDateTime createdDate) {
        long noOfDays = createdDate.until(LocalDateTime.now(), ChronoUnit.DAYS);
        return "" + noOfDays;
    }


    private String padding(int i) {
        if (i < 10)
            return "" + 0 + 1;
        return "" + i;
    }

    public String withdrawalAllowed(CurrentAccountTransaction currentTransaction) {
        String error = "";
        error = minimumSavingRespected(currentTransaction);
        return error;
    }

    private String minimumSavingRespected(CurrentAccountTransaction currentTransaction) {
        double futureBalance = getAccountBalance(currentTransaction.getCurrentAccount()) + currentTransaction.getCurrentAmount();
        if (currentTransaction.getCurrentAccount().getAccountMinBalance() > futureBalance) {
            return "Account will fall below minimum account balance";
        }
        return null;
    }

    public double getAccountBalance(CurrentAccount savingAccount) {
        double total = 0.0;
        List<CurrentAccountTransaction> savingAccountTransactions = savingAccount.getCurrentAccountTransaction();
        for (CurrentAccountTransaction tran : savingAccountTransactions) {
            total = tran.getCurrentAmount() + total;
        }
        return total;
    }

    public void createCurrentAccountTransactionFromLoan(CurrentAccount currentAccount, LoanAccount loanAccount) {
        //Create a initial loan transaction of borrowed amount
        LoanAccountTransaction loanAccountTransaction =
                loanAccountTransactionService.createLoanAccountTransaction(loanAccount);

        currentAccountService.createCurrentAccountTransaction(currentAccount, loanAccountTransaction, BVMicroUtils.CURRENT_LOAN_TRANSFER);//

        // Update new loan account transaction
        loanAccountTransaction.setAmountReceived(loanAccount.getLoanAmount());
        generalLedgerService.updateGLWithCurrentLoanAccountTransaction(loanAccountTransaction);//TODO: NO Accountledger set Amount missing in GL

//      generalLedgerService.updateGLAfterLoanAccountTransferRepayment(loanAccountTransaction);
        loanAccountTransaction.setAmountReceived(0); // Reset loanAmount
        callCenterService.saveCallCenterLog("ACTIVE", getLoggedInUserName(), loanAccount.getAccountNumber(),"LOAN FUNDS TRANSFERRED TO ACCOUNT"); //TODO ADD DATE
        loanAccountService.save(loanAccount);
    }

}
