package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SavingAccountService extends SuperService {


    @Autowired
    private SavingAccountRepository savingAccountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

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
    private BranchService branchService;

    @Autowired
    private CurrentAccountService currentAccountService;

    @Autowired
    private CurrentAccountTransactionRepository currentAccountTransactionRepository;

    @Autowired
    NotificationService notificationService;

    private double minimumSaving;

    public SavingAccount findByAccountNumberAndOrgId(String accountNumber, long orgId) {
        return savingAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public void createSavingAccount(SavingAccount savingAccount, User user) {

        ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
        UserRole customer = userRoleService.findUserRoleByName("ROLE_CUSTOMER", user.getOrgId());
        userRoleList.add(customer);
//        ArrayList<User> customerList = userService.findAllByUserRoleIn(userRoleList, user.getOrgId());

        int countNumberOfProductsInBranch = savingAccountRepository.countNumberOfProductsCreatedInBranch(user.getBranch().getCode(), user.getOrgId());
        savingAccount.setAccountNumber(BVMicroUtils.getCobacSavingsAccountNumber(savingAccount.getCountry(),
                savingAccount.getProductCode(), countNumberOfProductsInBranch, user.getCustomerNumber(), savingAccount.getBranchCode())); //TODO: Collision

        savingAccount.setAccountStatus(AccountStatus.ACTIVE);
        savingAccount.setCreatedBy(getLoggedInUserName());
        savingAccount.setCreatedDate(new Date(System.currentTimeMillis()));
        savingAccount.setLastUpdatedBy(getLoggedInUserName());
        savingAccount.setAccountLocked(false);
        savingAccount.setLastUpdatedDate(new Date(System.currentTimeMillis()));

        AccountType savingAccountType = accountTypeRepository.findByNumberAndOrgIdAndActiveTrue(savingAccount.getProductCode(), user.getOrgId());
        savingAccount.setAccountType(savingAccountType);
        savingAccount.setOrgId(user.getOrgId());
        savingAccount.setUser(user);
        savingAccountRepository.save(savingAccount);
        callCenterService.callCenterSavingAccount(savingAccount);

        user = userRepository.findById(user.getId()).get();
        user.getSavingAccount().add(savingAccount);
        userService.saveUser(user);

    }

    public List<SavingAccount> findByOrgId(long orgId) {
        return savingAccountRepository.findByOrgId(orgId);
    }
//
//    public SavingBilanzList getSavingAccountByUser(User user, boolean calculateInterest) {
//        User aUser = null;
//        if (null != user.getUserName()) {
//            aUser = userRepository.findByUserName(user.getUserName());
//        } else {
//            aUser = userRepository.findById(user.getId()).get();
//        }
//        ArrayList<User> userList = new ArrayList<User>();
//        userList.add(aUser);
//        return calculateUsersInterest(userList, calculateInterest);
//    }

    @Transactional
    public void createSavingAccountTransaction(SavingAccountTransaction savingAccountTransaction, RuntimeSetting runtimeSetting) {
        //Get id of savingAccount transaction
        savingAccountTransaction.setReference(BVMicroUtils.getSaltString()); //Collision
        savingAccountTransaction.setCreatedBy(getLoggedInUserName());
        savingAccountTransaction.setCreatedDate(LocalDateTime.now());
        savingAccountTransaction.setAccountBalance(calculateAccountBalance(savingAccountTransaction.getSavingAmount(), savingAccountTransaction.getSavingAccount()));

        savingAccountTransactionRepository.save(savingAccountTransaction);
        callCenterService.callCenterSavingAccountTransaction(savingAccountTransaction, runtimeSetting);
//        generalLedgerService.updateGLAfterSavingAccountTransaction(savingAccountTransaction);
    }

    public double calculateAccountBalance(double savingAmount, SavingAccount savingAccount) {

        Double balance = 0.0;
        for (SavingAccountTransaction transaction : savingAccount.getSavingAccountTransaction()) {
            balance = transaction.getSavingAmount() + balance;
        }
        return savingAmount + balance;
    }


    @Transactional
    public void createSavingAccountTransaction(SavingAccountTransaction savingAccountTransaction, SavingAccount savingAccount, RuntimeSetting rt) {
        //Get id of savingAccount transactions
        createSavingAccountTransaction(savingAccountTransaction, rt);
        if (savingAccount.getSavingAccountTransaction() != null) {
            savingAccount.getSavingAccountTransaction().add(savingAccountTransaction);
        } else {
            savingAccount.setSavingAccountTransaction(new ArrayList<SavingAccountTransaction>());
            savingAccount.getSavingAccountTransaction().add(savingAccountTransaction);
        }
        save(savingAccount);
    }


    public Optional<SavingAccount> findById(long id) {
        Optional<SavingAccount> savingAccount = savingAccountRepository.findById(id);
        return savingAccount;
    }

    public void save(SavingAccount save) {
        savingAccountRepository.save(save);
    }


    public SavingBilanzList getSavingBilanzByUser(User user, boolean calculateInterest, String countryCode) {
        User aUser = null;
        if (null != user.getUserName()) {
            aUser = userRepository.findByUserName(user.getUserName());
        } else {
            aUser = userRepository.findById(user.getId()).get();
        }
        ArrayList<User> userList = new ArrayList<User>();
        userList.add(aUser);
        return calculateUsersInterest(userList, calculateInterest, countryCode);

    }


//    public SavingBilanzList calculateAccountBilanz(Long accountId, String startDate, String endDate, boolean calculateInterest) {
//        List<SavingAccountTransaction> savingAccountTransactions = savingAccountTransactionRepository.searchStartEndDateAccount(startDate,endDate,accountId);
//        return calculateAccountBilanz(savingAccountTransactions,calculateInterest);
//    }


    public SavingBilanzList calculateAccountBilanz(
            List<SavingAccountTransaction> savingAccountTransactions,
            boolean calculateInterest, String currencyCode) {
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double savingAccountTransactionInterest = 0.0;

        SavingBilanzList savingBilanzsList = new SavingBilanzList();
        for (int k = 0; k < savingAccountTransactions.size(); k++) {

            final SavingAccountTransaction savingAccountTransaction = savingAccountTransactions.get(k);
            SavingBilanz savingBilanz = new SavingBilanz();
            savingBilanz = calculateInterest(savingAccountTransaction, calculateInterest, currencyCode);

            currentSaved = currentSaved + savingAccountTransaction.getSavingAmount();

            savingBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, currencyCode));
            savingBilanzsList.getSavingBilanzList().add(savingBilanz);
            totalSaved = totalSaved + savingAccountTransaction.getSavingAmount();
            if (calculateInterest) {
                savingAccountTransactionInterest = savingAccountTransactionInterest +
                        interestService.calculateInterestAccruedMonthCompounded(
                                savingAccountTransaction.getSavingAccount().getInterestRate(),
                                savingAccountTransaction.getCreatedDate(),
                                savingAccountTransaction.getSavingAmount());
                savingBilanzsList.setTotalSavingInterest(BVMicroUtils.formatCurrency(savingAccountTransactionInterest, currencyCode));
            }
        }
        savingBilanzsList.setTotalSaving(BVMicroUtils.formatCurrency(totalSaved, currencyCode));

        Collections.reverse(savingBilanzsList.getSavingBilanzList());
        return savingBilanzsList;
    }


    public SavingBilanzList calculateAccountBilanzInterval(long id, GLSearchDTO glSearchDTO,
                                                           boolean calculateInterest, long orgId, String currencyCode) {
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double savingAccountTransactionInterest = 0.0;
        List<SavingAccountTransaction> savingAccountTransactions = savingAccountTransactionRepository.searchStartEndDateFilter(glSearchDTO.getStartDate() + " 00:00:00.000", glSearchDTO.getEndDate() + " 23:59:59.999", id, orgId);
        SavingBilanzList savingBilanzsList = new SavingBilanzList();
        for (int k = 0; k < savingAccountTransactions.size(); k++) {

            final SavingAccountTransaction savingAccountTransaction = savingAccountTransactions.get(k);
            SavingBilanz savingBilanz = new SavingBilanz();
            savingBilanz = calculateInterest(savingAccountTransaction, calculateInterest, currencyCode);

            currentSaved = currentSaved + savingAccountTransaction.getSavingAmount();

            savingBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, currencyCode));
            savingBilanzsList.getSavingBilanzList().add(savingBilanz);
            totalSaved = totalSaved + savingAccountTransaction.getSavingAmount();
            if (calculateInterest) {
                savingAccountTransactionInterest = savingAccountTransactionInterest +
                        interestService.calculateInterestAccruedMonthCompounded(
                                savingAccountTransaction.getSavingAccount().getInterestRate(),
                                savingAccountTransaction.getCreatedDate(),
                                savingAccountTransaction.getSavingAmount());
                savingBilanzsList.setTotalSavingInterest(BVMicroUtils.formatCurrency(savingAccountTransactionInterest, currencyCode));
            }
        }
        savingBilanzsList.setTotalSaving(BVMicroUtils.formatCurrency(totalSaved, currencyCode));

//        Collections.reverse(savingBilanzsList.getSavingBilanzList());
        return savingBilanzsList;
    }


    private SavingBilanzList calculateUsersInterest(ArrayList<User> users, boolean calculateInterest, String currencyCode) {
        double totalSaved = 0.0;
        double currentSaved = 0.0;
        double savingAccountTransactionInterest = 0.0;
        SavingBilanzList savingBilanzsList = new SavingBilanzList();
        for (int i = 0; i < users.size(); i++) {
            List<SavingAccount> savingAccounts = users.get(i).getSavingAccount();
            List<SavingAccountTransaction> savingAccountTransactions = new ArrayList<SavingAccountTransaction>();
            for (int j = 0; j < savingAccounts.size(); j++) {
                SavingAccount savingAccount = savingAccounts.get(j);

                boolean defaultedPayments = checkDefaultLogic(savingAccount);
                savingAccount.setDefaultedPayment(defaultedPayments); //TODO:defaultLogic
                savingAccountTransactions = savingAccount.getSavingAccountTransaction();
                double accountTotalSaved = 0.0;
                for (int k = 0; k < savingAccountTransactions.size(); k++) {
                    final SavingAccountTransaction savingAccountTransaction = savingAccountTransactions.get(k);

                    SavingBilanz savingBilanz = calculateInterest(savingAccountTransaction, calculateInterest, currencyCode);
                    savingBilanz.setRepresentative(savingAccountTransaction.getRepresentative());
                    currentSaved = currentSaved + savingAccountTransaction.getSavingAmount();
                    savingBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(currentSaved, currencyCode));
                    savingBilanzsList.getSavingBilanzList().add(savingBilanz);
                    totalSaved = totalSaved + savingAccountTransaction.getSavingAmount();
                    accountTotalSaved = accountTotalSaved + savingAccountTransaction.getSavingAmount();
                    savingAccountTransactionInterest = savingAccountTransactionInterest +
                            interestService.calculateInterestAccruedMonthCompounded(
                                    savingAccountTransaction.getSavingAccount().getInterestRate(),
                                    savingAccountTransaction.getCreatedDate(),
                                    savingAccountTransaction.getSavingAmount());
                }
                savingAccount.setAccountBalance(accountTotalSaved);
                if (!defaultedPayments) {
                    boolean minBalance = checkMinBalanceLogin(savingAccount) ? true : false;
                    savingAccount.setDefaultedPayment(minBalance);// Minimum balance check
                }
                savingAccountRepository.save(savingAccount);
            }
        }

        savingBilanzsList.setTotalSaving(BVMicroUtils.formatCurrency(totalSaved, currencyCode));
        savingBilanzsList.setTotalSavingInterest(BVMicroUtils.formatCurrency(savingAccountTransactionInterest, currencyCode));
//        Collections.reverse(savingBilanzsList.getSavingBilanzList());
        return savingBilanzsList;
    }

    private boolean checkMinBalanceLogin(SavingAccount savingAccount) {

        if (savingAccount.getAccountMinBalance() > savingAccount.getAccountBalance()) {

            callCenterService.saveCallCenterLog("", savingAccount.getUser().getUserName(), savingAccount.getAccountNumber(), BVMicroUtils.MINIMUM_BALANCE_NOT_MET_FOR_THIS_ACCOUNT + savingAccount.getAccountNumber());
            return true;
        }

        return false;
    }


    private SavingBilanz calculateInterest(SavingAccountTransaction savingAccountTransaction, boolean calculateInterest, String countryCode) {
        SavingBilanz savingBilanz = new SavingBilanz();
        savingBilanz.setId("" + savingAccountTransaction.getId());
        savingBilanz.setAccountType(savingAccountTransaction.getSavingAccount().getAccountSavingType().getName());
        savingBilanz.setAccountMinimumBalance(BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAccount().getAccountMinBalance(), countryCode));
        savingBilanz.setMinimumBalance(BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAccount().getAccountMinBalance(), countryCode));
        savingBilanz.setCreatedBy(savingAccountTransaction.getCreatedBy());
        savingBilanz.setReference(savingAccountTransaction.getReference());
        savingBilanz.setAgent(savingAccountTransaction.getCreatedBy());
        savingBilanz.setInterestRate("" + savingAccountTransaction.getSavingAccount().getInterestRate());
        savingBilanz.setSavingAmount(savingAccountTransaction.getSavingAmount());
        savingBilanz.setRepresentative(savingAccountTransaction.getRepresentative());

        savingBilanz.setCreatedDate(BVMicroUtils.formatDateTime(savingAccountTransaction.getCreatedDate()));
        savingBilanz.setNotes(savingAccountTransaction.getNotes());
        savingBilanz.setAccountNumber(savingAccountTransaction.getSavingAccount().getAccountNumber());
        savingBilanz.setNoOfDays(calculateNoOfDays(savingAccountTransaction.getCreatedDate()));
        savingBilanz.setModeOfPayment(savingAccountTransaction.getModeOfPayment());
        savingBilanz.setAccountOwner(savingAccountTransaction.getAccountOwner());
        savingBilanz.setBranch(savingAccountTransaction.getSavingAccount().getBranchCode());

        if (calculateInterest) {
            savingBilanz.setInterestAccrued(
                    BVMicroUtils.formatCurrency(
                            interestService.calculateInterestAccruedMonthCompounded(
                                    savingAccountTransaction.getSavingAccount().getInterestRate(),
                                    savingAccountTransaction.getCreatedDate(),
                                    savingAccountTransaction.getSavingAmount()), countryCode));
        }
        return savingBilanz;
    }

    private String calculateNoOfDays(LocalDateTime createdDate) {
        long noOfDays = createdDate.until(LocalDateTime.now(), ChronoUnit.DAYS);
        return "" + noOfDays;
    }


    public boolean checkDefaultLogic(SavingAccount savingAccount) {

//        if(savingAccount.getAccountSavingType().getName().equals("GENERAL SAVINGS")){
        List<SavingAccountTransaction> savingAccountTransactionList = savingAccount.getSavingAccountTransaction();

        LocalDateTime currentDateCal = LocalDateTime.now();

        Date input = savingAccount.getCreatedDate();
        LocalDate createdLocalDate = input.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();


        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(LocalDate.of(createdLocalDate.getYear(), createdLocalDate.getMonth(), createdLocalDate.getDayOfMonth())),
                YearMonth.from(LocalDate.of(currentDateCal.getYear(), currentDateCal.getMonth(), currentDateCal.getDayOfMonth())));

        if (monthsBetween >= savingAccountTransactionList.size()) {
            CallCenter callCenter = new CallCenter();
            callCenter.setNotes(BVMicroUtils.REGULAR_MONTHLY_PAYMENT_MISSING);
            callCenter.setDate(new Date(System.currentTimeMillis()));
            callCenter.setReference(savingAccount.getUser().getFirstName() + " " + savingAccount.getUser().getLastName());
            callCenter.setAccountNumber(savingAccount.getAccountNumber());
            return true;
        }

        return false;
    }

    private String padding(int i) {
        if (i < 10)
            return "" + 0 + 1;
        return "" + i;
    }

    public String withdrawalAllowed(SavingAccountTransaction savingTransaction) {
        String error = "";
        error = minimumSavingRespected(savingTransaction);
        return error;
    }


    private String minimumSavingRespected(SavingAccountTransaction savingTransaction) {
        double futureBalance = getAccountBalance(savingTransaction.getSavingAccount()) + savingTransaction.getSavingAmount();
        if (savingTransaction.getSavingAccount().getAccountMinBalance() > futureBalance) {
            return "Account will fall below Minimum  account balance";
        }
        return null;
    }

    public double getAccountBalance(SavingAccount savingAccount) {
        double total = 0.0;
        List<SavingAccountTransaction> savingAccountTransactions = savingAccount.getSavingAccountTransaction();
        for (SavingAccountTransaction tran : savingAccountTransactions) {
            total = tran.getSavingAmount() + total;
        }
        return total;
    }

    @Transactional
    public LoanBilanz transferFromCurrentToLoan(CurrentAccount fromCurrentAccount,
                                                LoanAccount loanAccount,
                                                double transferAmount,
                                                String notes) {
        LocalDateTime now = LocalDateTime.now();
        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

        LoanBilanz loanBilanz = new LoanBilanz();

        if (!StringUtils.equals(loanAccount.getAccountStatus().name(), AccountStatus.ACTIVE.name())) {
            loanBilanz.setWarningMessage(BVMicroUtils.LOAN_MUST_BE_IN_ACTIVE_STATE);
            return loanBilanz;
        }

        LoanAccountTransaction loanAccountTransaction = new LoanAccountTransaction();
        loanAccountTransaction.setLoanAccount(loanAccount);

        loanAccountTransaction.setCreatedDate(now);
        loanAccountTransaction.setCreatedBy(loggedInUserName);
        loanAccountTransaction.setNotes(notes);

        loanAccountTransaction.setBranch(branchInfo.getId());
        loanAccountTransaction.setBranchCode(branchInfo.getCode());
        loanAccountTransaction.setBranchCountry(branchInfo.getCountry());
        loanAccountTransaction.setAmountReceived(transferAmount);
//        loanAccountTransaction.setAccountOwner(loanAccount.getUser().getLastName() +", "loanAccount.getUser().getLastName());
        loanAccountTransaction.setReference(BVMicroUtils.getSaltString());
        loanBilanz = loanAccountService.createLoanAccountTransaction(loanAccountTransaction, loanAccount, BVMicroUtils.TRANSFER);

        if ((loanBilanz.getMaximumPayment() == -1) || (loanBilanz.getMinimumPayment() == -1)) {
            return loanBilanz;
        }

        if (loanAccount.getMinimumPayment() == 0) {
            //Update shortee accounts min balance
            if (loanAccount.isBlockBalanceQuarantor()) {
                List<ShorteeAccount> shorteeAccounts = loanAccount.getShorteeAccounts();
                ShorteeAccount shorteeAccount = shorteeAccounts.get(0);
                SavingAccount shorteeSavingAccount = shorteeAccount.getSavingAccount();
                shorteeSavingAccount.setAccountMinBalance(shorteeSavingAccount.getAccountMinBalance() - loanAccountTransaction.getAmountReceived());
                savingAccountRepository.save(shorteeSavingAccount);
            }
            // Current Account update
            CurrentAccountTransaction currentAccountTransaction = getCurrentAccountTransaction(notes, branchInfo, fromCurrentAccount, transferAmount * -1, BVMicroUtils.CURRENT_LOAN_TRANSFER);
            fromCurrentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
            currentAccountRepository.save(fromCurrentAccount);

            generalLedgerService.updateGLAfterLoanAccountTransferRepayment(loanAccountTransaction);
            loanBilanz.setWarningMessage("true");
            return loanBilanz;
        }
        loanBilanz.setWarningMessage("Please, Make Minimum Loan Payment ");
        loanBilanz.setMinimumPayment(loanAccount.getMinimumPayment());
        return loanBilanz;
    }


    @NotNull
    public SavingAccountTransaction getSavingAccountTransaction(String notes, Branch branchInfo, SavingAccount savingAccount, double v, String modeOfPayment, RuntimeSetting rt) {
        SavingAccountTransaction savingAccountTransaction = new SavingAccountTransaction();
        savingAccountTransaction.setNotes(notes);
        savingAccountTransaction.setSavingAccount(savingAccount);
        savingAccountTransaction.setSavingAmount(v);
        savingAccountTransaction.setModeOfPayment(modeOfPayment);
        savingAccountTransaction.setBranch(branchInfo.getId());
        savingAccountTransaction.setBranchCode(branchInfo.getCode());
        savingAccountTransaction.setBranchCountry(branchInfo.getCountry());
        savingAccountTransaction.setOrgId(branchInfo.getOrgId());
        createSavingAccountTransaction(savingAccountTransaction, rt);
        return savingAccountTransaction;
    }

    @NotNull
    public CurrentAccountTransaction getCurrentAccountTransaction(String notes, Branch branchInfo, CurrentAccount currentAccount, double v, String transferMode) {

        CurrentAccountTransaction currentAccountTransaction = new CurrentAccountTransaction();
        currentAccountTransaction.setNotes(notes);
        currentAccountTransaction.setCurrentAccount(currentAccount);

        if (currentAccountTransaction.getAccountBalance() == 0) {
            currentAccountTransaction.setAccountBalance(currentAccountTransaction.getCurrentAccount().getAccountBalance() + v);
        }

        // TODO: Not sure why it returns exception for momo account transfer update Probable after FUTURE CALL
        String loggedInUser = "";
        try {
            loggedInUser = getLoggedInUserName();
        } catch (Exception e) {
            loggedInUser = "";
        }

        currentAccountTransaction.setCurrentAmount(v);
        currentAccountTransaction.setModeOfPayment(transferMode);
        currentAccountTransaction.setBranch(branchInfo.getId());
        currentAccountTransaction.setBranchCode(branchInfo.getCode());
        currentAccountTransaction.setBranchCountry(branchInfo.getCountry());
        currentAccountTransaction.setOrgId(branchInfo.getOrgId());

        currentAccountTransaction.setReference(BVMicroUtils.getSaltString()); //Collision
        currentAccountTransaction.setCreatedBy(loggedInUser);
        currentAccountTransaction.setCreatedDate(LocalDateTime.now());

        currentAccountTransactionRepository.save(currentAccountTransaction);

        return currentAccountTransaction;
    }

    public SavingAccount transferFromDebitToDebit(String fromAccountNumber,
                                                  String toAccountNumber,
                                                  double transferAmount,
                                                  String notes,
                                                  long orgId,
                                                  RuntimeSetting rt) {

        SavingAccount toSavingAccount = findByAccountNumberAndOrgId(toAccountNumber, orgId);
        if (toSavingAccount == null) {
            return null;
        }
        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

        SavingAccountTransaction fromSavingAccountTransaction = getSavingAccount(fromAccountNumber, transferAmount * -1, notes, branchInfo, BVMicroUtils.DEBIT_DEBIT_TRANSFER, orgId, rt);
        fromSavingAccountTransaction.setWithdrawalDeposit(-1);
        SavingAccountTransaction toSavingAccountTransaction = getSavingAccountTransaction(notes, branchInfo, toSavingAccount, transferAmount, BVMicroUtils.DEBIT_DEBIT_TRANSFER, rt);
        toSavingAccount.getSavingAccountTransaction().add(toSavingAccountTransaction);
        savingAccountRepository.save(toSavingAccount);

        generalLedgerService.updateGLAfterDebitDebitTransfer(fromSavingAccountTransaction, toSavingAccountTransaction);

        callCenterService.saveCallCenterLog(fromSavingAccountTransaction.getReference(),
                loggedInUserName, fromSavingAccountTransaction.getSavingAccount().getAccountNumber(),
                "TRANSFER FROM: Saving account transaction made to " + toSavingAccountTransaction.getSavingAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(toSavingAccountTransaction.getSavingAmount(), rt.getCountryCode()));

        callCenterService.saveCallCenterLog(toSavingAccountTransaction.getReference(),
                loggedInUserName, toSavingAccountTransaction.getSavingAccount().getAccountNumber(),
                "TRANSFER TO: Saving account transaction made from " + fromSavingAccountTransaction.getSavingAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(toSavingAccountTransaction.getSavingAmount(), rt.getCountryCode()));

        return fromSavingAccountTransaction.getSavingAccount();
    }

    public SavingAccount transferFromDebitToCurrent(String fromAccountNumber,
                                                    String toAccountNumber,
                                                    double transferAmount,
                                                    String notes,
                                                    long orgId,
                                                    RuntimeSetting rt) {

        CurrentAccount toCurrentAccount = currentAccountService.findByAccountNumberAndOrgId(toAccountNumber, orgId);
        if (toCurrentAccount == null) {
            return null;
        }

        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

        SavingAccountTransaction savingAccountTransaction = getSavingAccount(fromAccountNumber, transferAmount * -1, notes, branchInfo, BVMicroUtils.DEBIT_CURRENT_TRANSFER, orgId, rt);

        CurrentAccountTransaction toCurrentAccountTransaction = getCurrentAccountTransaction(notes, branchInfo, toCurrentAccount, transferAmount, BVMicroUtils.DEBIT_CURRENT_TRANSFER);
        toCurrentAccount.getCurrentAccountTransaction().add(toCurrentAccountTransaction);
        currentAccountRepository.save(toCurrentAccount);

        generalLedgerService.updateGLAfterDebitCurrentTransfer(savingAccountTransaction, toCurrentAccountTransaction);

//        callCenterService.saveCallCenterLog(savingAccountTransaction.getReference(),
//                loggedInUserName, savingAccountTransaction.getSavingAccount().getAccountNumber(),
//                "TRANSFER FROM: Current account transaction made to " + toCurrentAccountTransaction.getCurrentAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()));

        callCenterService.saveCallCenterLog(toCurrentAccountTransaction.getReference(),
                loggedInUserName, toCurrentAccountTransaction.getCurrentAccount().getAccountNumber(),
                "TRANSFER TO: Saving account transaction made from " + savingAccountTransaction.getSavingAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(toCurrentAccountTransaction.getCurrentAmount(), rt.getCountryCode()));

        return savingAccountTransaction.getSavingAccount();
    }

    public CurrentAccount transferFromCurrentToDebit(String fromCurrentAccountNumber,
                                                     String toDebitAccountNumber,
                                                     double transferAmount,
                                                     String notes,
                                                     long orgId, RuntimeSetting rt) {
        CurrentAccount fromCurrentAccount = currentAccountService.findByAccountNumberAndOrgId(fromCurrentAccountNumber, orgId);
        if (fromCurrentAccount == null) {
            return null;
        }
        String loggedInUserName = getLoggedInUserName();
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);
        SavingAccountTransaction savingAccountTransaction = getSavingAccount(toDebitAccountNumber, transferAmount, notes, branchInfo, BVMicroUtils.CURRENT_DEBIT_TRANSFER, orgId, rt);

        CurrentAccountTransaction fromCurrentAccountTransaction = getCurrentAccountTransaction(notes, branchInfo, fromCurrentAccount, -1 * transferAmount, BVMicroUtils.CURRENT_DEBIT_TRANSFER);

        fromCurrentAccount.getCurrentAccountTransaction().add(fromCurrentAccountTransaction);
        currentAccountRepository.save(fromCurrentAccount);

        generalLedgerService.updateGLAfterCurrentDebitTransfer(fromCurrentAccountTransaction, savingAccountTransaction);

        callCenterService.saveCallCenterLog(savingAccountTransaction.getReference(),
                loggedInUserName, savingAccountTransaction.getSavingAccount().getAccountNumber(),
                "TRANSFER FROM: " + rt.getCurrentAccount() + " transaction made to " + fromCurrentAccountTransaction.getCurrentAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()));

        callCenterService.saveCallCenterLog(savingAccountTransaction.getReference(),
                loggedInUserName, fromCurrentAccountTransaction.getCurrentAccount().getAccountNumber(),
                "TRANSFER TO: Saving account transaction made from " + savingAccountTransaction.getSavingAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(fromCurrentAccountTransaction.getCurrentAmount(), rt.getCountryCode()));

        return fromCurrentAccount;
    }

    @Transactional
    public CurrentAccount transferFromCurrentToCurrent(String fromAccountNumber,
                                                       String toAccountNumber,
                                                       double transferAmount,
                                                       String notes,
                                                       long orgId, RuntimeSetting rt) {

        CurrentAccount toCurrentAccount = currentAccountService.findByAccountNumberAndOrgId(toAccountNumber, orgId);
        if (toCurrentAccount == null) {
            return null;
        }
        String loggedInUserName = "";
        try {
            loggedInUserName = getLoggedInUserName();

        } catch (Exception e) {
            loggedInUserName = toCurrentAccount.getUser().getUserName(); //TODO: Not sure why PRINCIPAL - CONTEXT for self service momo transfer is NULL
        }
        User loggedInUser = userRepository.findByUserName(loggedInUserName);
        Branch branchInfo = branchService.getBranchInfo(loggedInUserName);

        CurrentAccountTransaction fromCurrentAccountTransaction = getCurrentAccount(fromAccountNumber, transferAmount, notes, branchInfo, BVMicroUtils.CURRENT_CURRENT_TRANSFER, loggedInUser);

        CurrentAccountTransaction toCurrentAccountTransaction = getCurrentAccountTransaction(notes, branchInfo, toCurrentAccount, transferAmount, BVMicroUtils.CURRENT_CURRENT_TRANSFER);
        toCurrentAccount.getCurrentAccountTransaction().add(toCurrentAccountTransaction);
        currentAccountRepository.save(toCurrentAccount);

        generalLedgerService.updateGLAfterCurrentCurrentTransfer(toCurrentAccountTransaction);

        callCenterService.saveCallCenterLog(toCurrentAccountTransaction.getReference(),
                loggedInUserName, toCurrentAccountTransaction.getCurrentAccount().getAccountNumber(),
                "TRANSFER TO: " + rt.getCurrentAccount() + " transaction made from " + fromCurrentAccountTransaction.getCurrentAccount().getAccountNumber() + " " + BVMicroUtils.formatCurrency(toCurrentAccountTransaction.getCurrentAmount(), rt.getCountryCode()));

        User fromUser = fromCurrentAccountTransaction.getCurrentAccount().getUser();
        if (fromUser.isReceiveEmailNotifications() && fromUser.getEmail() != null) { // if sender subscribed for notifications
            notificationService.notifySender(transferAmount, rt, toCurrentAccount.getUser().getFirstName(), fromUser, fromCurrentAccountTransaction.getReference(), rt.getCurrentAccount());
        }

        if (toCurrentAccount.getUser().isReceiveEmailNotifications() && toCurrentAccount.getUser().getEmail() != null) { // if receiver subscribed for notifications
            notificationService.notifyReceiver(transferAmount, rt, toCurrentAccount.getUser(), BVMicroUtils.maskAccountNumber(toCurrentAccount.getAccountNumber()), fromCurrentAccountTransaction.getReference(), rt.getCurrentAccount());
        }

        return toCurrentAccount;
    }

    public BigDecimal calculatePlatformFee(String amount, double fee) {
        BigDecimal finalAmount = new BigDecimal(amount);
        BigDecimal percentageRate = new BigDecimal(fee);
        return percentageRate.divide(new BigDecimal("100"), 2, RoundingMode.CEILING).multiply(finalAmount).setScale(0, RoundingMode.CEILING);
    }

//    @NotNull
//    private SavingAccountTransaction getToSavingAccount(String fromAccountNumber, double transferAmount, String notes, Branch branchInfo, String transferMode) {
//        SavingAccount savingAccount = findByAccountNumber(fromAccountNumber);
//        SavingAccountTransaction savingAccountTransaction = getSavingAccountTransaction(notes, branchInfo, savingAccount, transferAmount, transferMode);
//        savingAccount.getSavingAccountTransaction().add(savingAccountTransaction);
//        savingAccountRepository.save(savingAccount);
//        return savingAccountTransaction;
//    }

    @NotNull
    private SavingAccountTransaction getSavingAccount(String fromAccountNumber, double transferAmount, String notes, Branch branchInfo, String transferMode, long orgId, RuntimeSetting rt) {
        SavingAccount savingAccount = findByAccountNumberAndOrgId(fromAccountNumber, orgId);
        SavingAccountTransaction savingAccountTransaction = getSavingAccountTransaction(notes, branchInfo, savingAccount, transferAmount, transferMode, rt);
        savingAccount.getSavingAccountTransaction().add(savingAccountTransaction);
        savingAccountRepository.save(savingAccount);
        return savingAccountTransaction;
    }

    @NotNull
    private CurrentAccountTransaction getCurrentAccount(String fromAccountNumber, double transferAmount, String notes, Branch branchInfo, String transactionType, User loggedInUser) {
        CurrentAccount currentAccount = currentAccountService.findByAccountNumberAndOrgId(fromAccountNumber, loggedInUser.getOrgId());
        CurrentAccountTransaction currentAccountTransaction = getCurrentAccountTransaction(notes, branchInfo, currentAccount, transferAmount * -1, transactionType);
        currentAccountTransaction.setCreatedBy(loggedInUser.getUserName());
        currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        currentAccountRepository.save(currentAccount);
        return currentAccountTransaction;
    }

}
