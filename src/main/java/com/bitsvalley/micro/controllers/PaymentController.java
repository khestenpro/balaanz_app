package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.CurrentAccountRepository;
import com.bitsvalley.micro.repositories.DisbursementRequestStatusRepository;
import com.bitsvalley.micro.repositories.FolePayTransactionRepository;
import com.bitsvalley.micro.repositories.PaymentTransactionRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.CollectionRequestStatus;
import com.bitsvalley.micro.utils.DisbursementRequestStatus;
import com.bitsvalley.micro.webdomain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.lang.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Controller
@Slf4j
public class PaymentController extends SuperController {

    @Autowired
    private StripeService paymentsService;

    @Autowired
    private DisbursementRequestStatusRepository disbursementRequestStatusRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private SavingAccountService savingAccountService;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private InitSystemService initSystemService;

    @Autowired
    CMRService cmrService;

    @Autowired
    UserService userService;

    @Autowired
    FolePayTransactionRepository folePayTransactionRepository;

    @Autowired
    private CurrentAccountService currentAccountService;


    @PostMapping("/charge")
    public String charge(@ModelAttribute ChargeRequest chargeRequest, HttpServletRequest request, Model model) {

//        String description = request.getParameter("description");
        String accountType = request.getParameter("accountType");
        String bid = request.getParameter("bid");

        chargeRequest.setCurrency(ChargeRequest.Currency.USD);
        chargeRequest.setDescription(bid + " - " + chargeRequest.getDescription());
        Charge charge = null;
        boolean publicUser = false;
        try {
            charge = paymentsService.charge(chargeRequest);
            float moneyString = charge.getAmount();

            String amountFormat = String.format("%.2f", moneyString / 100);
            model.addAttribute("amount", (amountFormat));
            model.addAttribute("id", charge.getId());
            model.addAttribute("status", charge.getStatus() + "! Your " + accountType + " will be credited");
            model.addAttribute("chargeId", charge.getId());
            model.addAttribute("balance_transaction", charge.getBalanceTransaction());


            String loggedInUserName = getLoggedInUserName();
            if (loggedInUserName.indexOf("anonymousUser") > -1) {
                loggedInUserName = bid;
                publicUser = true;
            }

            paymentsService.updateStripePaymentTransaction(chargeRequest, charge, loggedInUserName);

        } catch (StripeException e) {
            model.addAttribute("error", e.getMessage());
            e.printStackTrace();
        }
        // Update Account and General ledger
        if (publicUser) {
            RuntimeSetting byBid = initSystemService.findByBid(bid);
            model.addAttribute("businessInfo", byBid);
            return "ccPublicPayResult";
        }

        return "ccPaymentResult";
    }


    @PostMapping("/chargeCCSaving")
    public String chargeCCSaving(@ModelAttribute ChargeRequest chargeRequest, HttpServletRequest request, Model model) {

        String accountType = request.getParameter("accountType");
        String accountId = request.getParameter("accountId");

        //Idealy Carry transaction in DB than session. Safer
        SavingAccountTransaction ccSavingAccountTransaction = (SavingAccountTransaction) request.getSession().getAttribute("ccSavingAccountTransaction");
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

//        CurrentAccount byId = currentAccountRepository.findById(new Long(accountId)).get();
        chargeRequest.setDescription(accountType + " payment ");

        chargeRequest.setCurrency(ChargeRequest.Currency.USD);
        Charge charge = null;
        try {
            charge = paymentsService.charge(chargeRequest);
            model.addAttribute("amount", "$" + (charge.getAmount()));
            model.addAttribute("id", charge.getId());
            model.addAttribute("status", charge.getStatus() + "! Your " + accountType + " will be credited");
            model.addAttribute("chargeId", charge.getId());
            model.addAttribute("balance_transaction", charge.getBalanceTransaction());

            paymentsService.updateStripePaymentTransaction(chargeRequest, charge, getLoggedInUserName());
//            savingAccountTransactionRepository.save(ccSavingAccountTransaction);
            SavingAccount byId = savingAccountRepository.findById(ccSavingAccountTransaction.getSavingAccount().getId()).get();
            ccSavingAccountTransaction.setSavingAccount(byId);

            savingAccountService.createSavingAccountTransaction(ccSavingAccountTransaction, byId, runtimeSetting);

        } catch (StripeException e) {
            model.addAttribute("error", e.getMessage());
            e.printStackTrace();
        }
        // Update Account and General ledger
        return "ccPaymentResult";
    }


    @ExceptionHandler(StripeException.class)
    public String handleError(Model model, StripeException ex) {
        model.addAttribute("error", ex.getMessage());
        return "ccPaymentResult";
    }


//    @PostMapping(value = "/sendMomoToCurrent")
//    public String receiveCurrentFromMomo(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException {
//        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
//        String amount = request.getParameter("momoAmount");
//        int momoAmount = Integer.parseInt(amount);
//        String collectionPhoneNumer = user.getTelephone1().replaceAll(" ", "");
//
//        //TODO: Populate from model
//        CollectionRequest collectionRequest = new CollectionRequest();
//        collectionRequest.setAmount(new BigDecimal(momoAmount));
//        collectionRequest.setSenderPhoneNumber(Long.parseLong(collectionPhoneNumer));
////        collectionRequest.setSenderMsisdnProvider();
//        CollectionRequestStatus collectionRequestStatus = cmrService.sendMomoCollectionRequest(collectionRequest);
//        // check balances and show confirmation page
//        // or show inline error message from origin page
//
//        return "userHomeCustomer";
//    }


    @PostMapping(value = "/sendCurrentToMomo")
    public String sendCurrentToMomo(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        RuntimeSetting runtimeSettings = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        if (!StringUtils.equalsIgnoreCase("true", runtimeSettings.getWalletEnabled())) {
            model.addAttribute("disbursementInfo", "Contact Support. Feature not activated");
            return "userHomeCustomer";
        }

        String momoNumber = request.getParameter("momoNumber");
        String amount = request.getParameter("momoAmount");
        String momoNotes = request.getParameter("momoNotes");
        int momoAmount = 0;
        try {
            momoAmount = Integer.parseInt(amount);
        } catch (Exception e) {
            model.addAttribute("disbursementInfo", "Enter a correct amount");
            return "userHomeCustomer";
        }

        BigDecimal sendToMomoAmount = new BigDecimal(momoAmount);
        UserControl uControl = user.getUserControl();
        if (uControl == null) {
            user.setUserControl(new UserControl());
        }
        if (!user.getUserControl().isMobileMoneyActive()) {
            model.addAttribute("disbursementInfo", "Contact Support. Feature not activated");
            return "userHomeCustomer";
        }
        LocalDate today = LocalDate.now();

        if (user.getUserControl() != null) {
            Double currentDailySent = paymentsService.getDailyLimit(today, user.getOrgId(), user.getId());
            if (currentDailySent != null) {
                double currentDifference = user.getUserControl().getMobileMoneyDailyLimit() - currentDailySent.intValue();
                // currentDifference  // available today for transfer
                if (sendToMomoAmount.doubleValue() + currentDailySent > user.getUserControl().getMobileMoneyDailyLimit()) {
                    model.addAttribute("disbursementInfo", "Exceeds daily limit of " + BVMicroUtils.formatCurrency(user.getUserControl().getMobileMoneyDailyLimit()) + ". Maximum Possible is: " + BVMicroUtils.formatCurrency(currentDifference));
                    return "userHomeCustomer";
                }
            }
//            user.getUserControl().getMobileMoneyDailyLimit(),
        }

//        if (momoAmount > paymentsService.getMonthlyLimit(today, user.getOrgId(), user.getId()  )) {
//            model.addAttribute("disbursementInfo", "Exceeds daily limit of " + BVMicroUtils.formatCurrency(user.getUserControl().getMobileMoneyDailyLimit()));
//            return "userHomeCustomer";
//        }
        CurrentAccount currentAccount = user.getCurrentAccount().get(0); // get the first current account
        if ((currentAccount.getAccountBalance() - currentAccount.getAccountMinBalance()) < sendToMomoAmount.doubleValue()) {
            model.addAttribute("disbursementInfo", "Insufficient Account Balance .... frs " + BVMicroUtils.formatCurrency(sendToMomoAmount.doubleValue()));
            //TODO: Call FolePay API to get end customers momobalance before or after
            return "userHomeCustomer";
        }

        if (momoNumber.length() == 9) {
            momoNumber = "237" + momoNumber;
        }
        if (momoNumber.length() != 12) {
            model.addAttribute("disbursementInfo", "Verify Mobile money number ....  ");
            return "userHomeCustomer";
        }
        try {
            MomoCustomerInfo momoCustomerInfo = cmrService.getSubscriberInfo(momoNumber);
            request.getSession().setAttribute("disbursementCustomerName", momoCustomerInfo.getGivenNames() + " " + momoCustomerInfo.getFamilyName());

        } catch (Exception exception) {
            request.getSession().setAttribute("disbursementCustomerName", " ");
        }

        DisbursementRequest disbursementRequest = new DisbursementRequest();
        disbursementRequest.setRecipientPhoneNumber(Long.parseLong(momoNumber));
        disbursementRequest.setAmount(sendToMomoAmount); //new BigDecimal(momoAmount));
        disbursementRequest.setNote(momoNotes);

        request.getSession().setAttribute("platformFee", BVMicroUtils.formatCurrency(savingAccountService.calculatePlatformFee(String.valueOf(sendToMomoAmount), runtimeSettings.getPlatformFee()).doubleValue()));

        request.getSession().setAttribute("currentAccount", currentAccount);
        request.getSession().setAttribute("disbursementRequest", disbursementRequest);

        return "disbursementPreview";
    }

    /*
     * TO save mtn username per transaction
     * need to get attribute from html
     * and also need org momoaccount to save the transaction
     * */
    @PostMapping(value = "/sendCurrentToMomoConfirm")
    public String sendCurrentToMomoConfirm(ModelMap model, HttpServletRequest request) {

        HttpSession session = request.getSession();
        RuntimeSetting runtimeSetting = (RuntimeSetting) session.getAttribute("runtimeSettings");
        if (!StringUtils.equalsIgnoreCase(runtimeSetting.getWalletEnabled(), "true")) {
            model.put("error", "Contact Support. Service not enabled");
            return "disbursementPreview";
        }

        CurrentAccount currentAccount = (CurrentAccount) session.getAttribute("currentAccount");
        DisbursementRequest disbursementRequest = (DisbursementRequest) session.getAttribute("disbursementRequest");

        String balaanzPin = request.getParameter("balaanzPin");
        User user = (User) session.getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (!userService.validateBalaanzPin(user.getId(), balaanzPin)) {

            model.put("error", "Re-enter PIN. Not valid");
            return "disbursementPreview";
        }
        if ("true".equals(runtimeSetting.getWalletEnabled())) {
            ExecutorService threadPool = Executors.newCachedThreadPool();
//            Future<DisbursementRequestStatus> futureTask = threadPool.submit(() ->
            cmrService.sendMomoDisbursementRequest(currentAccount, disbursementRequest, runtimeSetting);
            threadPool.shutdown();

//        try {
//            String results = futureTask.get().getStatus();
//            if (StringUtils.equals("SUCCESSFUL", results)) {

            model.addAttribute("statusInfo", "Amount Sent ");
            model.addAttribute("amountSent", BVMicroUtils.formatCurrency(disbursementRequest.getAmount().doubleValue()));

        }
//            }
//        } catch (Exception e) {
//            log.info("-- --- ----- ------- ------- ------------ ",e.getMessage());
//        }

        //TODO: Call FolePay API to get end customers momobalance before or after
        session.removeAttribute("platformFee");
        return "disbursementResult";
    }
//
//    @GetMapping
//    public String checkTransactionStatus() {
//
//        return "";
//    }

    @PostMapping(value = "/pay/mtncollect")
    public String mtnCollect(@RequestBody CollectionRequestStatus status) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        log.info("xxxxxxxx - mtncollect - mtncollect - xxxxxxxxxxxxxx-----------------------");

        log.info("                                        ----                    ");

        log.info("xxxxxxxx-xxxxxxx-xxxxxxxxxxxxxx-----------------------");

        log.info(om.writeValueAsString(status));
//        log.info(" testing lombok {}",status.getCurrency());
        log.info("                                        ----                    ");
        log.info("xxxxxxxx-xxxxxxx-xxxxxxxxxxxxxx-----------------------");

        return "login";
    }

    // verify the content
    @PostMapping(value = "/pay/mtndisburse")
    public String mtnDisburse(@RequestBody DisbursementRequestStatus status) throws JsonProcessingException {

        log.info("--------            ------     save   disbursementRequestStatusRepository     ----------            ------");
        com.bitsvalley.micro.domain.DisbursementRequestStatus disbursementStatus = convertToDisbursementStatus(status);
        disbursementRequestStatusRepository.save(disbursementStatus);

        ObjectMapper om = new ObjectMapper();
        log.info("---                          -----------                      ------------");
        log.info(om.writeValueAsString(status));
        log.info("---                          -----------                      ------------");

        return "login";
    }

    private com.bitsvalley.micro.domain.DisbursementRequestStatus convertToDisbursementStatus(DisbursementRequestStatus status) {
        log.info("........ Converting to com.bitsvalley.micro.domain.DisbursementRequestStatus ...............");
        com.bitsvalley.micro.domain.DisbursementRequestStatus aStatus = new com.bitsvalley.micro.domain.DisbursementRequestStatus();
        aStatus.setStatus(status.getStatus());
        aStatus.setAmount(status.getAmount());
        aStatus.setCurrency(status.getCurrency());
        aStatus.setNote(status.getNote());
        aStatus.setAccountNumber(status.getAccountNumber());
        aStatus.setTransactionFee(aStatus.getTransactionFee());
        aStatus.setRecipientMsisdnProvider(status.getMsisdnProvider());
        aStatus.setAccountNumber(status.getAccountNumber());
        aStatus.setRecipientPhoneNumber(status.getPhoneNumber());
        aStatus.setRequestId(status.getRequestId());
        aStatus.setTransactionId(status.getTransactionId());
        if (aStatus.getDate() == null) {
            aStatus.setDate(new Date());
        }
        log.info(".......... Converted to DisbursementRequestStatus ...............");

        return aStatus;
    }

    @PostMapping(value = "/disbursementStatus")
    public String disbursementStatus(@RequestBody CollectionRequestStatus status, HttpServletRequest request, ModelMap model) {
        final com.bitsvalley.micro.domain.DisbursementRequestStatus byRequestId = disbursementRequestStatusRepository.findByRequestId(status.getRequestId());

        if (byRequestId != null && byRequestId.getStatus() != null) {
            model.addAttribute("statusInfo", byRequestId.getStatus());
            model.addAttribute("amount", BVMicroUtils.formatCurrency(byRequestId.getAmount().doubleValue()));
        } else {
            model.put("statusInfo", "Processing");
        }
        return "disbursementResult";
    }


    @GetMapping(value = "/grest88")
    public String getTransactions(ModelMap model) throws InvalidKeyException, NoSuchAlgorithmException {
        List<FolePayTransactionWeb> folePayTransactionWebs = cmrService.getTransactionSummary();
        Wallet wallet = cmrService.getBalance();

        model.put("wallet", wallet);
        Collections.reverse(folePayTransactionWebs);
        model.put("folePayDTransactions", folePayTransactionWebs);

        //callback from mtn
        List<com.bitsvalley.micro.domain.DisbursementRequestStatus> callBackList = BVMicroUtils.getCollectionFromIterable(disbursementRequestStatusRepository.findAll());
        Collections.reverse(callBackList);
        model.put("allCallBackBalaanzDRequests", callBackList);

        //No callback or failed. Remove records which we got a callback on
        List<FolePayTransaction> listFromIterable = BVMicroUtils.getCollectionFromIterable(folePayTransactionRepository.findAll());
        Collections.reverse(listFromIterable);
        for (FolePayTransaction trans : listFromIterable) {
            if (callBackList.contains(trans.getRequestId())) {
                listFromIterable.remove(trans);
            }
        }
        model.put("allBalaanzDRequests", listFromIterable);

        return "momoTransactions";
    }

    @GetMapping(value = "/grest88/{id}")
    public String customerMomo(@PathVariable("id") long id, ModelMap model) {

        return "customerMomoTransactions";
    }

}
