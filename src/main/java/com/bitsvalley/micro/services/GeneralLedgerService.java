package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.GeneralLedgerType;
import com.bitsvalley.micro.webdomain.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Service
@Slf4j
public class GeneralLedgerService extends SuperService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private GeneralLedgerRepository generalLedgerRepository;

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private LoanAccountTransactionRepository loanAccountTransactionRepository;

    @Autowired
    private SavingAccountTransactionRepository savingAccountTransactionRepository;

    @Autowired
    private DailySavingAccountTransactionRepository dailySavingAccountTransactionRepository;

    @Autowired
    private ShareAccountTransactionRepository shareAccountTransactionRepository;

    @Autowired
    private SavingAccountRepository savingAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private CurrentAccountTransactionRepository currentAccountTransactionRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private CallCenterService callCenterService;

    @Autowired
    LoanAccountService loanAccountService;

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    SavingAccountService savingAccountService;

    public List<GeneralLedger> findByAccountNumber(String accountNumber, long orgId) {
        return generalLedgerRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
    }

    public GeneralLedgerBilanz findByReference(String reference, long orgId) {

        List<GeneralLedgerWeb> generalLedgerWebs = mapperGeneralLedger(generalLedgerRepository.findByReferenceAndOrgId(reference, orgId));
        GeneralLedgerBilanz generalLedgerBilanz = getGeneralLedgerBilanz(generalLedgerWebs);
        return generalLedgerBilanz;

    }


    public void updateGLWithCurrentLoanAccountTransaction(LoanAccountTransaction loanAccountTransaction) {

        LedgerAccount ledgerAccount = determineLedgerAccount(loanAccountTransaction.getLoanAccount().getAccountType(), loanAccountTransaction.getOrgId());

        updateGeneralLedger(loanAccountTransaction, ledgerAccount.getCode(), BVMicroUtils.DEBIT, loanAccountTransaction.getLoanAmount() * -1, true);
        updateGeneralLedger(loanAccountTransaction, BVMicroUtils.CURRENT, BVMicroUtils.CREDIT, loanAccountTransaction.getLoanAmount(), true);
    }


    public void updateGLAfterSharePurchaseFromCurrent(ShareAccountTransaction shareAccountTransaction, String currentAccount) {
        shareAccountTransaction.setNotes("Purchase of shares from " + currentAccount);

        if (shareAccountTransaction.getShareAccount().getAccountType().equals(BVMicroUtils.PREFERENCE_SHARE_TYPE)) {
            updateGeneralLedger(shareAccountTransaction, BVMicroUtils.PREFERENCE_SHARE_GL_5005, BVMicroUtils.CREDIT, shareAccountTransaction.getShareAmount(), 5, true);
        } else {
            updateGeneralLedger(shareAccountTransaction, BVMicroUtils.SHARE_GL_5004, BVMicroUtils.CREDIT, shareAccountTransaction.getShareAmount(), 5, true);
        }
        shareAccountTransaction.setNotes("For purchase of shares ");
        updateGeneralLedger(shareAccountTransaction, BVMicroUtils.CURRENT_GL_3004, BVMicroUtils.DEBIT, shareAccountTransaction.getShareAmount() * -1, 3, true);
    }


    public void updateGLAfterLoanAccountCASHRepayment(LoanAccountTransaction loanAccountTransaction) {
//      GeneralLedger generalLedger = null;

        //DEBIT CASH RECEIVED
        updateGeneralLedger(loanAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.DEBIT, loanAccountTransaction.getAmountReceived() * -1, true);

//        some notes here
        LedgerAccount ledgerAccount = determineLedgerAccount(loanAccountTransaction.getLoanAccount().getAccountType(), loanAccountTransaction.getOrgId());
//        updateGeneralLedger(loanAccountTransaction, BVMicroUtils.LOAN_INTEREST, BVMicroUtils.CREDIT, loanAccountTransaction.getInterestPaid(), true);

        //derive interest account from ledgerAccount
        String loanLedgerInterestAccountName = ledgerAccount.getName() + "_INTEREST"; // GET matching interest account
        LedgerAccount byNameAndOrgIdInterestAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(loanLedgerInterestAccountName, ledgerAccount.getOrgId());
        updateGeneralLedger(loanAccountTransaction, byNameAndOrgIdInterestAccount, BVMicroUtils.CREDIT, loanAccountTransaction.getInterestPaid(), true);

        //CREDIT VAT PAID
        if (loanAccountTransaction.getLoanAccount().isVatOption()) {
            updateGeneralLedger(loanAccountTransaction, BVMicroUtils.VAT, BVMicroUtils.CREDIT, loanAccountTransaction.getInterestPaid() * 0.1925, true);
        }

        loanAccountTransaction.setNotes(loanAccountTransaction.getNotes());

        //PRINCIPAL PAID
        double amount = loanAccountTransaction.getAmountReceived() - loanAccountTransaction.getInterestPaid();
        if (loanAccountTransaction.getLoanAccount().isVatOption()) {
            amount = amount - (loanAccountTransaction.getInterestPaid() * 0.1925);
        }
        updateGeneralLedger(loanAccountTransaction, ledgerAccount, BVMicroUtils.CREDIT, amount, true);
    }


    public void updateGLAfterLoanAccountTransferRepayment(LoanAccountTransaction loanAccountTransaction) {
//        GeneralLedger generalLedger = null;

        //DEBIT CURRENT TRANSFER
        updateGeneralLedger(loanAccountTransaction, BVMicroUtils.CURRENT, BVMicroUtils.DEBIT, loanAccountTransaction.getAmountReceived() * -1, true);

        LedgerAccount ledgerAccountInterest = determineLedgerAccount(loanAccountTransaction.getLoanAccount().getAccountType(), loanAccountTransaction.getOrgId());
        updateGeneralLedger(loanAccountTransaction, ledgerAccountInterest, BVMicroUtils.CREDIT, loanAccountTransaction.getInterestPaid(), true);

        // Check the ledgerAccount duplicate variable

//        updateGeneralLedger(loanAccountTransaction, BVMicroUtils.LOAN_INTEREST, BVMicroUtils.CREDIT, loanAccountTransaction.getInterestPaid(), true);

        //CREDIT VAT PAID
        if (loanAccountTransaction.getLoanAccount().isVatOption()) {
            updateGeneralLedger(loanAccountTransaction, BVMicroUtils.VAT, BVMicroUtils.CREDIT, loanAccountTransaction.getVatPercent(), true);
        }
        double amount = 0;
        //PRINCIPAL PAID
        if (loanAccountTransaction.getLoanAccount().isVatOption()) {
            amount = loanAccountTransaction.getAmountReceived() - loanAccountTransaction.getInterestPaid() - loanAccountTransaction.getVatPercent();
        } else {
            amount = loanAccountTransaction.getAmountReceived() - loanAccountTransaction.getInterestPaid();
        }

        LedgerAccount ledgerAccount = determineLedgerAccount(loanAccountTransaction.getLoanAccount().getAccountType(), loanAccountTransaction.getOrgId());
//        LedgerAccount ledgerAccount = ledgerAccountRepository.findByName(loanAccountTransaction.getLoanAccount().getAccountType().getName());

        updateGeneralLedger(loanAccountTransaction, ledgerAccount, BVMicroUtils.CREDIT, amount, true);
    }

    private void updateGeneralLedger(ShareAccountTransaction shareAccountTransaction, String ledgerAccount, String creditDebit,
                                     double amount, int classNumber, boolean generalGL) {
        GeneralLedger generalLedger;
        generalLedger = shareAccountGLMapper(shareAccountTransaction, generalGL);
        LedgerAccount generalLedgerGL = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, shareAccountTransaction.getOrgId());
        if(generalLedgerGL == null){

            System.out.println("------     --------    --------  " );
            System.out.println("------Could not find ---LA with name --------  "+ ledgerAccount );
            System.out.println("------     --------    --------  " );

        }
        if (generalGL) {
            generalLedger.setLedgerAccount(generalLedgerGL);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        generalLedger.setAccountNumber(shareAccountTransaction.getShareAccount().getAccountNumber());
        extracted(generalLedger);
        generalLedger.setGlClass(classNumber);
        shareAccountTransaction.setOrgId(shareAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);
    }


    private LedgerAccount updateGeneralLedger(DailySavingAccountTransaction savingAccountTransaction, String accountLedger, String creditDebit,
                                              double amount, boolean generalGL) {

        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = savingAccountGLMapper(savingAccountTransaction);
        LedgerAccount ledgerAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(accountLedger, savingAccountTransaction.getOrgId());
        if (ledgerAccount == null) {
            ledgerAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(accountLedger, savingAccountTransaction.getOrgId());
        }
        if (generalGL) {
            generalLedger.setLedgerAccount(ledgerAccount);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        generalLedger.setAccountNumber(savingAccountTransaction.getDailySavingAccount().getAccountNumber());
        generalLedger.setOrgId(savingAccountTransaction.getOrgId());
        extractClassCodeFromCode(generalLedger, ledgerAccount);
        generalLedger.setOrgId(savingAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);
        return ledgerAccount;
    }

    private LedgerAccount updateGeneralLedger(SavingAccountTransaction savingAccountTransaction, String accountLedger, String creditDebit,
                                              double amount, boolean generalGL) {

        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = savingAccountGLMapper(savingAccountTransaction);

        LedgerAccount ledgerAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(accountLedger, savingAccountTransaction.getOrgId());

//        if(ledgerAccount != null){
//            System.out.println("--------------------------------- aFound name: ledgerAccount found:"+ ledgerAccount.getId() +"- "+ledgerAccount.getName()+"  ------------------------");
//        }else {
//            System.out.println("--------------------------------- aFound name: ledgerAccount NOT found orgID:\n"+ savingAccountTransaction.getOrgId()+" --" + accountLedger +"- \n \n \n "  +savingAccountTransaction.getSavingAccount().toString()+" End   ------------------------");
//        }

        if (ledgerAccount == null) {
            ledgerAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(accountLedger, savingAccountTransaction.getOrgId());
        }
        if (generalGL) {
            generalLedger.setLedgerAccount(ledgerAccount);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        generalLedger.setAccountNumber(savingAccountTransaction.getSavingAccount().getAccountNumber());
        generalLedger.setOrgId(savingAccountTransaction.getOrgId());
        extractClassCodeFromCode(generalLedger, ledgerAccount);
        generalLedger.setOrgId(savingAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);
        return ledgerAccount;
    }

    private LedgerAccount updateGeneralLedger(LoanAccountTransaction loanAccountTransaction, String ledgerAccount, String creditDebit,
                                              double amount, boolean generalGL) {
        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = loanAccountGLMapper(loanAccountTransaction, generalGL);
        LedgerAccount aLedgerAccount = null;
        if (generalGL) {
            aLedgerAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(ledgerAccount, loanAccountTransaction.getOrgId());
            if (aLedgerAccount == null) {
                aLedgerAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, loanAccountTransaction.getOrgId());
            }
            generalLedger.setLedgerAccount(aLedgerAccount);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        generalLedger.setAccountNumber(loanAccountTransaction.getLoanAccount().getAccountNumber());
        extractClassCodeFromCode(generalLedger, aLedgerAccount);
        generalLedger.setOrgId(loanAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);

        return aLedgerAccount;
    }

    private LedgerAccount updateGeneralLedger(LoanAccountTransaction loanAccountTransaction, LedgerAccount aLedgerAccount, String creditDebit,
                                              double amount, boolean generalGL) {
        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = loanAccountGLMapper(loanAccountTransaction, generalGL);
        if (generalGL) {
            generalLedger.setLedgerAccount(aLedgerAccount);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        generalLedger.setAccountNumber(loanAccountTransaction.getLoanAccount().getAccountNumber());
        generalLedger.setOrgId(loanAccountTransaction.getOrgId());
        extractClassCodeFromCode(generalLedger, aLedgerAccount);
        generalLedgerRepository.save(generalLedger);

        return aLedgerAccount;
    }

    private void extractClassCodeFromCode(GeneralLedger generalLedger, LedgerAccount aLedgerAccount) {
        String code = aLedgerAccount.getCode();
        String classCode = code.substring(code.length() - 4, code.length() - 3);
        generalLedger.setGlClass(Integer.parseInt(classCode));
    }

    private void updateGeneralLedger(CurrentAccountTransaction currentAccountTransaction, String ledgerAccount, String creditDebit,
                                     double amount, boolean generalGL) {
        log.info(" ----                         -------                          --------------");
        log.info(ledgerAccount + "----------------- ledgerAccount from " +  currentAccountTransaction.getOrgId());
        log.info(" ----                         -------                          --------------");

        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = currentAccountGLMapper(currentAccountTransaction, generalGL);
        LedgerAccount currentGL = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, currentAccountTransaction.getOrgId());
        if (currentGL == null) {
            currentGL = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(ledgerAccount, currentAccountTransaction.getOrgId());
        }

        if (currentGL == null) {
            currentGL = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, currentAccountTransaction.getOrgId());
        }
        if (currentGL == null && ledgerAccount.equals("EVENT")) {

            currentGL = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, currentAccountTransaction.getOrgId());
        }
        if (generalGL) {
            generalLedger.setLedgerAccount(currentGL);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setAmount(amount);
        if (currentAccountTransaction.getCurrentAccount() == null) {
            generalLedger.setAccountNumber("00000000000000000000000");
        } else {
            generalLedger.setAccountNumber(currentAccountTransaction.getCurrentAccount().getAccountNumber());
        }

        extractClassCodeFromCode(generalLedger, currentGL);
        generalLedger.setOrgId(currentAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);
    }

    private void updateGeneralLedger(POSAccountTransaction posAccountTransaction, String ledgerAccount, String creditDebit,
                                     double amount, boolean generalGL) {
        GeneralLedger generalLedger;//CREDIT INTEREST PAID
        generalLedger = currentAccountGLMapper(posAccountTransaction, generalGL);

        LedgerAccount posGLAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(ledgerAccount, posAccountTransaction.getOrgId());

        if (posGLAccount == null) {
            posGLAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, posAccountTransaction.getOrgId());
        }
        if (posGLAccount == null && ledgerAccount.equals("EVENT")) {

            posGLAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(ledgerAccount, posAccountTransaction.getOrgId());
        }
        if (posGLAccount == null) {// Patch: Create missing Account to proceed
            posGLAccount = getPOSLedgerAccount(posAccountTransaction.getOrgId());
            ledgerAccountRepository.save(posGLAccount);
        }
        if (generalGL) {
            generalLedger.setLedgerAccount(posGLAccount);
        }
        generalLedger.setType(creditDebit);
        generalLedger.setBranchCode(posAccountTransaction.getBranchCode());
        generalLedger.setAmount(amount);
        if (posAccountTransaction.getPosAccount() != null) {
            generalLedger.setAccountNumber(posAccountTransaction.getPosAccount().getAccountNumber());
        } else {
            //generalLedger.setAccountNumber("00000000000000000000000");
            generalLedger.setAccountNumber(posAccountTransaction.getAccountOwner());
        }
        extractClassCodeFromCode(generalLedger, posGLAccount);
        generalLedger.setOrgId(posAccountTransaction.getOrgId());
        generalLedgerRepository.save(generalLedger);
    }

    private GeneralLedger loanAccountGLMapper(LoanAccountTransaction loanAccountTransaction, boolean generalGL) {
        GeneralLedger gl = new GeneralLedger();
        if (!generalGL) {
            gl.setAccountNumber(loanAccountTransaction.getLoanAccount().getAccountNumber());
        } else {
            gl.setAccountNumber(null);
        }
        gl.setAmount(loanAccountTransaction.getAmountReceived());
        Date date = BVMicroUtils.convertToDate(loanAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);

        gl.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        gl.setNotes(loanAccountTransaction.getNotes());
        gl.setReference(loanAccountTransaction.getReference());
        gl.setBranchCode(loanAccountTransaction.getBranchCode());
        gl.setRepresentative(loanAccountTransaction.getRepresentative());
        gl.setLastUpdatedBy(loanAccountTransaction.getCreatedBy());
        gl.setCreatedBy(loanAccountTransaction.getCreatedBy());
        gl.setOrgId(loanAccountTransaction.getOrgId());
        return gl;
    }

    private GeneralLedger shareAccountGLMapper(ShareAccountTransaction shareAccountTransaction, boolean generalGL) {
        GeneralLedger gl = new GeneralLedger();
        if (!generalGL) {
            gl.setAccountNumber(shareAccountTransaction.getShareAccount().getAccountNumber());
        } else {
            gl.setAccountNumber(null);
        }
        gl.setAmount(shareAccountTransaction.getShareAmount());
        Date date = BVMicroUtils.convertToDate(shareAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);

        gl.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        gl.setNotes(shareAccountTransaction.getNotes());
        gl.setRepresentative(shareAccountTransaction.getRepresentative());
        gl.setReference(shareAccountTransaction.getReference());
        gl.setBranchCode(shareAccountTransaction.getBranchCode());
        gl.setLastUpdatedBy(shareAccountTransaction.getCreatedBy());
        gl.setCreatedBy(shareAccountTransaction.getCreatedBy());
        gl.setOrgId(shareAccountTransaction.getShareAccount().getUser().getOrgId());
        return gl;
    }


    private GeneralLedger currentAccountGLMapper(CurrentAccountTransaction currentAccountTransaction, boolean generalGL) {
        GeneralLedger gl = new GeneralLedger();

        if (!generalGL) {
            gl.setAccountNumber(currentAccountTransaction.getCurrentAccount().getAccountNumber());
        } else {
            gl.setAccountNumber(null);
        }

        gl.setAmount(currentAccountTransaction.getCurrentAmount());
        Date date = BVMicroUtils.convertToDate(currentAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);
        gl.setLastUpdatedDate(date);
        gl.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        gl.setNotes(currentAccountTransaction.getNotes());
        gl.setReference(currentAccountTransaction.getReference());
        gl.setBranchCode(currentAccountTransaction.getBranchCode());
        gl.setLastUpdatedBy(currentAccountTransaction.getCreatedBy());
        gl.setCreatedBy(currentAccountTransaction.getCreatedBy());
        gl.setRepresentative(currentAccountTransaction.getRepresentative());
        gl.setGlClass(3); //TODO Saving which class in GL ?
        gl.setType(GeneralLedgerType.CREDIT.name());
        return gl;
    }

    private GeneralLedger currentAccountGLMapper(POSAccountTransaction posAccountTransaction, boolean generalGL) {
        GeneralLedger gl = new GeneralLedger();

        if (!generalGL) {
            gl.setAccountNumber(posAccountTransaction.getPosAccount().getAccountNumber());
        } else {
            gl.setAccountNumber(null);
        }

        gl.setAmount(posAccountTransaction.getSumTotal());
        Date date = BVMicroUtils.convertToDate(posAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);
        gl.setLastUpdatedDate(date);
        gl.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        gl.setNotes(posAccountTransaction.getNotes());
        gl.setReference(posAccountTransaction.getReference());
        gl.setBranchCode(posAccountTransaction.getBranchCode());
        gl.setLastUpdatedBy(posAccountTransaction.getCreatedBy());
        gl.setCreatedBy(posAccountTransaction.getCreatedBy());
        gl.setRepresentative(posAccountTransaction.getRepresentative());
        gl.setGlClass(3); //TODO Saving which class in GL ?
        gl.setType(GeneralLedgerType.CREDIT.name());
        return gl;
    }

    private GeneralLedger savingAccountGLMapper(DailySavingAccountTransaction savingAccountTransaction) {
        GeneralLedger gl = new GeneralLedger();
        gl.setAccountNumber(savingAccountTransaction.getDailySavingAccount().getAccountNumber());
        gl.setAmount(savingAccountTransaction.getSavingAmount());
        Date date = BVMicroUtils.convertToDate(savingAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);
        gl.setLastUpdatedDate(date);
        gl.setNotes(savingAccountTransaction.getNotes());
        gl.setReference(savingAccountTransaction.getReference());
        gl.setBranchCode(savingAccountTransaction.getBranchCode());
        gl.setLastUpdatedBy(savingAccountTransaction.getCreatedBy());
        gl.setCreatedBy(savingAccountTransaction.getCreatedBy());
        gl.setRepresentative(savingAccountTransaction.getRepresentative());
        gl.setGlClass(3); //TODO Saving which class in GL ?
        gl.setType(savingAccountTransaction.getSavingAmount() >= 0 ? "CREDIT" : "DEBIT");
        return gl;
    }

    private GeneralLedger savingAccountGLMapper(SavingAccountTransaction savingAccountTransaction) {
        GeneralLedger gl = new GeneralLedger();
        gl.setAccountNumber(savingAccountTransaction.getSavingAccount().getAccountNumber());
        gl.setAmount(savingAccountTransaction.getSavingAmount());
        Date date = BVMicroUtils.convertToDate(savingAccountTransaction.getCreatedDate());
        gl.setDate(date);
        gl.setCreatedDate(date);
        gl.setLastUpdatedDate(date);
        gl.setNotes(savingAccountTransaction.getNotes());
        gl.setReference(savingAccountTransaction.getReference());
        gl.setBranchCode(savingAccountTransaction.getBranchCode());
        gl.setLastUpdatedBy(savingAccountTransaction.getCreatedBy());
        gl.setCreatedBy(savingAccountTransaction.getCreatedBy());
        gl.setRepresentative(savingAccountTransaction.getRepresentative());
        gl.setGlClass(3); //TODO Saving which class in GL ?
        gl.setType(savingAccountTransaction.getSavingAmount() >= 0 ? "CREDIT" : "DEBIT");
        return gl;
    }

//    public GeneralLedgerBilanz findAll(long orgId) {
//        Iterable<GeneralLedger> glIterable = generalLedgerRepository.findAllOldestFirst(orgId);
//        List<GeneralLedgerWeb> generalLedgerWebs = mapperGeneralLedger(glIterable);
//        return getGeneralLedgerBilanz(generalLedgerWebs);
//    }

    public List<GeneralLedgerWeb> mapperGeneralLedger(Iterable<GeneralLedger> resultGeneralLedger) {
        final Iterator<GeneralLedger> iterator = resultGeneralLedger.iterator();
        List<GeneralLedgerWeb> result = new ArrayList<GeneralLedgerWeb>();
        while (iterator.hasNext()) {
            GeneralLedger next = iterator.next();
            result.add(extracted(next));
        }
        return result;
    }


    public List<GeneralLedgerWeb> mapperGeneralLedger(List<GeneralLedger> gls) {

        List<GeneralLedgerWeb> result = new ArrayList<GeneralLedgerWeb>();
        for (GeneralLedger next : gls) {
            result.add(extracted(next));
        }
        return result;
    }


    private GeneralLedgerWeb extracted(GeneralLedger next) {
        GeneralLedgerWeb generalLedgerWeb = new GeneralLedgerWeb();
        generalLedgerWeb.setId(next.getId());
        generalLedgerWeb.setCreatedDate(next.getCreatedDate());
        generalLedgerWeb.setRecordedDate(next.getDate());
        generalLedgerWeb.setAccountNumber(next.getAccountNumber());
        generalLedgerWeb.setCreatedBy(next.getCreatedBy());
        generalLedgerWeb.setGlClass(next.getGlClass());
        generalLedgerWeb.setLastUpdatedDate(next.getLastUpdatedDate());
        generalLedgerWeb.setNotes(next.getNotes());
        generalLedgerWeb.setRepresentative(next.getRepresentative());
        generalLedgerWeb.setAmount(next.getAmount());
        generalLedgerWeb.setLastUpdatedBy(next.getLastUpdatedBy());
        generalLedgerWeb.setType(next.getType());
        generalLedgerWeb.setReference(next.getReference());
        generalLedgerWeb.setLedgerAccount(next.getLedgerAccount());
        return generalLedgerWeb;

    }

    @NotNull
    private GeneralLedgerBilanz getGeneralLedgerBilanz(List<GeneralLedgerWeb> generalLedgerList) {
        double debitTotal = 0.0;
        double creditTotal = 0.0;
        double currentTotal = 0.0;
        GeneralLedgerBilanz bilanz = new GeneralLedgerBilanz();
        for (GeneralLedgerWeb current : generalLedgerList) {

            if (GeneralLedgerType.CREDIT.name().equals(current.getType())) {
                current.setAmount(current.getAmount() < 0 ? current.getAmount() * -1 : current.getAmount());
                creditTotal = creditTotal + current.getAmount();
//                currentTotal = currentTotal - current.getAmount();
            } else if (GeneralLedgerType.DEBIT.name().equals(current.getType())) {
                current.setAmount(current.getAmount() > 0 ? current.getAmount() * -1 : current.getAmount());
                debitTotal = debitTotal + current.getAmount();
//                currentTotal = currentTotal - current.getAmount();
            }
            current.setCurrentTotal(creditTotal + debitTotal);
            int p = 0;
        }

        bilanz.setTotal(creditTotal - debitTotal);
        bilanz.setDebitTotal(debitTotal);
        bilanz.setCreditTotal(creditTotal);
        bilanz.setGeneralLedgerWeb(generalLedgerList);
        return bilanz;
    }

    public GeneralLedgerBilanz findGLByType(String type, long orgId) {
        List<GeneralLedger> glByType = generalLedgerRepository.findGLByTypeAndOrgId(type, orgId);
        List<GeneralLedgerWeb> generalLedgerWebList = new ArrayList<GeneralLedgerWeb>();
        for (GeneralLedger aGeneralLedger : glByType) {
            generalLedgerWebList.add(extracted(aGeneralLedger));
        }
        return getGeneralLedgerBilanz(generalLedgerWebList);
    }


    public TrialBalanceBilanz getCurrentTrialBalance(LocalDateTime startDate, LocalDateTime endDate, long orgId) {

        String aStartDate = BVMicroUtils.formatDateTime(startDate);
        String aEndDate = BVMicroUtils.formatDateTime(endDate);

        TrialBalanceBilanz trialBalanceWeb = getTrialBalanceWebs(aStartDate, aEndDate, orgId);
        return trialBalanceWeb;
    }

    @NotNull
    public TrialBalanceBilanz getTrialBalanceWebs(String aStartDate, String aEndDate, long orgId) {
        List<TrialBalanceWeb> trialBalanceWebList = new ArrayList<TrialBalanceWeb>();
        Iterable<LedgerAccount> all = ledgerAccountRepository.findByOrgIdAndActiveTrue(orgId);
        TrialBalanceWeb trialBalanceWeb;
        TrialBalanceBilanz trialBalanceBilanz = new TrialBalanceBilanz();
        double bilanzTotalDebit = 0.0;
        double bilanzTotalCredit = 0.0;
        for (LedgerAccount aLedgerAccount : all) {
            trialBalanceWeb = new TrialBalanceWeb();
            Double debitTotal = 0.0;
            Double creditTotal = 0.0;
            Double total = 0.0;
            double totalDifference = 0.0;

            debitTotal =
                    generalLedgerRepository.searchCriteriaLedgerType(aStartDate, aEndDate, aLedgerAccount.getId(), BVMicroUtils.DEBIT, orgId);

            creditTotal =
                    generalLedgerRepository.searchCriteriaLedgerType(aStartDate, aEndDate, aLedgerAccount.getId(), BVMicroUtils.CREDIT, orgId);
            creditTotal = creditTotal == null ? new Double(0) : creditTotal;
            debitTotal = debitTotal == null ? new Double(0) : debitTotal;

            trialBalanceWeb.setCreditTotal(creditTotal);
            trialBalanceWeb.setDebitTotal(debitTotal);

            String creditBalance = aLedgerAccount.getCreditBalance();
            if (StringUtils.isNotEmpty(creditBalance) && creditBalance.equalsIgnoreCase("true")) {
                totalDifference = creditTotal + debitTotal;
                trialBalanceWeb.setCreditBalance(totalDifference < 0 ? "false" : "true");
                totalDifference = totalDifference < 0 ? totalDifference * -1 : totalDifference;
            } else {
                totalDifference = debitTotal + creditTotal;
                trialBalanceWeb.setCreditBalance(totalDifference > 0 ? "true" : "false");
                totalDifference = totalDifference < 0 ? totalDifference * -1 : totalDifference;
            }

            if (trialBalanceWeb.getCreditBalance().equals("true")) {
                bilanzTotalCredit = bilanzTotalCredit + totalDifference;
            } else {
                bilanzTotalDebit = bilanzTotalDebit + totalDifference;
            }

            trialBalanceWeb.setTotalDifference(totalDifference);
            trialBalanceWeb.setCode(aLedgerAccount.getCode());
            trialBalanceWeb.setType(aLedgerAccount.getCategory());
            trialBalanceWeb.setName(aLedgerAccount.getDisplayName());
            //TODO: Show only ledgers with entries
            if (trialBalanceWeb.getCreditTotal() == 0 && trialBalanceWeb.getDebitTotal() == 0) {
                continue;
            }
            trialBalanceWebList.add(trialBalanceWeb);
        }

        trialBalanceBilanz.setTrialBalanceWeb(trialBalanceWebList);
        trialBalanceBilanz.setCreditTotal(bilanzTotalCredit);
        trialBalanceBilanz.setDebitTotal(bilanzTotalDebit);

        return trialBalanceBilanz;
    }

    public GeneralLedgerBilanz searchCriteria(String startDate, String endDate, String agentUsername, long ledgerAccount, long orgId, String branchCode) {
        List<GeneralLedger> glList = null;
        if (ledgerAccount == -1 && agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaStartEndDate(startDate, endDate, orgId, branchCode);
        } else if (ledgerAccount != -1 && agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaLedger(startDate, endDate, ledgerAccount, orgId, branchCode);
        } else if (ledgerAccount == -1 && !agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaWithCreatedBy(startDate, endDate, agentUsername, orgId, branchCode);
        } else if (ledgerAccount != -1 && !agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaWithCreatedByAndLedgerAccount(startDate, endDate, agentUsername, ledgerAccount, orgId, branchCode);
        }
        List<GeneralLedgerWeb> generalLedgerWebs = mapperGeneralLedger(glList);
        GeneralLedgerBilanz generalLedgerBilanz = getGeneralLedgerBilanz(generalLedgerWebs);
        return generalLedgerBilanz;
    }

    public GeneralLedgerBilanz searchCriteria(String startDate, String endDate, String agentUsername, long ledgerAccount, long orgId) {
        List<GeneralLedger> glList = null;
        if (ledgerAccount == -1 && agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaStartEndDate(startDate, endDate, orgId);
        } else if (ledgerAccount != -1 && agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaLedger(startDate, endDate, ledgerAccount, orgId);
        } else if (ledgerAccount == -1 && !agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaWithCreatedBy(startDate, endDate, agentUsername, orgId);
        } else if (ledgerAccount != -1 && !agentUsername.equals("-1")) {
            glList = generalLedgerRepository.searchCriteriaWithCreatedByAndLedgerAccount(startDate, endDate, agentUsername, ledgerAccount, orgId);
        }
        List<GeneralLedgerWeb> generalLedgerWebs = mapperGeneralLedger(glList);
        GeneralLedgerBilanz generalLedgerBilanz = getGeneralLedgerBilanz(generalLedgerWebs);
        return generalLedgerBilanz;
    }

    public BillSelectionBilanz searchCriteriaBillSelection(String startDate, String endDate, String userName, long orgId) {
        List<CurrentAccountTransaction> currentAccountTransactions = new ArrayList<CurrentAccountTransaction>();
        List<SavingAccountTransaction> savingAccountTransactions = new ArrayList<SavingAccountTransaction>();
        List<DailySavingAccountTransaction> dailySavingAccountTransactions = new ArrayList<DailySavingAccountTransaction>();
        List<LoanAccountTransaction> loanAccountTransactions = new ArrayList<LoanAccountTransaction>();

        if (userName.equals("-1")) {
            currentAccountTransactions = currentAccountTransactionRepository.searchStartEndDate(startDate, endDate, orgId);
            savingAccountTransactions = savingAccountTransactionRepository.searchStartEndDate(startDate, endDate, orgId);
            dailySavingAccountTransactions = dailySavingAccountTransactionRepository.searchStartEndDate(startDate, endDate, orgId);
            loanAccountTransactions = loanAccountTransactionRepository.searchStartEndDate(startDate, endDate, orgId);
        } else {
            currentAccountTransactions = currentAccountTransactionRepository.searchStartEndDate(startDate, endDate, userName, orgId);
            savingAccountTransactions = savingAccountTransactionRepository.searchStartEndDate(startDate, endDate, userName, orgId);
            dailySavingAccountTransactions = dailySavingAccountTransactionRepository.searchStartEndDate(startDate, endDate, userName, orgId);
            loanAccountTransactions = loanAccountTransactionRepository.searchStartEndDate(startDate, endDate, userName, orgId);
        }

        BillSelectionBilanz billSelectionBilanz = new BillSelectionBilanz();

        extractCurrentAccountTransactions(currentAccountTransactions, billSelectionBilanz);
        extractSavingAccountTransactions(savingAccountTransactions, billSelectionBilanz);
        extractDailySavingAccountTransactions(dailySavingAccountTransactions, billSelectionBilanz);
        extractLoanAccountTransactions(loanAccountTransactions, billSelectionBilanz);

        billSelectionBilanz.setTotal(
                (billSelectionBilanz.getTenThousand() * 10000) +
                        (billSelectionBilanz.getFiveThousand() * 5000) +
                        (billSelectionBilanz.getTwoThousand() * 2000) +
                        (billSelectionBilanz.getOneThousand() * 1000) +
                        (billSelectionBilanz.getFiveHundred() * 500) +
                        (billSelectionBilanz.getOneHundred() * 100) +
                        (billSelectionBilanz.getFifty() * 50) +
                        (billSelectionBilanz.getTwentyFive() * 25) +
                        (billSelectionBilanz.getTen() * 10) +
                        (billSelectionBilanz.getFive() * 5) +
                        (billSelectionBilanz.getOne() * 1));

        return billSelectionBilanz;
    }

    private void extractLoanAccountTransactions(List<LoanAccountTransaction> loanAccountTransactions, BillSelectionBilanz billSelectionBilanz) {

        int tenThousand = 0;
        int fiveThousand = 0;
        int twoThousand = 0;
        int oneThousand = 0;
        int fiveHundred = 0;
        int oneHundred = 0;
        int fifty = 0;
        int twentyFive = 0;
        int ten = 0;
        int five = 0;
        int one = 0;

        for (LoanAccountTransaction aTransaction : loanAccountTransactions) {
            if (aTransaction.getWithdrawalDeposit() == -1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() - aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() - aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() - aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() - aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() - aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() - aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() - aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() - aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() - aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() - aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() - aTransaction.getOne());

            } else if (aTransaction.getWithdrawalDeposit() == 1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() + aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() + aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() + aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() + aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() + aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() + aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() + aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() + aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() + aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() + aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() + aTransaction.getOne());

            }

        }

        billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() + tenThousand);
        billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() + fiveThousand);
        billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() + twoThousand);
        billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() + oneThousand);
        billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() + fiveHundred);
        billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() + oneHundred);
        billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() + fifty);
        billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() + twentyFive);
        billSelectionBilanz.setTen(billSelectionBilanz.getTen() + ten);
        billSelectionBilanz.setFive(billSelectionBilanz.getFive() + five);
        billSelectionBilanz.setOne(billSelectionBilanz.getOne() + one);

    }


    private void extractDailySavingAccountTransactions(List<DailySavingAccountTransaction> savingAccountTransactions, BillSelectionBilanz billSelectionBilanz) {

        for (DailySavingAccountTransaction aTransaction : savingAccountTransactions) {
            if (aTransaction.getWithdrawalDeposit() == -1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() - aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() - aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() - aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() - aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() - aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() - aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() - aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() - aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTwentyFive() - aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getTwentyFive() - aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getTwentyFive() - aTransaction.getOne());

            } else if (aTransaction.getWithdrawalDeposit() == 1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() + aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() + aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() + aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() + aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() + aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() + aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() + aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() + aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() + aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() + aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() + aTransaction.getOne());

            }

        }

    }


    private void extractSavingAccountTransactions(List<SavingAccountTransaction> savingAccountTransactions, BillSelectionBilanz billSelectionBilanz) {

        for (SavingAccountTransaction aTransaction : savingAccountTransactions) {
            if (aTransaction.getWithdrawalDeposit() == -1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() - aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() - aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() - aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() - aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() - aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() - aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() - aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() - aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTwentyFive() - aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getTwentyFive() - aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getTwentyFive() - aTransaction.getOne());

            } else if (aTransaction.getWithdrawalDeposit() == 1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() + aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() + aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() + aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() + aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() + aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() + aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() + aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() + aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() + aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() + aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() + aTransaction.getOne());

            }

        }

    }

    private void extractCurrentAccountTransactions(List<CurrentAccountTransaction> currentAccountTransactions, BillSelectionBilanz billSelectionBilanz) {

        for (CurrentAccountTransaction aTransaction : currentAccountTransactions) {
            if (aTransaction.getWithdrawalDeposit() == -1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() - aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() - aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() - aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() - aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() - aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() - aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() - aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() - aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() - aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() - aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() - aTransaction.getOne());

            } else if (aTransaction.getWithdrawalDeposit() == 1) {

                billSelectionBilanz.setTenThousand(billSelectionBilanz.getTenThousand() + aTransaction.getTenThousand());
                billSelectionBilanz.setFiveThousand(billSelectionBilanz.getFiveThousand() + aTransaction.getFiveThousand());
                billSelectionBilanz.setTwoThousand(billSelectionBilanz.getTwoThousand() + aTransaction.getTwoThousand());
                billSelectionBilanz.setOneThousand(billSelectionBilanz.getOneThousand() + aTransaction.getOneThousand());
                billSelectionBilanz.setFiveHundred(billSelectionBilanz.getFiveHundred() + aTransaction.getFiveHundred());
                billSelectionBilanz.setOneHundred(billSelectionBilanz.getOneHundred() + aTransaction.getOneHundred());
                billSelectionBilanz.setFifty(billSelectionBilanz.getFifty() + aTransaction.getFifty());
                billSelectionBilanz.setTwentyFive(billSelectionBilanz.getTwentyFive() + aTransaction.getTwentyFive());
                billSelectionBilanz.setTen(billSelectionBilanz.getTen() + aTransaction.getTen());
                billSelectionBilanz.setFive(billSelectionBilanz.getFive() + aTransaction.getFive());
                billSelectionBilanz.setOne(billSelectionBilanz.getOne() + aTransaction.getOne());

            }
        }

    }

    public GeneralLedgerBilanz findGLByLedgerAccount(long ledgerAccountId) {

        LedgerAccount ledgerAccount = ledgerAccountRepository.findById(ledgerAccountId).get();
        List<GeneralLedger> glList = ledgerAccount.getGeneralLedger();

        List<GeneralLedgerWeb> generalLedgerWebs = mapperGeneralLedger(glList);
        GeneralLedgerBilanz generalLedgerBilanz = getGeneralLedgerBilanz(generalLedgerWebs);
        return generalLedgerBilanz;
    }

    @Transactional
    public void updateManualAccountTransaction(LedgerEntryDTO ledgerEntryDTO, boolean excelImport, String countryCode, String branchCode) {
        Date date = new Date();
        Date recorded = new Date();
        String loggedInUserName = getLoggedInUserName();
        String reference = BVMicroUtils.getSaltString();
        //Reco
        GeneralLedger aGeneralLedger = recordGLFirstEntry(ledgerEntryDTO, date, loggedInUserName, reference, ledgerEntryDTO.getOrgId(), excelImport);
        GeneralLedger generalLedger;

        //record opposite double entry
        generalLedger = new GeneralLedger();
        generalLedger.setType(BVMicroUtils.getOppositeCreditOrDebit(ledgerEntryDTO.getCreditOrDebit()));
        generalLedger.setNotes(ledgerEntryDTO.getNotes());
        generalLedger.setReference(reference);
        generalLedger.setBranchCode(branchCode);

        generalLedger.setOrgId(ledgerEntryDTO.getOrgId());
        String recordDateString = ledgerEntryDTO.getRecordDate();
        Date recordedDate = BVMicroUtils.formatDate(recordDateString);
        // recorded = excelImport? recordedDate :recordedDate;
        generalLedger.setCreatedDate(recorded);
        generalLedger.setDate(aGeneralLedger.getDate());
        generalLedger.setLastUpdatedBy(loggedInUserName);
        generalLedger.setCreatedBy(loggedInUserName);
        generalLedger.setAmount(generalLedger.getType().equals("DEBIT") ? ledgerEntryDTO.getLedgerAmount() * -1 : ledgerEntryDTO.getLedgerAmount());

        generalLedger.setLastUpdatedDate(date);

        LedgerAccount destinationAccount = ledgerAccountRepository.findById(ledgerEntryDTO.getDestinationLedgerAccount()).get();
        LedgerAccount originAccount = ledgerAccountRepository.findById(ledgerEntryDTO.getOriginLedgerAccount()).get();
        generalLedger.setLedgerAccount(destinationAccount);
        generalLedger.setGlClass(Integer.parseInt(destinationAccount.getCategory().substring(0, 1)));
        generalLedgerRepository.save(generalLedger);
        callCenterService.saveCallCenterLog(reference, loggedInUserName, "", BVMicroUtils.formatCurrency(ledgerEntryDTO.getLedgerAmount(), countryCode) + " From: " + originAccount.getDisplayName() + " -- > To: " + destinationAccount.getDisplayName() + ":" + ledgerEntryDTO.getNotes());

    }

    public GeneralLedger recordGLFirstEntry(LedgerEntryDTO ledgerEntryDTO, Date date, String loggedInUserName, String reference, long orgId, boolean excelImport) {
        Date formatDate = BVMicroUtils.formatDate(ledgerEntryDTO.getRecordDate());
        GeneralLedger generalLedger = new GeneralLedger();
        generalLedger.setType(ledgerEntryDTO.getCreditOrDebit());
        generalLedger.setNotes(ledgerEntryDTO.getNotes());
        generalLedger.setReference(reference);
        generalLedger.setOrgId(ledgerEntryDTO.getOrgId());
        generalLedger.setDate(formatDate != null ? formatDate : date);
        generalLedger.setLastUpdatedBy(loggedInUserName);
        generalLedger.setCreatedBy(loggedInUserName);

        generalLedger.setAmount(ledgerEntryDTO.getCreditOrDebit().equals("DEBIT") ? ledgerEntryDTO.getLedgerAmount() * -1 : ledgerEntryDTO.getLedgerAmount());
        if (excelImport) {
            generalLedger.setCreatedDate(BVMicroUtils.formatDate(ledgerEntryDTO.getRecordDate())); //From Excel import
        } else {
            generalLedger.setCreatedDate(date);
        }
        generalLedger.setRepresentative(loggedInUserName);
        generalLedger.setLastUpdatedDate(new Date(System.currentTimeMillis()));
        generalLedger.setAccountNumber(ledgerEntryDTO.getAccountNumber());
        LedgerAccount originalAccount = ledgerAccountRepository.findById(ledgerEntryDTO.getOriginLedgerAccount()).get();
        generalLedger.setLedgerAccount(originalAccount);
        generalLedger.setGlClass(Integer.parseInt(originalAccount.getCode().split("_GL_")[1].substring(0, 1)));
//        generalLedger.setGlClass(new Integer(originalAccount.getCategory().substring(0, 1)));
        generalLedger.setOrgId(orgId);
        generalLedgerRepository.save(generalLedger);
        return generalLedger;
    }

    public AccountType getAccountType(String accountNumber, long orgId) {
        return accountTypeRepository.findByNumberAndOrgIdAndActiveTrue(accountNumber, orgId);
    }

    public void updateGLAfterSavingAccountTransaction(SavingAccountTransaction savingAccountTransaction, String debitCredit) {
        savingAccountTransaction.getNotes();

        if (savingAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CASH)) {
            savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());
            updateGeneralLedger(savingAccountTransaction, BVMicroUtils.CASH, debitCredit.equals(BVMicroUtils.DEBIT) ? BVMicroUtils.CREDIT : BVMicroUtils.DEBIT, savingAccountTransaction.getSavingAmount(), true);
        } else if (savingAccountTransaction.getModeOfPayment().equals(BVMicroUtils.TRANSFER)) {
            savingAccountTransaction.setNotes(BVMicroUtils.TRANSFER + " " + savingAccountTransaction.getNotes());
            updateGeneralLedger(savingAccountTransaction, savingAccountTransaction.getSavingAccount().getAccountSavingType().getName(), debitCredit, savingAccountTransaction.getSavingAmount() * -1, true);
        }

        if (savingAccountTransaction.getModeOfPayment().equals(BVMicroUtils.SAVING_TO_GL_TRANSFER)) {
            savingAccountTransaction.setNotes(BVMicroUtils.CURRENT_TO_GL_TRANSFER + savingAccountTransaction.getNotes());
            updateGeneralLedger(savingAccountTransaction, savingAccountTransaction.getSavingAccount().getAccountType().getName(), BVMicroUtils.DEBIT, savingAccountTransaction.getSavingAmount(), true);
        }


        //TODO: COMPARE
//        String notes = currentAccountTransaction.getNotes();
//        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CASH)) {
//            currentAccountTransaction.setNotes( notes);
//            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, currentAccountTransaction.getCurrentAmount() > 0 ? "CREDIT" : "DEBIT", currentAccountTransaction.getCurrentAmount()*-1,  true);
//            currentAccountTransaction.setNotes(notes);
//            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CASH, currentAccountTransaction.getCurrentAmount() > 0 ? "DEBIT" : "CREDIT", currentAccountTransaction.getCurrentAmount(),  true);
//            currentAccountTransaction.setNotes(notes);
//        }
//        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CURRENT_TO_GL_TRANSFER)) {
//            currentAccountTransaction.setNotes( notes );
//            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT_GL_3004, BVMicroUtils.DEBIT, currentAccountTransaction.getCurrentAmount(),  true);
//        }

    }

    public void updateGLAfterCashSavingAccountTransaction(SavingAccountTransaction savingAccountTransaction) {
        double amount = savingAccountTransaction.getSavingAmount();
        String creditDebit = "";

        if (savingAccountTransaction.getWithdrawalDeposit() == -1) {
            updateGeneralLedger(savingAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.CREDIT, savingAccountTransaction.getSavingAmount() * -1, true);
            creditDebit = BVMicroUtils.DEBIT;
        } else {
            updateGeneralLedger(savingAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.DEBIT, savingAccountTransaction.getSavingAmount() * -1, true);
            creditDebit = BVMicroUtils.CREDIT;
        }
        updateGeneralLedger(savingAccountTransaction, savingAccountTransaction.getSavingAccount().getAccountSavingType().getName(), creditDebit, amount, true);
        savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());

    }


    public void updateGLAfterCashDailySavingAccountTransaction(DailySavingAccountTransaction savingAccountTransaction) {
        double amount = savingAccountTransaction.getSavingAmount();
        String creditDebit = "";

        if (savingAccountTransaction.getWithdrawalDeposit() == -1) {
            updateGeneralLedger(savingAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.CREDIT, savingAccountTransaction.getSavingAmount() * -1, true);
            creditDebit = BVMicroUtils.DEBIT;
        } else {
            updateGeneralLedger(savingAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.DEBIT, savingAccountTransaction.getSavingAmount() * -1, true);
            creditDebit = BVMicroUtils.CREDIT;
        }
        updateGeneralLedger(savingAccountTransaction, savingAccountTransaction.getDailySavingAccount().getAccountSavingType().getName(), creditDebit, amount, true);
        savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());

    }


    public void updateGLAfterCurrentAccountTransaction(CurrentAccountTransaction currentAccountTransaction) {
        String notes = currentAccountTransaction.getNotes();
        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CASH)) {
            currentAccountTransaction.setNotes(notes);
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, currentAccountTransaction.getCurrentAmount() > 0 ? "CREDIT" : "DEBIT", currentAccountTransaction.getCurrentAmount() * -1, true);
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CASH, currentAccountTransaction.getCurrentAmount() > 0 ? "DEBIT" : "CREDIT", currentAccountTransaction.getCurrentAmount(), true);
        }
        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CURRENT_TO_GL_TRANSFER)) {
            currentAccountTransaction.setNotes(notes);
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT_GL_3004, BVMicroUtils.DEBIT, currentAccountTransaction.getCurrentAmount(), true);
        }

    }

    public void updateGLAfterPosAccountTransaction(POSAccountTransaction posAccountTransaction, String targetGL) {
        String notes = posAccountTransaction.getNotes();
        if (posAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CASH)) {
            posAccountTransaction.setNotes(notes);
            updateGeneralLedger(posAccountTransaction, BVMicroUtils.POS_GL_3333, posAccountTransaction.getSumTotal() > 0 ? "CREDIT" : "DEBIT", posAccountTransaction.getSumTotal() * -1, true);
            posAccountTransaction.setNotes(notes);
            updateGeneralLedger(posAccountTransaction, targetGL, posAccountTransaction.getSumTotal() > 0 ? "DEBIT" : "CREDIT", posAccountTransaction.getSumTotal(), true);
         }
//        if (posAccountTransaction.getModeOfPayment().equals(BVMicroUtils.POS_GL_3333)) {
//            posAccountTransaction.setNotes( notes );
//            updateGeneralLedger(posAccountTransaction, BVMicroUtils.POS_GL_3333, BVMicroUtils.DEBIT, posAccountTransaction.getSumTotal(),  true);
//        }

    }

    public void updateGLAfterCurrentAccountAfterCashTransaction(CurrentAccountTransaction currentAccountTransaction) {
        String notes = currentAccountTransaction.getNotes();
        currentAccountTransaction.setNotes(notes);

        if (currentAccountTransaction.getCurrentAmount() > 0) {
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, BVMicroUtils.CREDIT, currentAccountTransaction.getCurrentAmount(), true);
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.DEBIT, currentAccountTransaction.getCurrentAmount() * -1, true);

        } else if (currentAccountTransaction.getCurrentAmount() < 0) {
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, BVMicroUtils.DEBIT, currentAccountTransaction.getCurrentAmount(), true);
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CASH, BVMicroUtils.CREDIT, currentAccountTransaction.getCurrentAmount() * -1, true);
        }
        currentAccountTransaction.setNotes(notes);
    }


    public void updateGLAfterCurrentCurrentTransfer(CurrentAccountTransaction currentAccountTransaction) {
        String notes = currentAccountTransaction.getNotes();
        currentAccountTransaction.getNotes();
        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CURRENT_CURRENT_TRANSFER)) {
            currentAccountTransaction.setNotes(currentAccountTransaction.getNotes());
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, "DEBIT", currentAccountTransaction.getCurrentAmount() * -1, true);
            currentAccountTransaction.setNotes(currentAccountTransaction.getNotes());
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, "CREDIT", currentAccountTransaction.getCurrentAmount(), true);
            currentAccountTransaction.setNotes(notes);
        }
    }

    public void updateGLAfterCurrentDebitTransfer(CurrentAccountTransaction currentAccountTransaction, SavingAccountTransaction savingAccountTransaction) {
        ;
        LedgerAccount ledgerAccount = determineLedgerAccount(savingAccountTransaction.getSavingAccount().getAccountType(), savingAccountTransaction.getOrgId());
        if (currentAccountTransaction.getModeOfPayment().equals(BVMicroUtils.CURRENT_DEBIT_TRANSFER)) {
            currentAccountTransaction.setNotes(ledgerAccount.getCode() + " " + currentAccountTransaction.getNotes());
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT, "DEBIT", currentAccountTransaction.getCurrentAmount(), true);

            savingAccountTransaction.setSavingAmount(savingAccountTransaction.getSavingAmount()); //testing
            updateGeneralLedger(savingAccountTransaction, ledgerAccount.getCode(), "CREDIT", savingAccountTransaction.getSavingAmount(), true);
            currentAccountTransaction.setNotes(currentAccountTransaction.getNotes());


        }
    }

    public void updateGLAfterDebitDebitTransfer(SavingAccountTransaction fromSavingAccountTransaction, SavingAccountTransaction toSavingAccountTransaction) {
        LedgerAccount fromLedgerAccount = determineLedgerAccount(fromSavingAccountTransaction.getSavingAccount().getAccountType(), fromSavingAccountTransaction.getOrgId());
        LedgerAccount toLedgerAccount = determineLedgerAccount(toSavingAccountTransaction.getSavingAccount().getAccountType(), fromSavingAccountTransaction.getOrgId());
        String fromNotes = fromSavingAccountTransaction.getNotes();
        String toNotes = fromSavingAccountTransaction.getNotes();
        if (fromSavingAccountTransaction.getModeOfPayment().equals(BVMicroUtils.DEBIT_DEBIT_TRANSFER)) {
            fromSavingAccountTransaction.setNotes(fromSavingAccountTransaction.getNotes());
            updateGeneralLedger(fromSavingAccountTransaction, fromLedgerAccount.getCode(), "DEBIT", fromSavingAccountTransaction.getSavingAmount(), true);
            toSavingAccountTransaction.setNotes(toSavingAccountTransaction.getNotes());
            updateGeneralLedger(toSavingAccountTransaction, toLedgerAccount.getCode(), "CREDIT", toSavingAccountTransaction.getSavingAmount(), true);
            fromSavingAccountTransaction.setNotes(fromNotes);
            toSavingAccountTransaction.setNotes(toNotes);
        }
    }

    public void updateGLAfterDebitCurrentTransfer(SavingAccountTransaction savingAccountTransaction, CurrentAccountTransaction currentAccountTransaction) {
        LedgerAccount savingLedgerAccount = determineLedgerAccount(savingAccountTransaction.getSavingAccount().getAccountType(), savingAccountTransaction.getOrgId());
        String notes = savingAccountTransaction.getNotes();
        if (savingAccountTransaction.getModeOfPayment().equals(BVMicroUtils.DEBIT_CURRENT_TRANSFER)) {
            updateGeneralLedger(currentAccountTransaction, BVMicroUtils.CURRENT_GL_3004, "CREDIT", currentAccountTransaction.getCurrentAmount(), true);
            updateGeneralLedger(savingAccountTransaction, savingLedgerAccount.getCode(), "DEBIT", savingAccountTransaction.getSavingAmount(), true);
        }
    }


    @Transactional
    public void updateGLAfterLedgerAccountMultipleGLEntry(LedgerEntryDTO newLedgerEntryDTO) {
        String reference = "";
        List<String> paramValueString = newLedgerEntryDTO.getParamValueString();
        long originCurrentAccount = newLedgerEntryDTO.getOriginLedgerAccount();
        long orgId;
        Optional<CurrentAccount> byId = currentAccountRepository.findById(originCurrentAccount);
        String accountNumber = "";
        if (byId.isPresent()) {
            accountNumber = byId.get().getAccountNumber();
            reference = extractFromCurrentAccountToGLAccounts(newLedgerEntryDTO, byId.get());
            orgId = byId.get().getOrgId();
        } else {
            SavingAccount savingAccount = savingAccountRepository.findById(originCurrentAccount).get();
            accountNumber = savingAccount.getAccountNumber();
            extractFromSavingAccountToGLAccounts(newLedgerEntryDTO, savingAccount);
            orgId = savingAccount.getOrgId();
        }

        newLedgerEntryDTO.setCreditOrDebit(BVMicroUtils.CREDIT);
        String ledgerAccountId = "";
        String accountAmount = "";
//        int i = 0;
        for (String aString : paramValueString) {
            String[] s = aString.split("_");
            ledgerAccountId = s[0];
            accountAmount = s[1];
            Double amount =  Double.parseDouble(accountAmount);
            if (amount.doubleValue() == 0.0) continue;

            newLedgerEntryDTO.setOriginLedgerAccount(Long.parseLong(ledgerAccountId));
            newLedgerEntryDTO.setLedgerAmount(Double.parseDouble(accountAmount));
            newLedgerEntryDTO.setAccountNumber(accountNumber);
//            ++i;
            recordGLFirstEntry(newLedgerEntryDTO,
                    BVMicroUtils.formatDate(newLedgerEntryDTO.getRecordDate()), getLoggedInUserName(), reference, orgId, false);
        }
    }


    private String extractFromCurrentAccountToGLAccounts(LedgerEntryDTO newLedgerEntryDTO, CurrentAccount currentAccount) {

        CurrentAccountTransaction currentAccountTransaction = new CurrentAccountTransaction();
        currentAccountTransaction.setAccountOwner("false");
//        currentAccountTransaction.setCurrentAmount(newLedgerEntryDTO.getLedgerAmount());
        currentAccountTransaction.setRepresentative(getLoggedInUserName());
        currentAccountTransaction.setCurrentAccount(currentAccount);
        currentAccountTransaction.setNotes(newLedgerEntryDTO.getNotes());
        currentAccountTransaction.setWithdrawalDeposit(-1);
        currentAccountTransaction.setModeOfPayment(BVMicroUtils.CURRENT_TO_GL_TRANSFER);
        currentAccountTransaction.setCurrentAmount(newLedgerEntryDTO.getLedgerAmount() * -1);
        if (StringUtils.isNotEmpty(currentAccountService.withdrawalAllowed(currentAccountTransaction))) {
            // TODO: Move this check to controller
        }
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
        currentAccountTransaction.setBranch(branchInfo.getId());
        currentAccountTransaction.setBranchCode(branchInfo.getCode());
        currentAccountTransaction.setBranchCountry(branchInfo.getCountry());
        currentAccountTransaction.setCreatedDate(BVMicroUtils.formatLocaleDate(newLedgerEntryDTO.getRecordDate()));
        currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        currentAccountService.createCurrentAccountTransaction(currentAccountTransaction, currentAccount);
        generalLedgerService.updateGLAfterCurrentAccountTransaction(currentAccountTransaction);
        return currentAccountTransaction.getReference();

    }


    private void extractFromSavingAccountToGLAccounts(LedgerEntryDTO newLedgerEntryDTO, SavingAccount savingAccount) {

        SavingAccountTransaction savingAccountTransaction = new SavingAccountTransaction();
        savingAccountTransaction.setAccountOwner("false");
        savingAccountTransaction.setSavingAmount(newLedgerEntryDTO.getLedgerAmount() * -1);
        savingAccountTransaction.setRepresentative(getLoggedInUserName());
        savingAccountTransaction.setSavingAccount(savingAccount);
        savingAccountTransaction.setNotes(newLedgerEntryDTO.getNotes());
        savingAccountTransaction.setWithdrawalDeposit(-1);
        savingAccountTransaction.setModeOfPayment(BVMicroUtils.SAVING_TO_GL_TRANSFER);
//        if(StringUtils.isNotEmpty(currentAccountService.withdrawalAllowed(savingAccountTransaction))){
//            // TODO: Move this check to controller
//        }
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
        savingAccountTransaction.setBranch(branchInfo.getId());
        savingAccountTransaction.setBranchCode(branchInfo.getCode());
        savingAccountTransaction.setBranchCountry(branchInfo.getCountry());
        savingAccountTransaction.setCreatedDate(BVMicroUtils.formatLocaleDate(newLedgerEntryDTO.getRecordDate()));
        savingAccount.getSavingAccountTransaction().add(savingAccountTransaction);
        savingAccountTransaction.setReference(BVMicroUtils.getSaltString()); //Collision
        savingAccountTransaction.setCreatedBy(getLoggedInUserName());
        savingAccountTransaction.setAccountBalance(savingAccountService.calculateAccountBalance(savingAccountTransaction.getSavingAmount(), savingAccountTransaction.getSavingAccount()));
        generalLedgerService.updateGLAfterSavingAccountTransaction(savingAccountTransaction, BVMicroUtils.DEBIT);
    }

    @Transactional
    public void updateGLAfterLedgerAccountMultipleAccountEntry(LedgerEntryDTO newLedgerEntryDTO, long orgId, String reference, RuntimeSetting runtimeSetting) {
        List<String> paramValueString = newLedgerEntryDTO.getParamValueString();
        newLedgerEntryDTO.setCreditOrDebit(BVMicroUtils.DEBIT);
        GeneralLedger generalLedger = recordGLFirstEntry(newLedgerEntryDTO, BVMicroUtils.formatDate(newLedgerEntryDTO.getRecordDate()), getLoggedInUserName(), reference, orgId, false);

        String accountNumber = "";
        String accountAmount = "";
        int i = 0;
        for (String aString : paramValueString) {
            String[] s = aString.split("_");
            accountNumber = s[0];
            accountAmount = s[1];
            Double amount = new Double(accountAmount);
            if (amount.doubleValue() == 0.0) continue;
            ++i;
            LedgerAccount ledgerAccount = determineLedgerAccount(accountNumber, generalLedger.getOrgId());
            if (ledgerAccount != null) {
                System.out.println("---------      accno        ------" + accountAmount);
                System.out.println("---------      orgID        ------" + generalLedger.getOrgId());
            } else {
                System.out.println("---------                                    ------");
                System.out.println("---------                                    ------");
                System.out.println("---------                                    ------");


            }
            final Integer productCode = new Integer(accountNumber.substring(3, 5));
//            accountNumber = accountNumber.substring(10, 21);

            if (productCode > 9 && productCode < 20) {

                SavingAccount byAccountNumber = savingAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
                SavingAccountTransaction savingAccountTransaction = new SavingAccountTransaction();
                savingAccountTransaction.setSavingAccount(byAccountNumber);
                savingAccountTransaction.setWithdrawalDeposit(1);
                savingAccountTransaction.setSavingAmount(amount);
                savingAccountTransaction.setNotes("GL Account to transfer" + newLedgerEntryDTO.getNotes());
                savingAccountTransaction.setCreatedBy(getLoggedInUserName());
                savingAccountTransaction.setReference(generalLedger.getReference() + "_" + i);
                Date date = BVMicroUtils.formatDate(newLedgerEntryDTO.getRecordDate());
                savingAccountTransaction.setCreatedDate(BVMicroUtils.convertToLocalDateTimeViaMilisecond(date));
                savingAccountTransaction.setModeOfPayment(BVMicroUtils.GL_TRANSFER);
                Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
                savingAccountTransaction.setBranch(branchInfo.getId());
                savingAccountTransaction.setBranchCode(branchInfo.getCode());
                savingAccountTransaction.setBranchCountry(branchInfo.getCountry());
                savingAccountTransaction.setOrgId(byAccountNumber.getOrgId());
//                savingAccountTransaction.setAccountOwner(byAccountNumber.getUser().getLastName());
                savingAccountTransaction.setSavingAmountInLetters("SYSTEM");
                savingAccountTransactionRepository.save(savingAccountTransaction);
                byAccountNumber.getSavingAccountTransaction().add(savingAccountTransaction);
                savingAccountRepository.save(byAccountNumber);
                updateGeneralLedger(savingAccountTransaction, ledgerAccount.getCode(), BVMicroUtils.CREDIT, savingAccountTransaction.getSavingAmount(), true);
                callCenterService.callCenterSavingAccountTransaction(savingAccountTransaction, runtimeSetting);
            } else if (productCode == 20) {

                CurrentAccount byAccountNumber = currentAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
                CurrentAccountTransaction currentAccountTransaction = new CurrentAccountTransaction();
                currentAccountTransaction.setCurrentAccount(byAccountNumber);
                currentAccountTransaction.setWithdrawalDeposit(1);
                currentAccountTransaction.setCurrentAmount(amount);
                currentAccountTransaction.setNotes("GL Account to transfer.  " + newLedgerEntryDTO.getNotes());
                currentAccountTransaction.setCreatedBy(getLoggedInUserName());
                currentAccountTransaction.setReference(generalLedger.getReference() + "_" + i);
                Date date = BVMicroUtils.formatDate(newLedgerEntryDTO.getRecordDate());
                currentAccountTransaction.setCreatedDate(BVMicroUtils.convertToLocalDateTimeViaMilisecond(date));
                currentAccountTransaction.setModeOfPayment(BVMicroUtils.GL_TRANSFER);
                Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
                currentAccountTransaction.setBranch(branchInfo.getId());
                currentAccountTransaction.setBranchCode(branchInfo.getCode());
                currentAccountTransaction.setBranchCountry(branchInfo.getCountry());
//                    currentAccountTransaction.setAccountOwner(byAccountNumber.getUser().getLastName());
                currentAccountTransaction.setOrgId(byAccountNumber.getOrgId());
                currentAccountTransaction.setCurrentAmountInLetters("SYSTEM");
                currentAccountTransactionRepository.save(currentAccountTransaction);
                byAccountNumber.getCurrentAccountTransaction().add(currentAccountTransaction);
                currentAccountRepository.save(byAccountNumber);
                double currentAmount = currentAccountTransaction.getCurrentAmount();
                System.out.println("----         ---           ---------  " + currentAmount);
                updateGeneralLedger(currentAccountTransaction, ledgerAccount.getCode(), BVMicroUtils.CREDIT, currentAccountTransaction.getCurrentAmount(), true);


            } else if (productCode > 39 && productCode < 50) {
                LoanAccount byAccountNumber = loanAccountRepository.findByAccountNumberAndOrgId(accountNumber, orgId);
                LoanAccountTransaction loanAccountTransaction = new LoanAccountTransaction();
                loanAccountTransaction.setLoanAccount(byAccountNumber);
                loanAccountTransaction.setWithdrawalDeposit(1);
//                loanAccountTransaction.setLoanAmount(new Double(accountAmount));
                loanAccountTransaction.setAmountReceived(amount);
                loanAccountTransaction.setNotes("GL Account to transfer. " + newLedgerEntryDTO.getNotes());
                loanAccountTransaction.setCreatedBy(getLoggedInUserName());
                loanAccountTransaction.setReference(generalLedger.getReference() + "_" + i);
                Date date = BVMicroUtils.formatDate(newLedgerEntryDTO.getRecordDate());
                loanAccountTransaction.setCreatedDate(BVMicroUtils.convertToLocalDateTimeViaMilisecond(date));
                loanAccountTransaction.setModeOfPayment(BVMicroUtils.GL_TRANSFER);
                Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
                loanAccountTransaction.setBranch(branchInfo.getId());
                loanAccountTransaction.setBranchCode(branchInfo.getCode());
                loanAccountTransaction.setBranchCountry(branchInfo.getCountry());
                loanAccountTransaction.setAccountOwner("false");
                loanAccountTransaction.setLoanAmountInLetters("SYSTEM");
                loanAccountTransaction.setRepresentative("GL TRANSFER");
                loanAccountTransaction.setOrgId(byAccountNumber.getOrgId());

                loanAccountService.updateInterestOwedPayment(byAccountNumber, loanAccountTransaction);
                loanAccountService.calculateAccountBilanz(byAccountNumber.getLoanAccountTransaction(), true, runtimeSetting.getCountryCode());

                updateGeneralLedger(loanAccountTransaction, ledgerAccount.getCode(), BVMicroUtils.CREDIT, loanAccountTransaction.getAmountReceived(), true);
                byAccountNumber.getLoanAccountTransaction().add(loanAccountTransaction);
                loanAccountRepository.save(byAccountNumber);
            }
        }
    }

    public LedgerAccount determineLedgerAccount(String accountNumber, long orgId) {
        System.out.println(accountNumber + " --     ---    -- " + orgId);
        System.out.println(accountNumber + " --     ---    -- " + orgId);
        String productCode = "";
        if (StringUtils.isNotEmpty(accountNumber) && accountNumber.length() != 2) {
            productCode = accountNumber.substring(3, 5);
        }
        System.out.println(" Product Code --     ---    -- " + productCode);

        if (productCode.equals("20")) {

            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CURRENT, orgId);
        } else if (productCode.equals("11")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.GENERAL_SAVINGS, orgId);
        } else if (productCode.equals("12")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.RETIREMENT_SAVINGS, orgId);
        } else if (productCode.equals("13")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.DAILY_SAVINGS, orgId);
        } else if (productCode.equals("14")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.MEDICAL_SAVINGS, orgId);
        } else if (productCode.equals("15")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.SOCIAL_SAVINGS, orgId);
        } else if (productCode.equals("16")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.BUSINESS_SAVINGS, orgId);
        } else if (productCode.equals("17")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CHILDREN_SAVINGS, orgId);
        } else if (productCode.equals("18")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.REAL_ESTATE_SAVINGS, orgId);
        } else if (productCode.equals("19")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.EDUCATION_SAVINGS, orgId);
        } else if (productCode.equals("41")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.SHORT_TERM_LOAN, orgId);
        } else if (productCode.equals("42")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CONSUMPTION_LOAN, orgId);
        } else if (productCode.equals("43")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.AGRICULTURE_LOAN, orgId);
        } else if (productCode.equals("44")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.BUSINESS_INVESTMENT_LOAN, orgId);
        } else if (productCode.equals("45")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.SCHOOL_FEES_LOAN, orgId);
        } else if (productCode.equals("46")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.REAL_ESTATE_LOAN, orgId);
        } else if (productCode.equals("47")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.OVERDRAFT_LOAN, orgId);
        } else if (productCode.equals("48")) {
            return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.NJANGI_FINANCING, orgId);
        }
        return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.NO_NAME, orgId);
    }

    public LedgerAccount determineLedgerAccount(AccountType accountType, long orgId) {

//        if(StringUtils.isNotEmpty(accountNumber) && accountNumber.length()!=2){
//            productCode = accountNumber.substring(3, 5);
//        }
//        LedgerAccount byCodeAndOrgIdAndActiveTrue = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(productCode, orgId);
        LedgerAccount byNameAndOrgIdAndActiveTrue = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(accountType.getName(), orgId);
        if (byNameAndOrgIdAndActiveTrue != null) {
            return byNameAndOrgIdAndActiveTrue;
        }
        LedgerAccount byCodeAndOrgIdAndActiveTrue = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(accountType.getCode(), orgId);
        if (byCodeAndOrgIdAndActiveTrue != null) {
            return byCodeAndOrgIdAndActiveTrue;
        }

        return null;
    }


//    public LedgerAccount determineInterestLedgerAccount(AccountType accountType, long orgId){
//        String productCode = accountNumber;
//        if( StringUtils.isNotEmpty(accountNumber) && accountNumber.length() != 2 ){
//
//            productCode = accountNumber.substring(3, 5);
//
//            if (productCode.equals("41")) {
//
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.SHORT_TERM_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("42")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CONSUMPTION_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("43")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.AGRICULTURE_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("44")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.BUSINESS_INVESTMENT_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("45")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.SCHOOL_FEES_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("46")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.REAL_ESTATE_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("47")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.OVERDRAFT_LOAN_INTEREST, orgId);
//            } else if (productCode.equals("48")) {
//                return ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.NJANGI_FINANCING_LOAN_INTEREST, orgId);
//            }
//
//        }
//        return null;
//    }


    public void deleteTransactions(String reference, long orgId) {

        Optional<SavingAccountTransaction> savingByReferenceAndOrgId = savingAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);
        Optional<CurrentAccountTransaction> currentByReferenceOrgId = currentAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);
        Optional<LoanAccountTransaction> loanByReferenceAndOrgId = loanAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);
        Optional<DailySavingAccountTransaction> dailySavingByReferenceAndOrgId = dailySavingAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);
        Optional<ShareAccountTransaction> shareByReferenceAndOrgId = shareAccountTransactionRepository.findByReferenceAndOrgId(reference, orgId);

        if (savingByReferenceAndOrgId.isPresent()) {
            SavingAccountTransaction savingAccountTransaction = savingByReferenceAndOrgId.get();
            SavingAccount savingAccount = savingAccountTransaction.getSavingAccount();
            List<SavingAccountTransaction> savingAccountTransactions = savingAccount.getSavingAccountTransaction();
            savingAccountTransactions.remove(savingAccountTransaction);
            savingAccountRepository.save(savingAccount);
            savingAccountTransaction.setSavingAccount(null);
            savingAccountTransactionRepository.save(savingAccountTransaction);
            savingAccountTransactionRepository.delete(savingAccountTransaction);
        } else if (currentByReferenceOrgId.isPresent()) {
            CurrentAccountTransaction currentAccountTransaction = currentByReferenceOrgId.get();
            CurrentAccount currentAccount = currentAccountTransaction.getCurrentAccount();
            List<CurrentAccountTransaction> currentAccountTransactions = currentAccount.getCurrentAccountTransaction();
            currentAccountTransactions.remove(currentAccountTransaction);
            currentAccountRepository.save(currentAccount);
            currentAccountTransaction.setCurrentAccount(null);
            currentAccountTransactionRepository.save(currentAccountTransaction);
            currentAccountTransactionRepository.delete(currentAccountTransaction);
        } else if (loanByReferenceAndOrgId.isPresent()) {
            LoanAccountTransaction loanAccountTransaction = loanByReferenceAndOrgId.get();
            loanAccountTransaction.setLoanAccount(null);
            loanAccountTransactionRepository.save(loanAccountTransaction);
            loanAccountTransactionRepository.delete(loanAccountTransaction);
        } else if (dailySavingByReferenceAndOrgId.isPresent()) {
            DailySavingAccountTransaction dailySavingAccountTransaction = dailySavingByReferenceAndOrgId.get();
            dailySavingAccountTransaction.setDailySavingAccount(null);
            dailySavingAccountTransactionRepository.save(dailySavingAccountTransaction);
            dailySavingAccountTransactionRepository.delete(dailySavingAccountTransaction);
        } else if (shareByReferenceAndOrgId.isPresent()) {
            shareAccountTransactionRepository.delete(shareByReferenceAndOrgId.get());
        }

    }


    public Iterable<LedgerAccount> getLedgerAccounts(User user) {
        List<LedgerAccount> ledgerAccountList = new ArrayList<LedgerAccount>();
        Iterable<LedgerAccount> all;
        LedgerAccount ledgerAccount;

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.LOAN);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setCode(BVMicroUtils.LOAN + "_" + BVMicroUtils.GL_3001);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.CASH);
        ledgerAccount.setCategory("5000 – 5999");
        ledgerAccount.setCode(BVMicroUtils.CASH + "_" + BVMicroUtils.GL_5001);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.VAT);
        ledgerAccount.setCode(BVMicroUtils.VAT + "_" + BVMicroUtils.GL_4002);
        ledgerAccount.setCategory("4000 – 4999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.CURRENT);
        ledgerAccount.setCode(BVMicroUtils.CURRENT + "_" + BVMicroUtils.GL_3004);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.GENERAL_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.GENERAL_SAVINGS + "_" + BVMicroUtils.GL_3005);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.RETIREMENT_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.RETIREMENT_SAVINGS + "_" + BVMicroUtils.GL_3006);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.DAILY_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.DAILY_SAVINGS + "_" + BVMicroUtils.GL_3007);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.MEDICAL_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.MEDICAL_SAVINGS + "_" + BVMicroUtils.GL_3008);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);


        ledgerAccount = getPOSLedgerAccount(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SOCIAL_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.SOCIAL_SAVINGS + "_" + BVMicroUtils.GL_3010);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.BUSINESS_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.BUSINESS_SAVINGS + "_" + BVMicroUtils.GL_3011);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.CHILDREN_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.CHILDREN_SAVINGS + "_" + BVMicroUtils.GL_3012);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.REAL_ESTATE_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.REAL_ESTATE_SAVINGS + "_" + BVMicroUtils.GL_3013);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.EDUCATION_SAVINGS);
        ledgerAccount.setCode(BVMicroUtils.EDUCATION_SAVINGS + "_" + BVMicroUtils.GL_3014);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SHORT_TERM_LOAN);
        ledgerAccount.setCode(BVMicroUtils.SHORT_TERM_LOAN + "_" + BVMicroUtils.GL_3015);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SHORT_TERM_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.SHORT_TERM_LOAN_INTEREST + "_" + BVMicroUtils.GL_7015);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.CONSUMPTION_LOAN);
        ledgerAccount.setCode(BVMicroUtils.CONSUMPTION_LOAN + "_" + BVMicroUtils.GL_3016);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.CONSUMPTION_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.CONSUMPTION_LOAN_INTEREST + "_" + BVMicroUtils.GL_7016);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);


        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.AGRICULTURE_LOAN);
        ledgerAccount.setCode(BVMicroUtils.AGRICULTURE_LOAN + "_" + BVMicroUtils.GL_3017);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.AGRICULTURE_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.AGRICULTURE_LOAN_INTEREST + "_" + BVMicroUtils.GL_7017);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.BUSINESS_INVESTMENT_LOAN);
        ledgerAccount.setCode(BVMicroUtils.BUSINESS_INVESTMENT_LOAN + "_" + BVMicroUtils.GL_3018);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.BUSINESS_INVESTMENT_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.BUSINESS_INVESTMENT_LOAN_INTEREST + "_" + BVMicroUtils.GL_7018);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SCHOOL_FEES_LOAN);
        ledgerAccount.setCode(BVMicroUtils.SCHOOL_FEES_LOAN + "_" + BVMicroUtils.GL_3019);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SCHOOL_FEES_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.SCHOOL_FEES_LOAN_INTEREST + "_" + BVMicroUtils.GL_7019);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);


        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.REAL_ESTATE_LOAN);
        ledgerAccount.setCode(BVMicroUtils.REAL_ESTATE_LOAN + "_" + BVMicroUtils.GL_3020);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.REAL_ESTATE_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.REAL_ESTATE_LOAN_INTEREST + "_" + BVMicroUtils.GL_7020);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);


        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.OVERDRAFT_LOAN);
        ledgerAccount.setCode(BVMicroUtils.OVERDRAFT_LOAN + "_" + BVMicroUtils.GL_3021);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.OVERDRAFT_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.OVERDRAFT_LOAN_INTEREST + "_" + BVMicroUtils.GL_7021);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);


        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.NJANGI_FINANCING);
        ledgerAccount.setCode(BVMicroUtils.NJANGI_FINANCING + "_" + BVMicroUtils.GL_3022);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("false");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.NJANGI_FINANCING_LOAN_INTEREST);
        ledgerAccount.setCategory("7000 – 7999");
        ledgerAccount.setCode(BVMicroUtils.NJANGI_FINANCING_LOAN_INTEREST + "_" + BVMicroUtils.GL_7022);
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.SHARE);
        ledgerAccount.setCode(BVMicroUtils.SHARE + "_" + BVMicroUtils.GL_5004);
        ledgerAccount.setCategory("5000 – 5999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.PREFERENCE_SHARE_TYPE);
        ledgerAccount.setCode(BVMicroUtils.PREFERENCE_SHARE + "_" + BVMicroUtils.GL_5005);
        ledgerAccount.setCategory("5000 – 5999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setOrgId(user.getOrgId());
        ledgerAccountList.add(ledgerAccount);

        Iterable<LedgerAccount> ledgerListIterable = ledgerAccountList;
        ledgerAccountRepository.saveAll(ledgerListIterable);
        all = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());
        return all;
    }

    @NotNull
    private LedgerAccount getPOSLedgerAccount(long orgID) {

        AccountType pointOFSale = new AccountType();
        pointOFSale.setNumber("39");
        pointOFSale.setOrgId(orgID);
        pointOFSale.setName(BVMicroUtils.POS_GL_3333);
        pointOFSale.setCategory(BVMicroUtils.SALES);
        pointOFSale.setDisplayName(BVMicroUtils.POINT_OF_SALE);
        pointOFSale.setActive(true);
        accountTypeRepository.save(pointOFSale);

        LedgerAccount ledgerAccount;
        ledgerAccount = new LedgerAccount();
        ledgerAccount.setName(BVMicroUtils.POS_GL_3333);
        ledgerAccount.setDisplayName(BVMicroUtils.POINT_OF_SALE);

        ledgerAccount.setCode(BVMicroUtils.POS_GL_3333);
        ledgerAccount.setCategory("3000 – 3999");
        ledgerAccount.setStatus(BVMicroUtils.ACTIVE);
        ledgerAccount.setCreditBalance("true");
        ledgerAccount.setCashAccountTransfer("true");
        ledgerAccount.setCashTransaction("true");
        ledgerAccount.setInterAccountTransfer("true");
        ledgerAccount.setActive(true);
        ledgerAccount.setCreatedDate(new Date());
        ledgerAccount.setCreatedBy(BVMicroUtils.INIT_SYSTEM);
        ledgerAccount.setOrgId(orgID);

        return ledgerAccount;
    }
}
