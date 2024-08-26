package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;

import com.bitsvalley.micro.repositories.CurrentAccountTransactionRepository;
import com.bitsvalley.micro.repositories.RuntimePropertiesRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.CurrentBilanzList;
import com.bitsvalley.micro.webdomain.GLSearchDTO;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class CurrentAccountController extends SuperController {

    @Autowired
    CurrentAccountTransactionRepository currentAccountTransactionRepository;

    @Autowired
    UserService userService;

    @Autowired
    CallCenterService callCenterService;

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    RuntimePropertiesRepository runtimePropertiesRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CurrentAccountTransactionService currentAccountTransactionService;

    @Autowired
    BranchService branchService;

    @Autowired
    PdfService pdfService;

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    @GetMapping(value = "/registerCurrentAccount")
    public String registerCurrent(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user == null) {
            return "findCustomer";
        }
        CurrentAccount currentAccount = new CurrentAccount();
        model.put("currentAccount", currentAccount);
        return "currentAccount";
    }


    @PostMapping(value = "/registerCurrentAccountForm")
    public String registerCurrentAccount(@ModelAttribute("current") CurrentAccount currentAccount, ModelMap model, HttpServletRequest request) {
        System.out.println("----------------               ------ calling  current - registerCurrentAccountForm ---------              --------------");
        System.out.println("----------------               -------calling  current - registerCurrentAccountForm -------              --------------");

        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        user = userRepository.findById(user.getId()).get();
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());//TODO Create branch repo
        currentAccountService.createCurrentAccount(currentAccount, user, branchInfo);

        return findUserByUserName(user, model, request);
    }


    @GetMapping(value = "/registerCurrentAccountTransaction/{id}")
    public String registerCurrentAccountTransaction(@PathVariable("id") long id, ModelMap model, RuntimeSetting rt) {
        CurrentAccountTransaction currentAccountTransaction = new CurrentAccountTransaction();
        return displayCurrentBilanzNoInterest(id, model, currentAccountTransaction, rt);
    }

    private String displayCurrentBilanzNoInterest(long id, ModelMap model, CurrentAccountTransaction currentAccountTransaction, RuntimeSetting rt) {
        CurrentAccount currentAccount = currentAccountService.findById(id).get();
        List<CurrentAccountTransaction> currentAccountTransactionList = currentAccount.getCurrentAccountTransaction();

        CurrentBilanzList currentBilanzByUserList = currentAccountService.calculateAccountBilanz(currentAccountTransactionList, false, rt);
        model.put("name", getLoggedInUserName());
        model.put("currentBilanzList", currentBilanzByUserList);

        currentAccountTransaction.setCurrentAccount(currentAccount);
        currentAccountTransaction.setAccountBalance(currentAccount.getAccountBalance());
        model.put("currentAccountTransaction", currentAccountTransaction);
        model.put("glSearchDTO", new GLSearchDTO());
        return "currentBilanzNoInterest";
    }


    @GetMapping(value = "/createCurrentAccountReceiptPdf/{id}")
    public void currentReceiptPDF(@PathVariable("id") long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_current_receipt.pdf");
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        CurrentAccountTransaction aCurrentAccountTransaction = currentAccountTransactionService.findById(new Long(id)).get();
        boolean displayBalance = containsAuthority(BVMicroUtils.ROLE_ACCOUNT_BALANCES);
        String htmlInput = pdfService.generateCurrentTransactionReceiptPDF(aCurrentAccountTransaction, initSystemService.findByOrgId(user.getOrgId()), displayBalance);
        generateByteOutputStream(response, htmlInput);
    }


    @GetMapping(value = "/statementCurrentPDF/{id}")
    public void generateStatementPDF(@PathVariable("id") long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_current_statement.pdf");
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        CurrentAccount currentAccount = currentAccountService.findById(Long.valueOf(id)).get();
        CurrentBilanzList currentBilanzByUserList = currentAccountService.
                calculateAccountBilanz(currentAccount.getCurrentAccountTransaction(), false, runtimeSetting);
        String htmlInput = pdfService.generatePDFCurrentBilanzList(currentBilanzByUserList,
                currentAccount, runtimeSetting.getLogo(),
                initSystemService.findByOrgId(user.getOrgId()));
        generateByteOutputStream(response, htmlInput);
    }


    @PostMapping(value = "/updateMinCBalance")
    public String updateMinCBalance(HttpServletRequest request, ModelMap model) {
        String id = request.getParameter("cBalanceId");
        String newMinBalance = request.getParameter("minCurrentBalance");
        CurrentAccount currentAccount = currentAccountService.findById(Long.parseLong(id)).get();
        currentAccount.setAccountMinBalance(Double.parseDouble(newMinBalance));
        currentAccountService.save(currentAccount);
        callCenterService.saveCallCenterLog("", currentAccount.getUser().getUserName(), newMinBalance, BVMicroUtils.AMOUNT_ON_HOLD_BALANCE_CHANGED + currentAccount.getAccountNumber());

        model.put("minBalAccountInfo", "Account On Hold balance Updated");
        return "userHome";
    }


    @PostMapping(value = "/statementCurrentPDFDateInterval/{id}")
    public void statementCurrentPDFDateInterval(@PathVariable("id") long id, @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO, ModelMap model,
                                                HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_current_statement.pdf");
        CurrentAccount currentAccount = currentAccountService.findById(new Long(id)).get();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        CurrentBilanzList currentBilanzByUserList = currentAccountService.calculateAccountBilanzInterval(false, glSearchDTO, currentAccount, runtimeSetting);
        String htmlInput = pdfService.generatePDFCurrentBilanzListInterval(currentBilanzByUserList,
                currentAccount, runtimeSetting.getLogo(),
                initSystemService.findByOrgId(user.getOrgId()), glSearchDTO);
        generateByteOutputStream(response, htmlInput);
    }

    @PostMapping("/registerCurrentAccountTransactionCCForm")
    public String checkoutCC(ModelMap model, @ModelAttribute("currentAccountTransaction") CurrentAccountTransaction currentAccountTransaction,
                             HttpServletRequest request) {

        String currentAccountId = request.getParameter("currentAccountId");
        CurrentAccount currentAccount = currentAccountService.findById(new Long(currentAccountId)).get();
        currentAccountTransaction.setCurrentAccount(currentAccount);

        BigDecimal bdAmount = new BigDecimal(currentAccountTransaction.getCurrentAmount());
        BigDecimal interest = new BigDecimal(.035);
        interest = bdAmount.multiply(interest);
        BigDecimal totalAmount = bdAmount.add(interest);
        String amountFormat = String.format("%.2f", totalAmount);
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_EVEN);
        model.addAttribute("stripeAmount", totalAmount.multiply(new BigDecimal(100)).doubleValue()); // in cents
        model.addAttribute("totalAmount", amountFormat); // in cents
        model.addAttribute("netAmount", Double.valueOf(currentAccountTransaction.getCurrentAmount()).intValue()); // in cents

        Integer amt = Double.valueOf(currentAccountTransaction.getCurrentAmount() * 100).intValue();
        model.addAttribute("amount", amt); // in cents
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("currency", "USD");
        model.addAttribute("description", "Making a payment into the checking account");
        model.put("currentAccountTransaction", currentAccountTransaction);
        return "ccCheckout";
    }

    @PostMapping(value = "/registerCurrentAccountTransactionForm")
    public String registerCurrentAccountTransactionForm(ModelMap model, @ModelAttribute("currentAccountTransaction") CurrentAccountTransaction currentAccountTransaction,
                                                        HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        getRepresentative(currentAccountTransaction, user);

        String currentAccountId = request.getParameter("currentAccountId");
        CurrentAccount currentAccount = currentAccountService.findById(new Long(currentAccountId)).get();
        currentAccountTransaction.setCurrentAccount(currentAccount);

        currentAccountTransaction.setWithdrawalDeposit(1);
        String error = "";
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        if ("CASH".equals(currentAccountTransaction.getModeOfPayment()) && "true".equals(runtimeSetting.getBillSelectionEnabled())) {
            if (!checkBillSelectionMatchesEnteredAmount(currentAccountTransaction)) {
                model.put("billSelectionError", "Bills Selection does not match entered amount");
                return displayCurrentBilanzNoInterest(Long.parseLong(currentAccountId), model, currentAccountTransaction, runtimeSetting);
            }
        }
        String deposit_withdrawal = request.getParameter("deposit_withdrawal");
        if (StringUtils.isEmpty(currentAccountTransaction.getModeOfPayment())) {
            error = "Select Method of Payment - MOP";
        } else if (StringUtils.isEmpty(deposit_withdrawal)) {
            error = "Select Transaction Type";
        }

        if (deposit_withdrawal.equals("WITHDRAWAL")) {
            currentAccountTransaction.setCurrentAmount(currentAccountTransaction.getCurrentAmount() * -1);
            currentAccountTransaction.setWithdrawalDeposit(-1);
            error = currentAccountService.withdrawalAllowed(currentAccountTransaction);
            //Make sure min amount is not violated at withdrawal
        }
        if (!StringUtils.isEmpty(error)) {
            model.put("billSelectionError", error);
            return displayCurrentBilanzNoInterest(Long.parseLong(currentAccountId), model, currentAccountTransaction, runtimeSetting);
        }
        String modeOfPayment = request.getParameter("modeOfPayment");
        currentAccountTransaction.setModeOfPayment(modeOfPayment);
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());

        currentAccountTransaction.setBranch(branchInfo.getId());
        currentAccountTransaction.setBranchCode(branchInfo.getCode());
        currentAccountTransaction.setBranchCountry(branchInfo.getCountry());

        currentAccountService.createCurrentAccountTransaction(currentAccountTransaction, currentAccount);
        generalLedgerService.updateGLAfterCurrentAccountAfterCashTransaction(currentAccountTransaction);

        if (currentAccount.getCurrentAccountTransaction() != null) {
            currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        } else {
            currentAccount.setCurrentAccountTransaction(new ArrayList<CurrentAccountTransaction>());
            currentAccount.getCurrentAccountTransaction().add(currentAccountTransaction);
        }

        String username = getLoggedInUserName();
        CurrentBilanzList currentBilanzByUserList = currentAccountService.calculateAccountBilanz(currentAccount.getCurrentAccountTransaction(), false, runtimeSetting);
        callCenterService.saveCallCenterLog(currentAccountTransaction.getReference(),
                username, currentAccount.getAccountNumber(),
                runtimeSetting.getCurrentAccount() + " transaction made " + BVMicroUtils.formatCurrency(currentAccountTransaction.getCurrentAmount(), runtimeSetting.getCountryCode()));

        if (currentAccount.getUser().isReceiveEmailNotifications() && currentAccount.getUser().getEmail() != null) {
            notificationService.notifyReceiver(currentAccountTransaction.getCurrentAmount(), runtimeSetting, currentAccount.getUser(), BVMicroUtils.maskAccountNumber(currentAccount.getAccountNumber()),
                    currentAccountTransaction.getReference(), currentAccountTransaction.getWithdrawalDeposit() > 0 ? "Deposit" : "Withdrawal");
        }

        model.put("name", username);
        model.put("billSelectionInfo", BVMicroUtils.formatCurrency(currentAccountTransaction.getCurrentAmount(), runtimeSetting.getCountryCode()) + " ---- PAYMENT HAS REGISTERED ----- ");
        model.put("currentBilanzList", currentBilanzByUserList);
        request.getSession().setAttribute("currentBilanzList", currentBilanzByUserList);


        Optional<User> byId = userRepository.findById(user.getId());
        request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, byId.get());
        currentAccountTransaction.setCurrentAccount(currentAccount);
        resetCurrentAccountTransaction(currentAccountTransaction); //reset BillSelection and amount
        model.put("glSearchDTO", new GLSearchDTO());
        model.put("currentAccountTransaction", currentAccountTransaction);
        return "currentBilanzNoInterest";

    }

    private void getRepresentative(CurrentAccountTransaction currentAccountTransaction, User user) {
        if (null == currentAccountTransaction.getAccountOwner()) {
            currentAccountTransaction.setAccountOwner("false");
        }
        if (StringUtils.isEmpty(currentAccountTransaction.getRepresentative())) {
            currentAccountTransaction.setRepresentative(BVMicroUtils.getFullName(user));
        }
    }

    private void resetCurrentAccountTransaction(CurrentAccountTransaction sat) {
        sat.setCurrentAmount(0);
        sat.setModeOfPayment(null);
        sat.setWithdrawalDeposit(0);
        sat.setCurrentAmount(0);
        sat.setFifty(0);
        sat.setFiveHundred(0);
        sat.setFiveThousand(0);
        sat.setOneHundred(0);
        sat.setOneThousand(0);
        sat.setTenThousand(0);
        sat.setTwentyFive(0);
        sat.setTen(0);
        sat.setFive(0);
        sat.setOne(0);
        sat.setTwoThousand(0);
        sat.setNotes("");

    }

    private boolean checkBillSelectionMatchesEnteredAmount(CurrentAccountTransaction sat) {
        boolean match = (sat.getCurrentAmount() == (sat.getTenThousand() * 10000) +
                (sat.getFiveThousand() * 5000) +
                (sat.getTwoThousand() * 2000) +
                (sat.getOneThousand() * 1000) +
                (sat.getFiveHundred() * 500) +
                (sat.getOneHundred() * 100) +
                (sat.getFifty() * 50) +
                (sat.getTwentyFive() * 25) +
                (sat.getTen() * 10) +
                (sat.getFive() * 5) +
                (sat.getOne() * 1));
        if (match) {
            sat.setNotes(sat.getNotes()
                    + addBillSelection(sat));
        }
        return match;
    }

    private String addBillSelection(CurrentAccountTransaction sat) {
        String billSelection = " BS \n";
        billSelection = billSelection + concatBillSelection(" 10 000x", sat.getTenThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 5 000x", sat.getFiveThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 2 000x", sat.getTwoThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 1 000x", sat.getOneThousand()) + "\n";
        billSelection = billSelection + concatBillSelection(" 500x", sat.getFiveHundred()) + "\n";
        billSelection = billSelection + concatBillSelection(" 100x", sat.getOneHundred()) + "\n";
        billSelection = billSelection + concatBillSelection(" 50x", sat.getFifty()) + "\n";
        billSelection = billSelection + concatBillSelection(" 25x", sat.getTwentyFive()) + "\n";
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


}