package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.LoanBilanz;
import com.bitsvalley.micro.webdomain.LoanBilanzList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LoanAccountService extends SuperService {

    @Autowired
    private SavingAccountRepository savingAccountRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private LoanAccountTransactionService loanAccountTransactionService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    @Autowired
    private InterestService interestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CallCenterService callCenterService;

    @Autowired
    private CallCenterRepository callCenterRepository;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Autowired
    private LoanAccountTransactionRepository loanAccountTransactionRepository;

    @Autowired
    private SavingAccountService savingAccountService;

    @Autowired
    private RuntimePropertiesRepository runtimePropertiesRepository;

    @Autowired
    private ShorteeAccountRepository shorteeAccountRepository;

    @Autowired
    BranchService branchService;

    public LoanAccount findByAccountNumberAndOrgId(String accountNumber,long orgId) {
        return loanAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    @NotNull
    @Transactional
    public LoanAccount createLoanAccount(User user, LoanAccount loanAccount,
                                         SavingAccount savingAccountGuarantor, SavingAccount savingAccountGuarantor2, SavingAccount savingAccountGuarantor3, String countryCode) {
        Date createdDate = new Date();
        String loggedInUserName = getLoggedInUserName();
        ShorteeAccount shorteeAccount = new ShorteeAccount();
        ArrayList<ShorteeAccount> listShorteeAccount = new ArrayList<ShorteeAccount>();

        if(savingAccountGuarantor != null && savingAccountGuarantor.getAccountNumber()!= null){
            SavingAccount shorteeSavingAccount = savingAccountService.findByAccountNumberAndOrgId(
                    savingAccountGuarantor.getAccountNumber(), loanAccount.getOrgId());

            shorteeAccount.setSavingAccount(shorteeSavingAccount);
            if( loanAccount.isBlockBalanceQuarantor()){
                if(loanAccount.getGuarantor1Amount1() > 0 ){
                    shorteeSavingAccount.setAccountMinBalance(
                            shorteeSavingAccount.getAccountMinBalance() + loanAccount.getGuarantor1Amount1());
                    shorteeSavingAccount.setLastUpdatedDate(createdDate);
                    shorteeSavingAccount.setLastUpdatedBy(loggedInUserName);
                    shorteeSavingAccount.setAccountStatus(AccountStatus.SHORTEE_ACCOUNT);
                    shorteeSavingAccount.setAccountLocked(true);
                    savingAccountService.save(shorteeSavingAccount);
                }
            }
            shorteeAccount.setAmountShortee(loanAccount.getGuarantor1Amount1());

            shorteeAccount.setCreatedDate(createdDate);
            shorteeAccount.setLastUpdatedDate(createdDate);

            shorteeAccount.setCreatedBy(loggedInUserName);
            shorteeAccount.setLastUpdatedBy(loggedInUserName);
            shorteeAccount.setOrgId(user.getOrgId());
            shorteeAccountRepository.save(shorteeAccount);
            listShorteeAccount.add(shorteeAccount);
            callCenterService.callCenterShorteeUpdate(shorteeSavingAccount, loanAccount.getGuarantor1Amount1(), countryCode );
        }

        if(savingAccountGuarantor2 != null && savingAccountGuarantor2.getAccountNumber()!= null){
            SavingAccount shorteeSavingAccount = savingAccountService.findByAccountNumberAndOrgId(
                    savingAccountGuarantor2.getAccountNumber(),loanAccount.getOrgId());

            shorteeAccount.setSavingAccount(shorteeSavingAccount);
            if( loanAccount.isBlockBalanceQuarantor()){
                if(loanAccount.getGuarantor1Amount1() > 0 ){
                    shorteeSavingAccount.setAccountMinBalance(
                            shorteeSavingAccount.getAccountMinBalance() + loanAccount.getGuarantor1Amount1());
                    shorteeSavingAccount.setLastUpdatedDate(createdDate);
                    shorteeSavingAccount.setLastUpdatedBy(loggedInUserName);
                    shorteeSavingAccount.setAccountStatus(AccountStatus.SHORTEE_ACCOUNT);
                    shorteeSavingAccount.setAccountLocked(true);
                    shorteeSavingAccount.setOrgId(user.getOrgId());
                    savingAccountService.save(shorteeSavingAccount);
                }
            }
            shorteeAccount.setAmountShortee(loanAccount.getGuarantor1Amount2());

            shorteeAccount.setCreatedDate(createdDate);
            shorteeAccount.setLastUpdatedDate(createdDate);

            shorteeAccount.setCreatedBy(loggedInUserName);
            shorteeAccount.setLastUpdatedBy(loggedInUserName);
            shorteeAccountRepository.save(shorteeAccount);
            listShorteeAccount.add(shorteeAccount);
            callCenterService.callCenterShorteeUpdate(shorteeSavingAccount, loanAccount.getGuarantor1Amount2(), countryCode);
        }

        if(savingAccountGuarantor3 != null && savingAccountGuarantor3.getAccountNumber()!= null){
            SavingAccount shorteeSavingAccount = savingAccountService.findByAccountNumberAndOrgId(
                    savingAccountGuarantor3.getAccountNumber(), loanAccount.getOrgId());

            shorteeAccount.setSavingAccount(shorteeSavingAccount);
            if( loanAccount.isBlockBalanceQuarantor()){
                if(loanAccount.getGuarantor1Amount3() > 0 ){
                    shorteeSavingAccount.setAccountMinBalance(
                            shorteeSavingAccount.getAccountMinBalance() + loanAccount.getGuarantor1Amount3());
                    shorteeSavingAccount.setLastUpdatedDate(createdDate);
                    shorteeSavingAccount.setLastUpdatedBy(loggedInUserName);
                    shorteeSavingAccount.setAccountStatus(AccountStatus.SHORTEE_ACCOUNT);
                    shorteeSavingAccount.setAccountLocked(true);
                    savingAccountService.save(shorteeSavingAccount);
                }
            }
            shorteeAccount.setAmountShortee(loanAccount.getGuarantor1Amount3());

            shorteeAccount.setCreatedDate(createdDate);
            shorteeAccount.setLastUpdatedDate(createdDate);

            shorteeAccount.setCreatedBy(loggedInUserName);
            shorteeAccount.setLastUpdatedBy(loggedInUserName);
            shorteeAccountRepository.save(shorteeAccount);
            listShorteeAccount.add(shorteeAccount);
            callCenterService.callCenterShorteeUpdate(shorteeSavingAccount, loanAccount.getGuarantor1Amount3(), countryCode);
        }

        loanAccount.setShorteeAccounts(listShorteeAccount);

        double payment = interestService.monthlyPaymentAmortisedPrincipal(loanAccount.getInterestRate(),
                loanAccount.getTermOfLoan(), loanAccount.getLoanAmount());
        loanAccount.setMonthlyPayment(payment);

        if(createLoanAccount(loanAccount, user)) {
            return loanAccount;
        }else{
            return null;
        }
    }


    @Transactional
    public boolean createLoanAccount(LoanAccount loanAccount, User user) {

        String loggedInUserName = getLoggedInUserName();
        User aUser = userRepository.findByUserName(loggedInUserName);
        loanAccount.setCreatedBy(loggedInUserName);
        loanAccount.setLastUpdatedBy(loggedInUserName);

        loanAccount.setCountry(aUser.getBranch().getCountry());
        loanAccount.setBranchCode(aUser.getBranch().getCode());
        loanAccount.setCurrentLoanAmount(loanAccount.getLoanAmount());

        ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
        UserRole customer = userRoleService.findUserRoleByName("ROLE_CUSTOMER", aUser.getOrgId());
        userRoleList.add(customer);
//        ArrayList<User> customerList = userService.findAllByUserRoleIn(userRoleList);

        int countNumberOfProductsInBranch = loanAccountRepository.countNumberOfProductsCreatedInBranch(user.getBranch().getCode(),user.getOrgId());
        loanAccount.setAccountNumber(BVMicroUtils.getCobacSavingsAccountNumber(loanAccount.getCountry(),
                loanAccount.getProductCode(),countNumberOfProductsInBranch, user.getCustomerNumber(), loanAccount.getBranchCode())); //TODO: Collision

        LoanAccount byAccountNumberAndOrgId = loanAccountRepository.findByAccountNumberAndOrgId(loanAccount.getAccountNumber(), user.getOrgId());
        if(byAccountNumberAndOrgId != null){
            return false;
        }

        loanAccount.setAccountStatus(AccountStatus.PENDING_APPROVAL);

        Date dateNow = new Date(System.currentTimeMillis());
        loanAccount.setCreatedDate(dateNow);
        loanAccount.setApprovedDate(new Date(0));
        loanAccount.setApprovedBy( AccountStatus.PENDING_APPROVAL.name() );
        loanAccount.setLastUpdatedDate(dateNow);
        loanAccount.setLastPaymentDate(dateNow);

        AccountType accountType = accountTypeRepository.findByNumberAndOrgIdAndActiveTrue(loanAccount.getProductCode(), user.getOrgId());
        loanAccount.setAccountType(accountType);

        user = userRepository.findById(user.getId()).get();
        loanAccount.setUser(user);
        loanAccount.setOrgId(user.getOrgId());


            loanAccountRepository.save(loanAccount);


        user.getLoanAccount().add(loanAccount);
        userService.saveUser(user);

        //Trace
        callCenterService.saveCallCenterLog("",
                user.getUserName(), loanAccount.getAccountType().getName(), loanAccount.getLoanAmount() + " "+loanAccount.getAccountNumber());
        return true;
    }

    @Transactional
    public LoanBilanz createLoanAccountTransaction(LoanAccountTransaction loanAccountTransaction, LoanAccount aLoanAccount, String modeOfPayment) {

        LoanBilanz loanBilanz = updateInterestOwedPayment(aLoanAccount, loanAccountTransaction);
        // ro
        if( loanBilanz.getMinimumPayment() != -1 && loanBilanz.getMaximumPayment() != -1 ){
            save(aLoanAccount);
        }else{
            return loanBilanz;
        }

        loanAccountTransaction.setModeOfPayment(modeOfPayment);
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
        loanAccountTransaction.setOrgId(aLoanAccount.getUser().getOrgId());
        loanAccountTransaction.setBranch(branchInfo.getId());
        loanAccountTransaction.setBranchCode(branchInfo.getCode());
        loanAccountTransaction.setBranchCountry(branchInfo.getCountry());

//        loanAccountTransaction.setLoanAccount(aLoanAccount);
        if (aLoanAccount.getLoanAccountTransaction() != null) {
            aLoanAccount.getLoanAccountTransaction().add(loanAccountTransaction);
        } else {
            aLoanAccount.setLoanAccountTransaction(new ArrayList<LoanAccountTransaction>());
            aLoanAccount.getLoanAccountTransaction().add(loanAccountTransaction);
        }

        // move this to return call Common place
        if(!StringUtils.equals(loanAccountTransaction.getModeOfPayment(), BVMicroUtils.TRANSFER)){
            generalLedgerService.updateGLAfterLoanAccountCASHRepayment(loanAccountTransaction);
        }
        callCenterService.saveCallCenterLog(loanAccountTransaction.getReference(), aLoanAccount.getUser().getUserName(), aLoanAccount.getAccountNumber(),
                "Loan account Payment received Amount: "+ BVMicroUtils.formatCurrency(loanAccountTransaction.getAmountReceived()));

        return loanBilanz;
    }

    public LoanBilanzList getLoanBilanzByUser(User user, boolean calculateInterest, String countryCode) {
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


    public Optional<LoanAccount> findById(long id) {
        Optional<LoanAccount> loanAccount = loanAccountRepository.findById(id);
        return loanAccount;
    }

    public void save(LoanAccount save) {
        loanAccountRepository.save(save);
    }


    public LoanBilanz updateInterestOwedPayment(LoanAccount loanAccount, LoanAccountTransaction loanAccountTransaction) {
        LocalDateTime transactionDate = loanAccountTransaction.getCreatedDate();
        LoanBilanz loanBilanz = new LoanBilanz();
        LocalDateTime date =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(loanAccount.getLastPaymentDate().getTime()), ZoneId.systemDefault());
        long days = date.until(transactionDate, ChronoUnit.DAYS);
        double interestOwed = loanAccount.getCurrentLoanAmount() * days * (loanAccount.getInterestRate() * .01 / 365);
        double interestOwedWithoutVAT = loanAccount.getCurrentLoanAmount() * days * (loanAccount.getInterestRateTTC() * .01 / 365);
        double maxPayableAmount = loanAccount.getCurrentLoanAmount() + interestOwed;

        if( loanAccountTransaction.getAmountReceived() > maxPayableAmount){
            loanBilanz.setWarningAmount(maxPayableAmount);

            loanBilanz.setWarningMessage("Please, Make Maximum ("+maxPayableAmount+") Loan Payment of: ");

            loanBilanz.setMaximumPayment(-1);
            return loanBilanz;
        }
        if( loanAccountTransaction.getAmountReceived() < interestOwed ){
            //TODO: Negative scenario if amt paid is less than interest accrued
            loanAccount.setMinimumPayment(interestOwed);
            loanBilanz.setWarningAmount(interestOwed);
            loanBilanz.setWarningMessage("Please, Make Minimum ("+interestOwed+") Loan Payment of: ");
            loanBilanz.setMinimumPayment(-1);
            return loanBilanz;
        }
        if ( interestOwed <= loanAccountTransaction.getAmountReceived()) {

            loanAccountTransaction.setInterestPaid(interestOwedWithoutVAT);
            loanAccountTransaction.setVatPercent( loanAccount.getVatRate()*interestOwedWithoutVAT );

                loanAccountTransaction.setCurrentLoanAmount(
                        loanAccount.getCurrentLoanAmount() - (loanAccountTransaction.getAmountReceived() - interestOwed));

            loanAccount.setCurrentLoanAmount(loanAccountTransaction.getCurrentLoanAmount());
            loanAccount.setTotalInterestOnLoan(loanAccount.getTotalInterestOnLoan() + interestOwed);
            loanAccount.setInterestOwed(interestOwed);
            Date lastPaymentDate = new Date();
            loanAccount.setLastPaymentDate(lastPaymentDate);
            loanAccount.setLastUpdatedDate(lastPaymentDate);
            loanAccount.setMinimumPayment(0);
        }

        loanAccountTransaction.setAccountBalance(loanAccountTransaction.getCurrentLoanAmount());
        loanAccountTransactionRepository.save(loanAccountTransaction);
        return loanBilanz;
    }


    public LoanBilanzList calculateAccountBilanz(
            List<LoanAccountTransaction> loanAccountTransactions,
            boolean calculateInterest, String countryCode) {
        double totalLoan = 0.0;
        String currentLoanBalance = "0";
        double totalLoanAccountTransactionInterest = 0.0;
        double totalLoanAccountTransactionInterestDue = 0.0;

        LoanBilanzList loanBilanzsList = new LoanBilanzList();
        LoanBilanz loanBilanz = null;
        for (int k = 0; k < loanAccountTransactions.size(); k++) {
            final LoanAccountTransaction loanAccountTransaction = loanAccountTransactions.get(k);

            loanBilanz = calculateInterest(loanAccountTransaction, calculateInterest, countryCode );
            currentLoanBalance = loanBilanz.getCurrentBalance();

            totalLoan = totalLoan + loanAccountTransaction.getLoanAmount();
            if (calculateInterest) {
                totalLoanAccountTransactionInterest = totalLoanAccountTransactionInterest + loanAccountTransaction.getInterestPaid();
            }
            loanBilanzsList.getLoanBilanzList().add(loanBilanz);
        }

        loanBilanzsList.setTotalLoanInterest(BVMicroUtils.formatCurrency(totalLoanAccountTransactionInterest, countryCode)); //TODO set total interest
        loanBilanzsList.setTotalLoan(BVMicroUtils.formatCurrency(totalLoan, countryCode));
        loanBilanzsList.setCurrentLoanBalance(currentLoanBalance);

        return loanBilanzsList;
    }


    private LoanBilanzList calculateUsersInterest(ArrayList<User> users, boolean calculateInterest, String countryCode) {
//        double totalCurrentLoan = 0.0;
        double totalLoanAmount = 0.0;
        double loanAccountTransactionInterest = 0.0;
        LoanBilanzList loanBilanzsList = new LoanBilanzList();
        double currentLoanBalanceAllUserLoans = 0.0;
        for (int i = 0; i < users.size(); i++) {
            List<LoanAccount> loanAccounts = userRepository.findById(users.get(i).getId()).get().getLoanAccount();

            double currentLoanBalanceUserLoans = 0.0;
            List<LoanAccountTransaction> loanAccountTransactions = new ArrayList<LoanAccountTransaction>();
            for (int j = 0; j < loanAccounts.size(); j++) {
                LoanAccount loanAccount = loanAccounts.get(j);

                if(loanAccount.getAccountStatus().equals(AccountStatus.REJECTED))
                    continue;

                boolean defaultedPayments = checkDefaultLogic(loanAccount);
                loanAccount.setDefaultedPayment(defaultedPayments); //TODO:defaultLogic
                loanAccountTransactions = loanAccount.getLoanAccountTransaction();
                double currentLoanBalance = 0.0;
                if(loanAccountTransactions.size() == 0){
                    LoanBilanz loanBilanz = new LoanBilanz();
                    loanBilanz.setAccountNumber(loanAccount.getAccountNumber());
                    loanBilanz.setLoanId(loanAccount.getId()+"");
                    loanBilanz.setAgent(loanAccount.getCreatedBy());
                    loanBilanz.setInitialLoanAmount(BVMicroUtils.formatCurrency(loanAccount.getLoanAmount(),countryCode ));
                    loanBilanz.setCurrentBalance("0");
//                loanBilanz.setLoanAmount("0");
                    loanBilanz.setInterestDue("0");
                    loanBilanz.setCreatedDate(BVMicroUtils.formatDate(loanAccount.getCreatedDate()));
                    loanBilanz.setAccountType(loanAccount.getAccountType().getName());
                    loanBilanz.setInterestRate(loanAccount.getInterestRate()+"");
                    loanBilanz.setNotes(loanAccount.getNotes());
                    loanBilanz.setNoOfDays("0");
                    loanBilanzsList.getLoanBilanzList().add(loanBilanz);
                }else{

                    LoanBilanz loanBilanz = new LoanBilanz();
                    //loan account recent statement
//                for (int k = 0; k < loanAccountTransactions.size(); k++) {
                    final LoanAccountTransaction loanAccountTransaction = loanAccountTransactions.get(loanAccountTransactions.size()-1);

                    loanBilanz = calculateInterest(loanAccountTransaction, calculateInterest, countryCode);
                    totalLoanAmount = totalLoanAmount + loanAccountTransaction.getLoanAmount();

                    currentLoanBalance = loanAccountTransaction.getCurrentLoanAmount();
//                }
                    loanBilanzsList.getLoanBilanzList().add(loanBilanz);
                }

                currentLoanBalanceUserLoans = currentLoanBalanceUserLoans + currentLoanBalance;
                loanAccount.setCurrentLoanAmount(currentLoanBalance);
                loanAccountRepository.save(loanAccount);
            }

            currentLoanBalanceAllUserLoans = currentLoanBalanceAllUserLoans + currentLoanBalanceUserLoans;

            loanBilanzsList.setCurrentLoanBalance(BVMicroUtils.formatCurrency(currentLoanBalanceUserLoans, countryCode));
        }
        loanBilanzsList.setTotalLoan(BVMicroUtils.formatCurrency(totalLoanAmount, countryCode));
        loanBilanzsList.setTotalLoanInterest(BVMicroUtils.formatCurrency(loanAccountTransactionInterest, countryCode));
        return loanBilanzsList;
    }


    private boolean checkMinBalanceLogin(double currentSaved, LoanAccount savingAccount) {

//        if(savingAccount.getAccountMinBalance() > currentSaved){
//            CallCenter callCenter = new CallCenter();
//            callCenter.setDate(new Date(System.currentTimeMillis()));
//            callCenter.setNotes("Minimum Balance not met for this account");
//            callCenter.setAccountHolderName(savingAccount.getUser().getFirstName() + " " + savingAccount.getUser().getLastName());
//            callCenter.setAccountNumber(savingAccount.getAccountNumber());
//            callCenterRepository.save(callCenter);
//            return true;
//        }

        return false;
    }


    private LoanBilanz calculateInterest(LoanAccountTransaction loanAccountTransaction, boolean calculateInterest, String countryCode) {
        LoanBilanz loanBilanz = new LoanBilanz();
        loanBilanz.setId("" + loanAccountTransaction.getId());
        loanBilanz.setLoanId("" + loanAccountTransaction.getLoanAccount().getId());
        LoanAccount loanAccount = loanAccountTransaction.getLoanAccount();
        loanBilanz.setAccountType(loanAccount.getAccountType().getName());
        loanBilanz.setCreatedBy(loanAccountTransaction.getCreatedBy());
        loanBilanz.setReference(loanAccountTransaction.getReference());
        loanBilanz.setInitialLoanAmount(BVMicroUtils.formatCurrency(loanAccountTransaction.getLoanAccount().getLoanAmount(), countryCode));
        loanBilanz.setAgent(loanAccountTransaction.getCreatedBy());
        loanBilanz.setInterestRate("" + loanAccount.getInterestRate());
        loanBilanz.setLoanAmount(BVMicroUtils.formatCurrency(loanAccountTransaction.getLoanAmount(), countryCode));
        loanBilanz.setCreatedDate(BVMicroUtils.formatDateTime(loanAccountTransaction.getCreatedDate()));
        loanBilanz.setNotes(loanAccountTransaction.getNotes());
        loanBilanz.setAccountNumber(loanAccount.getAccountNumber());
        loanBilanz.setNoOfDays(calculateNoOfDays(loanAccountTransaction.getCreatedDate()));
        loanBilanz.setRepresentative(loanAccountTransaction.getRepresentative());
//
        int days = new Integer(loanBilanz.getNoOfDays());
        double intRate = new Double(loanBilanz.getInterestRate());
        double v = (loanAccount.getCurrentLoanAmount() * days * intRate * 0.01)/365;

        loanBilanz.setInterestDue(BVMicroUtils.formatCurrency(v, countryCode));

        loanBilanz.setModeOfPayment(loanAccountTransaction.getModeOfPayment());
        loanBilanz.setAccountOwner(loanAccountTransaction.getAccountOwner());
        loanBilanz.setBranch(loanAccount.getBranchCode());
        loanBilanz.setMonthYearOfLastPayment(calculateMonthOfLastPayment(loanAccount.getCreatedDate(),
                loanAccount.getTermOfLoan()));
        loanBilanz.setInterestAccrued(BVMicroUtils.formatCurrency(loanAccountTransaction.getInterestPaid(), countryCode));
        if(loanAccount.isVatOption()){
            loanBilanz.setVatPercent(BVMicroUtils.formatCurrency(loanAccountTransaction.getVatPercent(), countryCode));// Configure
        }else{
            loanBilanz.setVatPercent(BVMicroUtils.formatCurrency(0, countryCode));// Configure
        }

        loanBilanz.setCurrentBalance(BVMicroUtils.formatCurrency(loanAccountTransaction.getCurrentLoanAmount(), countryCode));
        loanBilanz.setAmountReceived(BVMicroUtils.formatCurrency(loanAccountTransaction.getAmountReceived(), countryCode));
        loanBilanz.setMonthlyPayment(loanAccount.getMonthlyPayment() + "");
        return loanBilanz;
    }

    private String calculateMonthOfLastPayment(Date createdDate, int termOfLoan) {
        LocalDate localDate = LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(createdDate));
        localDate = localDate.plusMonths(termOfLoan);
        return localDate.getMonthValue() + "/" + localDate.getYear();
    }

    private String calculateNoOfDays(LocalDateTime createdDate) {
        long noOfDays = createdDate.until(LocalDateTime.now(), ChronoUnit.DAYS);
        return "" + noOfDays;
    }

    private double getNumberOfMonths(LocalDateTime cretedDateInput) {
        double noOfMonths = 0.0;
        Duration diff = Duration.between(cretedDateInput, LocalDateTime.now());
        noOfMonths = diff.toDays() / 30;
        return Math.floor(noOfMonths);
    }

    public boolean checkDefaultLogic(LoanAccount savingAccount) {

//        if(savingAccount.getAccountSavingType().getName().equals("GENERAL SAVINGS")){
//            List<SavingAccountTransaction> savingAccountTransactionList = savingAccount.getSavingAccountTransaction();
//
//            Date createdDate = savingAccount.getCreatedDate();
//            Date currentDate = new Date(System.currentTimeMillis());
//
//            Calendar currentDateCal = GregorianCalendar.getInstance();
//            currentDateCal.setTime(currentDate);
//
//            Calendar createdCalenderCal = GregorianCalendar.getInstance();
//            createdCalenderCal.setTime(createdDate);
//
//            long monthsBetween = ChronoUnit.MONTHS.between(
//                    YearMonth.from(LocalDate.parse(createdCalenderCal.get(GregorianCalendar.YEAR)+"-"+padding(createdCalenderCal.get(GregorianCalendar.MONTH))+"-"+padding(createdCalenderCal.get(GregorianCalendar.DAY_OF_MONTH)))),
//                    YearMonth.from(LocalDate.parse(currentDateCal.get(GregorianCalendar.YEAR)+"-"+padding(currentDateCal.get(GregorianCalendar.MONTH))+"-"+padding(currentDateCal.get(GregorianCalendar.DAY_OF_MONTH)))));
//
//            if (monthsBetween >= savingAccountTransactionList.size()){
//                CallCenter callCenter = new CallCenter();
//                callCenter.setNotes(BVMicroUtils.REGULAR_MONTHLY_PAYMENT_MISSING);
//                callCenter.setDate(new Date(System.currentTimeMillis()));
//                callCenter.setAccountHolderName(savingAccount.getUser().getFirstName() + " "+ savingAccount.getUser().getLastName());
//                callCenter.setAccountNumber(savingAccount.getAccountNumber());
//                callCenterRepository.save(callCenter);
//                return true;
//            }
//
//        }
        return false;
    }

    private String padding(int i) {
        if (i < 10)
            return "" + 0 + 1;
        return "" + i;
    }

    public List<LoanAccount> findLoansPendingAction(long orgId) {
        List<LoanAccount> byStatusNotActive = loanAccountRepository.findByStatusNotActiveAndOrgId(orgId);
        return byStatusNotActive;
    }

    public List<LoanAccount> findLoansActive(long orgId) {
        List<LoanAccount> byStatusNotActive = loanAccountRepository.findByStatusActiveAndOrgId(orgId);
        return byStatusNotActive;
    }

}
