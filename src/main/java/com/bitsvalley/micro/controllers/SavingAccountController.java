package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.AccountTypeRepository;
import com.bitsvalley.micro.repositories.SavingAccountTransactionRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

//import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class SavingAccountController extends SuperController {

    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    CallCenterService callCenterService;

//    @Autowired
//    EmailSenderService emailSenderService;

    @Autowired
    SavingAccountTransactionRepository savingAccountTransactionRepository;

    @Autowired
    SavingAccountService savingAccountService;

    @Autowired
    LoanAccountService loanAccountService;

    @Autowired
    GeneralLedgerService generalLedgerService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PdfService pdfService;

    @Autowired
    BranchService branchService;

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    UserService userService;

    @Autowired
    CMRService cmrService;

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    @GetMapping(value = "/registerSavingAccount")
    public String registerSaving(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user == null) {
            return "findCustomer";
        }
        SavingAccount savingAccount = new SavingAccount();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        savingAccount.setAccountMinBalance(runtimeSetting.getSavingMinBalance());
        model.put("savingAccount", savingAccount);
        List<AccountType> byOrgIdAndCategory = accountTypeRepository.findByOrgIdAndCategoryAndActiveTrue(user.getOrgId(), BVMicroUtils.SAVINGS);
        model.put("accountTypes", byOrgIdAndCategory);
        return "savingAccount";
    }


    @PostMapping(value = "/registerSavingAccountForm")
    public String registerSavingAccount(@ModelAttribute("saving") SavingAccount savingAccount, ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        user = userRepository.findById(user.getId()).get();
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());//TODO Create branch repo
        savingAccount.setBranchCode(branchInfo.getCode());
        savingAccount.setCountry(branchInfo.getCountry());
        savingAccountService.createSavingAccount(savingAccount, user);
        return findUserByUserName(user, model, request);
    }


    @GetMapping(value = "/registerSavingAccountTransaction/{id}")
    public String registerSavingAccountTransaction(@PathVariable("id") long id, ModelMap model, HttpServletResponse response, Integer Error, Integer emailSent, String countryCode) {
        SavingAccountTransaction savingAccountTransaction = new SavingAccountTransaction();
        //Error and email sent arguments get passed in by savingEmailPDF() function when a user
        //emails themself a statement pdf. If they selected an invalid interval emailSent == 0 and
        //Error == 1. The variables are used to decide what message to put in the model for thymeleaf.
        if(Error == null && emailSent == null){
            Error = 0;
            emailSent = 0;
        }
        return displaySavingBilanzNoInterest(id, model, savingAccountTransaction, Error, emailSent, countryCode);
    }

    private String displaySavingBilanzNoInterest(long id, ModelMap model, SavingAccountTransaction savingAccountTransaction, Integer Error, Integer emailSent, String countryCode) {
        Optional<SavingAccount> savingAccount = savingAccountService.findById(id);
        SavingAccount aSavingAccount = savingAccount.get();
        List<SavingAccountTransaction> savingAccountTransactionList = aSavingAccount.getSavingAccountTransaction();
        SavingBilanzList savingBilanzByUserList = savingAccountService.calculateAccountBilanz(savingAccountTransactionList, false, countryCode);
        model.put("name", getLoggedInUserName());
//        Collections.reverse(savingBilanzByUserList.getSavingBilanzList());
        model.put("savingBilanzList", savingBilanzByUserList);
        if(emailSent==1){
            model.put("emailSent", true);
        }else{ model.put("emailSent", false); }
        if(Error ==1){
            model.put("invalidInterval", true);
        }else{ model.put("invalidInterval", false); }
        savingAccountTransaction.setSavingAccount(aSavingAccount);
        savingAccountTransaction.setAccountBalance(aSavingAccount.getAccountBalance());
        model.put("savingAccountTransaction", savingAccountTransaction);
        model.put("glSearchDTO", new GLSearchDTO());

        return "savingBilanzNoInterest";
    }

    @GetMapping(value = "/statementPDF/{id}")
    public void generateStatementPDF(@PathVariable("id") long id, ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        response.setHeader("Content-disposition","attachment;filename="+ id+"_statement.pdf");
//            OutputStream responseOutputStream = response.getOutputStream();
        Optional<SavingAccount> savingAccount = savingAccountService.findById(new Long(id));
        SavingBilanzList savingBilanzByUserList = savingAccountService.
                calculateAccountBilanz(savingAccount.get().getSavingAccountTransaction(),false,runtimeSetting.getCountryCode() );
        String htmlInput = pdfService.generatePDFSavingBilanzList(savingBilanzByUserList, savingAccount.get(),runtimeSetting.getLogo(), initSystemService.findByOrgId(user.getOrgId()) );
        generateByteOutputStream(response, htmlInput);
    }

    @PostMapping(value = "/statementPDFInterval/{id}")
    public void generateStatementPDFInterval(@PathVariable("id") long id, ModelMap model, HttpServletRequest request, HttpServletResponse response, @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        response.setHeader("Content-disposition","attachment;filename="+ id+"_statement.pdf");
//            OutputStream responseOutputStream = response.getOutputStream();
        Optional<SavingAccount> savingAccount = savingAccountService.findById(new Long(id));
        SavingBilanzList savingBilanzByUserList = savingAccountService.
                calculateAccountBilanzInterval(new Long(id),glSearchDTO,false, user.getOrgId(), runtimeSetting.getCountryCode());
        String htmlInput = pdfService.generatePDFSavingBilanzListInterval(savingBilanzByUserList, savingAccount.get(),runtimeSetting.getLogo(), initSystemService.findByOrgId(user.getOrgId()), glSearchDTO);
        generateByteOutputStream(response, htmlInput);
    }


    @PostMapping(value = "/updateMinSBalance")
    public String updateMinSBalance(HttpServletRequest request, ModelMap model ) {
        String id = request.getParameter("sBalanceId");
        String newMinBalance =  request.getParameter("minSavingBalance");
        SavingAccount savingAccount = savingAccountRepository.findById(Long.parseLong(id)).get();
        savingAccount.setAccountMinBalance(new Double(newMinBalance));
        savingAccountRepository.save(savingAccount);
        callCenterService.saveCallCenterLog("", savingAccount.getUser().getUserName(), newMinBalance, BVMicroUtils.AMOUNT_ON_HOLD_BALANCE_CHANGED +savingAccount.getAccountNumber());
        model.put("minBalAccountInfo", "Account on hold balance Updated" );
        return "userHome";
    }


    @PostMapping(value = "/savingStatementPDFInterval/{id}")
    public String savingEmailPDF(@PathVariable("id") long id, ModelMap model, @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO, HttpServletRequest request,
//                                 HttpServletResponse response) throws IOException, MessagingException {
                                 HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        Optional<SavingAccount> savingAccount = savingAccountService.findById(new Long(id));
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        SavingBilanzList savingBilanzByUserList = savingAccountService.
                calculateAccountBilanzInterval(Long.valueOf(id), glSearchDTO, false, user.getOrgId(), runtimeSetting.getCountryCode());
        if (savingAccountTransactionRepository.searchStartEndDateFilter(glSearchDTO.getStartDate() + " 00:00:00.000",
                glSearchDTO.getEndDate() + " 23:59:59.999", id, user.getOrgId()).size() == 0) {
            return registerSavingAccountTransaction(id, model, response, 1, 0, runtimeSetting.getCountryCode());
        }
        String htmlInput = pdfService.generatePDFSavingBilanzListInterval(savingBilanzByUserList, savingAccount.get(), runtimeSetting.getLogo(), initSystemService.findByOrgId(user.getOrgId()), glSearchDTO);
        File outputFile = new File(id + "_statement.pdf");
        HtmlConverter.convertToPdf(htmlInput, new PdfWriter(id + "_statement.pdf"));
        String emailBody = "Hello,\n\n" + "Click on the attached file to view each processed transactions for " +
                savingAccount.get().getUser().getGender() + ". " + savingAccount.get().getUser().getFirstName() + " " +
                savingAccount.get().getUser().getLastName() + "'s saving account.";
//        emailSenderService.emailMe(runtimeSetting.getEmail(), emailBody, "Processed Transactions",
//                outputFile, id + "_statement.pdf");
        return registerSavingAccountTransaction(id, model, response, 0, 1, runtimeSetting.getCountryCode());
    }


    @GetMapping(value = "/transferFromCurrentToLoanAccountsForm")
    public String transferBetweenAccounts(ModelMap model,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        TransferBilanz transferBilanz = new TransferBilanz();
        transferBilanz.setTransferType(BVMicroUtils.CURRENT_LOAN_TRANSFER);
        model.put("transferBilanz", transferBilanz);
        return "transfer";
    }

    @GetMapping(value = "/shortOfMinimum")
    public void shortOfMinimum(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = userRepository.findByUserName(getLoggedInUserName());
        response.setHeader("Content-disposition", "attachment;filename=" +"short_of_min_balance.pdf");
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        List<SavingAccount> orgSavingAccounts = savingAccountService.findByOrgId(user.getOrgId());
        List<SavingAccount> accsNotMeetingRequireMin = orgSavingAccounts.stream()
                .filter(account -> account.getAccountMinBalance()-account.getAccountBalance()>0).collect(Collectors.toList());
        String htmlInput = pdfService.generateShortOfMinimum(accsNotMeetingRequireMin, runtimeSetting, user);
        generateByteOutputStream(response, htmlInput);
    }

    @GetMapping(value = "/transferFromCurrentToDebitForm")
    public String transferFromCurrentToDebitForm(ModelMap model,
                                                 HttpServletRequest request) {

        if (getUserInUse(model, request)) return "findCustomer";
        TransferBilanz transferBilanz = new TransferBilanz();
        transferBilanz.setTransferType(BVMicroUtils.CURRENT_DEBIT_TRANSFER);
        model.put("transferBilanz", transferBilanz );
        return "transferCurrentToDebit";
    }

    @GetMapping(value = "/transferFromCurrentToCurrentForm")
    public String transferFromCurrentToCurrentForm(ModelMap model,
                                                   HttpServletRequest request) {

        if (getUserInUse(model, request)) return "findCustomer";
        TransferBilanz transferBilanz = new TransferBilanz();
        transferBilanz.setTransferType(BVMicroUtils.CURRENT_CURRENT_TRANSFER);
        model.put("transferBilanz", transferBilanz );
        return "transferCurrentToCurrent";
    }


    @GetMapping(value = "/transferFromDebitToDebitForm")
    public String transferFromDebitToDebitForm(ModelMap model,
                                               HttpServletRequest request) {

        if (getUserInUse(model, request)) return "findCustomer";
        TransferBilanz transferBilanz = new TransferBilanz();
        transferBilanz.setTransferType(BVMicroUtils.DEBIT_DEBIT_TRANSFER);
        model.put("transferBilanz", transferBilanz );
        return "transferDebitToDebit";
    }

    private boolean getUserInUse(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user == null) {
            model.addAttribute("user", new User());
            return true;
        }
        try {
            user.getSavingAccount().size();
        } catch (RuntimeException exp) {
            model.addAttribute("user", new User());
            return true;
        }
        return false;
    }

    @GetMapping(value = "/transferFromDebitToCurrentForm")
    public String transferFromDebitToCurrentForm(ModelMap model,
                                                 HttpServletRequest request) {

        if (getUserInUse(model, request)) return "findCustomer";
        TransferBilanz transferBilanz = new TransferBilanz();
        transferBilanz.setTransferType(BVMicroUtils.DEBIT_CURRENT_TRANSFER);
        model.put("transferBilanz", transferBilanz );
        return "transferDebitToCurrent";
    }


    @PostMapping(value = "/transferb2bAccount")
    public String transferFromAccountToAccount(ModelMap model,
                                               @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        //Validate transfer amount is available
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        model.put("fromTransferText",transferBilanz.getTransferFromAccount() );
        model.put("toTransferText",transferBilanz.getTransferToAccount() );
        model.put("transferAmount",BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(),runtimeSetting.getCountryCode()) );
        model.put("notes", transferBilanz.getNotes());
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());

        if(transferBilanz.getTransferType().equals(BVMicroUtils.CURRENT_LOAN_TRANSFER)) {

            CurrentAccount currentAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(), loggedInUser.getOrgId());
            LoanAccount byAccountNumber = loanAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(),loggedInUser.getOrgId());
            if( transferBilanz.getTransferAmount() < 1){
                model.put("error", "Transfer amount not valid");
                model.put("transferBilanz",transferBilanz);
                return "transfer";
            }
//            TODO: Loan account transfer Current accoutn can go in the minus
//            if(currentAccount.getAccountBalance() < transferBilanz.getTransferAmount()){
//                model.put("error", "Account Balance cannot be lower than transfer amount");
//                model.put("transferBilanz",transferBilanz);
//                return "transfer";
//            }
            LoanBilanz loanBilanz = savingAccountService.transferFromCurrentToLoan(currentAccount,
                    byAccountNumber,
                    transferBilanz.getTransferAmount(), transferBilanz.getNotes());

            if(loanBilanz.getMaximumPayment() == -1 ){
                model.put("error", loanBilanz.getWarningMessage() + BVMicroUtils.formatCurrency(loanBilanz.getWarningAmount(),runtimeSetting.getCountryCode()));
                model.put("transferBilanz",transferBilanz);
                return "transfer";
            }

            if(loanBilanz.getMinimumPayment() == -1){
                model.put("error", loanBilanz.getWarningMessage() + BVMicroUtils.formatCurrency(loanBilanz.getWarningAmount(),runtimeSetting.getCountryCode()));
                model.put("transferBilanz",transferBilanz);
                return "transfer";
            }

//            if(!StringUtils.equals("true", loanBilanz.getWarningMessage())){
//                model.put("error", loanBilanz.getWarningMessage());
//                model.put("transferBilanz",transferBilanz);
//                return "transfer";
//            }

        }else if(transferBilanz.getTransferType().equals(BVMicroUtils.DEBIT_DEBIT_TRANSFER)) {
            savingAccountService.transferFromDebitToDebit(transferBilanz.getTransferFromAccount(),
                    transferBilanz.getTransferToAccount(),
                    transferBilanz.getTransferAmount(), transferBilanz.getNotes(), loggedInUser.getOrgId(), runtimeSetting);

        }else if(transferBilanz.getTransferType().equals(BVMicroUtils.DEBIT_CURRENT_TRANSFER)) {
            savingAccountService.transferFromDebitToCurrent(transferBilanz.getTransferFromAccount(),
                    transferBilanz.getTransferToAccount(),
                    transferBilanz.getTransferAmount(), transferBilanz.getNotes(), loggedInUser.getOrgId(), runtimeSetting);
        }else if(transferBilanz.getTransferType().equals(BVMicroUtils.CURRENT_DEBIT_TRANSFER)) {
            savingAccountService.transferFromCurrentToDebit(transferBilanz.getTransferFromAccount(),
                    transferBilanz.getTransferToAccount(),
                    transferBilanz.getTransferAmount(), transferBilanz.getNotes(), loggedInUser.getOrgId(), runtimeSetting);
        }else if(transferBilanz.getTransferType().equals(BVMicroUtils.CURRENT_CURRENT_TRANSFER)) {

            User customerInUse = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

            Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            if (authorities.size() == 1 && StringUtils.equals(authorities.iterator().next().toString(), BVMicroUtils.ROLE_CUSTOMER)) {
                if (!userService.validateBalaanzPin(customerInUse.getId(), transferBilanz.getTerminalCode())) {

                    userRepository.findByUserName(getLoggedInUserName());
                    model.put("error", "Re-enter PIN. Not valid");
                    return "transferReview";
                }
            }
                savingAccountService.transferFromCurrentToCurrent(transferBilanz.getTransferFromAccount(),
                    transferBilanz.getTransferToAccount(),
                    transferBilanz.getTransferAmount(), transferBilanz.getNotes(), loggedInUser.getOrgId(), runtimeSetting);

//            CurrentAccount byAccountNumberAndOrgId = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(), loggedInUser.getOrgId());

        }

        return "transferConfirm";
    }



    @PostMapping(value = "/transferFromCurrentToCurrentFormReview")
    public String transferFromCurrentToCurrentFormReview(ModelMap model,
                                                         @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        CurrentAccount toAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(), loggedInUser.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        if(null==toAccount){
            model.put("invalidToAccount","Please make sure Account Number is valid" );
            return "transferCurrentToCurrent";
        }
        model.put("transferBilanz", transferBilanz);
        CurrentAccount fromAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(), loggedInUser.getOrgId());

        model.put("transferType", transferBilanz.getTransferType());
        model.put("fromTransferText","Balance " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +" --- Minimum Balance "+ BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("toTransferText", BVMicroUtils.getFullName(toAccount.getUser()) );
        model.put("transferAmount", BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(), runtimeSetting.getCountryCode()));
        model.put("notes", transferBilanz.getNotes());
        return "transferReview";
    }

    @PostMapping(value = "/transferFromCurrentToDebitFormReview")
    public String transferFromCurrentToDebitFormReview(ModelMap model,
                                                       @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        SavingAccount toAccount = savingAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(),loggedInUser.getOrgId());
        if(null==toAccount){
            model.put("invalidToAccount","Please make sure Account Number is valid" );
            return "transferCurrentToCurrent";
        }
        model.put("transferBilanz", transferBilanz);
        CurrentAccount fromAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(), loggedInUser.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        model.put("transferType", transferBilanz.getTransferType());
        model.put("fromTransferText","Balance: " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(), runtimeSetting.getCountryCode()) +" Minimum Balance: "+ BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("toTransferText", BVMicroUtils.getFullName(toAccount.getUser()) + " "+toAccount.getAccountType().getName() +" Balance: " + BVMicroUtils.formatCurrency(toAccount.getAccountBalance(), runtimeSetting.getCountryCode()) +"Minimum Balance: "+ BVMicroUtils.formatCurrency(toAccount.getAccountMinBalance(), runtimeSetting.getCountryCode()) );
        model.put("transferAmount", BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(), runtimeSetting.getCountryCode()));
        model.put("notes", transferBilanz.getNotes());
        return "transferReview";
    }

    @PostMapping(value = "/transferFromDebitToDebitFormReview")
    public String transferFromDebitToDebitFormReview(ModelMap model,
                                                     @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());

        SavingAccount toAccount = savingAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(),loggedInUser.getOrgId());
        if(null==toAccount){
            model.put("invalidToAccount","Please make sure Account Number is valid" );
            return "transferDebitToDebit";
        }
        model.put("transferBilanz", transferBilanz);
        SavingAccount fromAccount = savingAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(),loggedInUser.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        model.put("transferType", transferBilanz.getTransferType());
        model.put("fromTransferText",fromAccount.getAccountType().getName() +" Balance " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +"Minimum Balance: "+ BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("toTransferText",BVMicroUtils.getFullName(toAccount.getUser()) +"  "+toAccount.getAccountType().getName() +" Balance: " + BVMicroUtils.formatCurrency(toAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +" Minimum Balance: "+ BVMicroUtils.formatCurrency(toAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("transferAmount", BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(),runtimeSetting.getCountryCode()));
        model.put("notes", transferBilanz.getNotes());
        return "transferReview";
    }

    @PostMapping(value = "/transferFromDebitToCurrentFormReview")
    public String transferFromDebitToCurrentFormReview(ModelMap model,
                                                       @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());

        CurrentAccount toAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(),loggedInUser.getOrgId());
        if(null==toAccount){
            model.put("invalidToAccount","Please make sure Account Number is valid" );
            return "transferDebitToCurrent";
        }
        model.put("transferBilanz", transferBilanz);
        SavingAccount fromAccount = savingAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(),loggedInUser.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        model.put("transferType", transferBilanz.getTransferType());
        model.put("fromTransferText",fromAccount.getAccountType().getName() +"Balance: " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +"Minimum Balance: "+ BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("toTransferText",BVMicroUtils.getFullName(toAccount.getUser()) + " Balance: " + BVMicroUtils.formatCurrency(toAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +"Minimum Balance: "+ BVMicroUtils.formatCurrency(toAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("transferAmount", BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(),runtimeSetting.getCountryCode()));
        model.put("notes", transferBilanz.getNotes());
        return "transferReview";
    }

    @PostMapping(value = "/transferFromCurrentToLoanAccountsFormReview")
    public String transferFromCurrentToLoanAccountsFormReview(ModelMap model,
                                                              @ModelAttribute("transferBilanz") TransferBilanz transferBilanz, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        model.put("transferBilanz", transferBilanz);
        CurrentAccount fromAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(),loggedInUser.getOrgId());
        LoanAccount toAccount = loanAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferToAccount(),loggedInUser.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        model.put("transferType", BVMicroUtils.CURRENT_LOAN_TRANSFER);
        model.put("fromTransferText", "Balance " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(),runtimeSetting.getCountryCode()) +" Minimum Balance: "+ BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(),runtimeSetting.getCountryCode()) );
        model.put("toTransferText", BVMicroUtils.getFullName(toAccount.getUser()) +" "+ toAccount.getAccountType().getName() +" Balance: " + BVMicroUtils.formatCurrency(toAccount.getCurrentLoanAmount(),runtimeSetting.getCountryCode()) +" Initial Loan: "+ BVMicroUtils.formatCurrency(toAccount.getLoanAmount(),runtimeSetting.getCountryCode()) );
        model.put("transferAmount", BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(),runtimeSetting.getCountryCode()) );
        model.put("notes", transferBilanz.getNotes());

        return "transferReview";
    }



    @PostMapping(value = "/registerSavingAccountTransactionForm")
    public String registerSavingAccountTransactionForm(ModelMap model, @ModelAttribute("savingAccountTransaction") SavingAccountTransaction savingAccountTransaction, HttpServletRequest request) {

        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        savingAccountTransaction.setOrgId(user.getOrgId());
        getRepresentative(savingAccountTransaction, user);
        String savingAccountId = request.getParameter("savingAccountId");
        SavingAccount savingAccount = savingAccountService.findById(Long.parseLong(savingAccountId)).get();
        savingAccountTransaction.setSavingAccount(savingAccount);
        String deposit_withdrawal = request.getParameter("deposit_withdrawal");
        String error = "";
        savingAccountTransaction.setWithdrawalDeposit(1);
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        if(StringUtils.isEmpty( savingAccountTransaction.getModeOfPayment() ) ){
            error = "Select Method of Payment - MOP";
        }
        else if(StringUtils.isEmpty(deposit_withdrawal)){
            error = "Select Transaction Type";
        }

        if (deposit_withdrawal.equals("WITHDRAWAL")) {
            savingAccountTransaction.setSavingAmount(savingAccountTransaction.getSavingAmount() * -1);
            savingAccountTransaction.setWithdrawalDeposit(-1);
            error = savingAccountService.withdrawalAllowed(savingAccountTransaction);
//            debitCredit = BVMicroUtils.DEBIT;
            //Make sure min amount is not violated at withdrawal
        }

        if (!StringUtils.isEmpty(error)) {
            model.put("billSelectionError", error);
            savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());
            return displaySavingBilanzNoInterest(Long.parseLong(savingAccountId), model, savingAccountTransaction,0,0, runtimeSetting.getCountryCode());
        }
        //
        if((savingAccountTransaction.getSavingAmount() + savingAccountTransaction.getSavingAccount().getAccountBalance() ) < savingAccountTransaction.getSavingAccount().getAccountMinBalance()){
            savingAccount.setDefaultedPayment(true);// Minimum balance check
//            model.put("billSelectionError", "Please make minimum payment of "+ BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAccount().getMinimumPayment(),runtimeSetting.getCountryCode()));
//            savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());
//            return displaySavingBilanzNoInterest(Long.parseLong(savingAccountId), model, savingAccountTransaction,0,0, runtimeSetting.getCountryCode());
        }
        if ("CASH".equals(savingAccountTransaction.getModeOfPayment()) && "true".equals(runtimeSetting.getBillSelectionEnabled()) ) {
            if (!checkBillSelectionMatchesEnteredAmount(savingAccountTransaction)) {
                model.put("billSelectionError", "Bills Selection does not match entered amount");
                savingAccountTransaction.setNotes(savingAccountTransaction.getNotes());
                return displaySavingBilanzNoInterest(Long.parseLong(savingAccountId), model, savingAccountTransaction,0,0, runtimeSetting.getCountryCode());
            }
        }

        String modeOfPayment = request.getParameter("modeOfPayment");
        savingAccountTransaction.setModeOfPayment(modeOfPayment);
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());

        savingAccountTransaction.setBranch(branchInfo.getId());
        savingAccountTransaction.setBranchCode(branchInfo.getCode());
        savingAccountTransaction.setBranchCountry(branchInfo.getCountry());

        savingAccountService.createSavingAccountTransaction(savingAccountTransaction, savingAccount, runtimeSetting);

        generalLedgerService.updateGLAfterCashSavingAccountTransaction(savingAccountTransaction);
        String username = getLoggedInUserName();
        callCenterService.saveCallCenterLog(savingAccountTransaction.getReference(),
                savingAccount.getUser().getUserName(), savingAccount.getAccountNumber(),
                savingAccount.getAccountType().getDisplayName()+" transaction made "+ BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(),runtimeSetting.getCountryCode()));

//        notificationService.sampleDisbursement(savingAccountTransaction.getSavingAmount(), String.valueOf(savingAccount.getOrgId()), " ", savingAccount.getUser().getEmail() );

        SavingBilanzList savingBilanzByUserList = savingAccountService.calculateAccountBilanz(savingAccount.getSavingAccountTransaction(), false, runtimeSetting.getCountryCode());
        model.put("name", username );
        model.put("billSelectionInfo", BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(),runtimeSetting.getCountryCode()) + " ---- PAYMENT HAS REGISTERED ----- ");
        model.put("savingBilanzList", savingBilanzByUserList);
        request.getSession().setAttribute("savingBilanzList", savingBilanzByUserList);
        Optional<User> byId = userRepository.findById(user.getId());
        request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, byId.get());
        savingAccountTransaction.setSavingAccount(savingAccount);
        resetSavingsAccountTransaction(savingAccountTransaction); //reset BillSelection and amount
        savingAccountTransaction.setNotes("");
        model.put("savingAccountTransaction", savingAccountTransaction);
        model.put("glSearchDTO", new GLSearchDTO());
        return "savingBilanzNoInterest";

    }


    @PostMapping("/registerSavingAccountTransactionCCForm")
    public String checkoutCC(ModelMap model, @ModelAttribute("savingAccountTransaction") SavingAccountTransaction savingAccountTransaction,
                             HttpServletRequest request) {

        String savingAccountId = request.getParameter("savingAccountId");
        SavingAccount savingAccount = savingAccountService.findById(new Long(savingAccountId)).get();
        savingAccountTransaction.setSavingAccount(savingAccount);
        request.getSession().setAttribute("ccSavingAccountTransaction", savingAccountTransaction);
//        String stripePublicKey = "pk_live_51IMZEuCZhPDaR6Ktl0ekDnlehEloR2xElhYYZjOmCkbS7RpAy9vJ5pxOzg2cwq8YeHum0EFNfvrjUJUxKlhGUjt600sbV5M2UT";
        model.addAttribute("amount", new Double(savingAccountTransaction.getSavingAmount()).intValue()); // in cents
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("currency", "USD");
        model.addAttribute("description", "Making a payment into the saving account");
        model.put("savingAccountTransaction",savingAccountTransaction);
        return "ccSavingCheckout";
    }

    private void getRepresentative(SavingAccountTransaction savingAccountTransaction, User user) {
        if(null == savingAccountTransaction.getAccountOwner()){
            savingAccountTransaction.setAccountOwner("false");
        }
        if (StringUtils.isEmpty(savingAccountTransaction.getRepresentative())) {
            savingAccountTransaction.setRepresentative(BVMicroUtils.getFullName(user));
        }
    }

    @GetMapping(value = "/showUserSavingBilanz/{id}")
    public String showUserSavingBilanz(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        SavingBilanzList savingBilanzByUserList = savingAccountService.getSavingBilanzByUser(user, true, runtimeSetting.getCountryCode());
        model.put("name", getLoggedInUserName());
        model.put("savingBilanzList", savingBilanzByUserList);
        return "savingBilanz";
    }

    @GetMapping(value = "/showSavingAccountBilanz/{accountId}")
    public String showSavingAccountBilanz(@PathVariable("accountId") long accountId, ModelMap model, HttpServletRequest request) {
        Optional<SavingAccount> byId = savingAccountService.findById(accountId);
        List<SavingAccountTransaction> savingAccountTransaction = byId.get().getSavingAccountTransaction();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        SavingBilanzList savingBilanzByUserList = savingAccountService.calculateAccountBilanz(savingAccountTransaction, true, runtimeSetting.getCountryCode() );
        model.put("name", getLoggedInUserName());
        model.put("savingBilanzList", savingBilanzByUserList);
        return "savingBilanz";
    }


    private void resetSavingsAccountTransaction(SavingAccountTransaction sat) {
        sat.setSavingAmount(0);
        sat.setFifty(0);
        sat.setFiveHundred(0);
        sat.setFiveThousand(0);
        sat.setOneHundred(0);
        sat.setOneThousand(0);
        sat.setTenThousand(0);
        sat.setTwentyFive(0);
        sat.setTwoThousand(0);
    }

    private boolean checkBillSelectionMatchesEnteredAmount(SavingAccountTransaction sat) {

        double selection = (sat.getTenThousand() * 10000) +
                (sat.getFiveThousand() * 5000) +
                (sat.getTwoThousand() * 2000) +
                (sat.getOneThousand() * 1000) +
                (sat.getFiveHundred() * 500) +
                (sat.getOneHundred() * 100) +
                (sat.getFifty() * 50) +
                (sat.getTwentyFive() * 25) +
                (sat.getTen() * 10) +
                (sat.getFive() * 5) +
                (sat.getOne() * 1);

        boolean match = (sat.getSavingAmount() == selection) || (sat.getSavingAmount()*-1 == selection) ;

        if (match) {
            sat.setNotes(sat.getNotes()
                    + addBillSelection(sat));
        }
        return match;
    }

    private String addBillSelection(SavingAccountTransaction sat) {
        String billSelection = " BS \n";
        billSelection = billSelection + concatBillSelection(" 10 000x", sat.getTenThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 5 000x", sat.getFiveThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 2 000x", sat.getTwoThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 1 000x", sat.getOneThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 500x", sat.getFiveHundred()) + "\n";
        billSelection = billSelection + concatBillSelection(" 100x", sat.getOneHundred()) + "\n";
        billSelection = billSelection + concatBillSelection(" 50x", sat.getFifty());
        billSelection = billSelection + concatBillSelection(" 25x", sat.getTwentyFive());
        billSelection = billSelection + concatBillSelection(" 10x", sat.getTen()) + "\n";
        billSelection = billSelection + concatBillSelection(" 5x", sat.getFive()) + "\n";
        billSelection = billSelection + concatBillSelection(" 1x", sat.getOne());
        return billSelection;
    }

    private String concatBillSelection(String s, int qty) {
        if (qty == 0) {
            return "";
        }
        s = s + qty;
        return s;
    }

    @GetMapping(value = "/requestMOMO")
    public String requestMOMO() throws JsonProcessingException {
//        CollectionRequestStatus collectionRequestStatus = cmrService.sendMomoRequest();
//        ObjectMapper om = new ObjectMapper();
//        om.writeValueAsString(collectionRequestStatus);
//        collectionRequestStatus.getStatus();
        return "login";
    }


//    TODO: Call to check account balance. Send token before
//    https://developer.folepay.com/mobilepay/client/api/payments/walletbalance/237100001

}