package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.RuntimePropertiesRepository;
import com.bitsvalley.micro.repositories.ShareAccountRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import com.bitsvalley.micro.webdomain.TransferBilanz;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class ShareAccountController extends SuperController {

    @Autowired
    ShareAccountRepository shareAccountRepository;

    @Autowired
    ShareAccountService shareAccountService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BranchService branchService;

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    PdfService pdfService;

    @Autowired
    RuntimePropertiesRepository runtimePropertiesRepository;

    @Autowired
    private CallCenterService callCenterService;

    @GetMapping(value = "/shareDetails/{id}")
    public String shareDetails(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        ShareAccount byId = shareAccountRepository.findById(id).get();

        User user = byId.getUser();
        request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, user);
        model.put("transferBilanz", new TransferBilanz());
        model.put("share", byId);
        if (byId.getAccountStatus().equals(AccountStatus.PENDING_PAYOUT)) {
            model.put("showTransferBilanzSection", true);
        } else {
            model.put("showTransferBilanzSection", false);
        }
        return "shareDetails";
    }

    @GetMapping(value = "/shareDetailsAccNumber/{accountNumber}")
    public String shareDetailAccNumber(@PathVariable("accountNumber") String accountNumber, ModelMap model, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        ShareAccount byId = shareAccountRepository.findByAccountNumberAndOrgId(accountNumber, loggedInUser.getOrgId());
        User user = byId.getUser();
        request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, user);
        model.put("transferBilanz", new TransferBilanz());
        model.put("share", byId);

        if (byId.getAccountStatus().equals(AccountStatus.PENDING_PAYOUT)) {
            model.put("showTransferBilanzSection", true);
        } else {
            model.put("showTransferBilanzSection", false);
        }

        return "shareDetails";
    }

    @Transactional
    @GetMapping(value = "/approveShareAccount/{id}")
    public String approveShare(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        ShareAccount byId = shareAccountRepository.findById(id).get();
        model.put("transferBilanz", new TransferBilanz());
        if (byId.getCreatedBy().equals(getLoggedInUserName())) {
            model.put("shareApproveInfo", "A different authorized user should approve this purchase");
            model.put("showTransferBilanzSection", false);
        } else {
            byId.setAccountStatus(AccountStatus.PENDING_PAYOUT);
            byId.setApprovedBy(getLoggedInUserName());
            byId.setApprovedDate(new Date());
            shareAccountRepository.save(byId);
            callCenterService.saveCallCenterLog("PENDING PAYOUT", getLoggedInUserName(), byId.getAccountNumber(), "Share ACCOUNT APPROVED now pending payout"); //TODO ADD DATE
            model.put("showTransferBilanzSection", true);
        }
        return shareDetailAccNumber(byId.getAccountNumber(), model, request);

    }

    @Transactional
    @PostMapping(value = "/transferFromCurrentToShareAccountsFormReview")
    public String transferFromCurrentToShareAccountsFormReview(ModelMap model,
                                                               @ModelAttribute("transferBilanz") TransferBilanz transferBilanz
            , HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        model.put("transferBilanz", transferBilanz);
        String shareId = request.getParameter("shareId");
        CurrentAccount fromAccount = currentAccountService.findByAccountNumberAndOrgId(transferBilanz.getTransferFromAccount(), loggedInUser.getOrgId());
        ShareAccount toAccount = shareAccountRepository.findById(new Long(shareId)).get();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        if (fromAccount.getAccountBalance() < toAccount.getAccountBalance()) {
            model.put("error", "INSUFFICIENT FUNDS AVAILABLE TO MAKE SHARE PURCHASE");
            model.put("transferBilanz", new TransferBilanz());
            model.put("share", toAccount);
            return "shareDetails";
        } else {
            shareAccountService.transferFromCurrentToShareAccount(
                    fromAccount,
                    toAccount,
                    transferBilanz.getTransferAmount(),
                    transferBilanz.getNotes(), runtimeSetting.getCurrentAccount());
            String value = BVMicroUtils.formatCurrency(transferBilanz.getTransferAmount(), runtimeSetting.getCountryCode());

            model.put("transferType", BVMicroUtils.CURRENT_SHARE_TRANSFER);
            model.put("fromTransferText", " Balance: " + BVMicroUtils.formatCurrency(fromAccount.getAccountBalance(), runtimeSetting.getCountryCode()) + "  Minimum Balance:" + BVMicroUtils.formatCurrency(fromAccount.getAccountMinBalance(), runtimeSetting.getCountryCode()));
            model.put("toTransferText", toAccount.getAccountStatus().name() + "   Balance: " + value);

            model.put("transferAmount", value);
            model.put("notes", transferBilanz.getNotes());
            return "transferConfirm";
        }

    }


    @PostMapping(value = "/registerShareAccountForm")
    public String registerShareAccount(@ModelAttribute("shareAccount") ShareAccount shareAccount, ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        user = userRepository.findById(user.getId()).get();
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());//TODO Create branch repo
        shareAccount.setBranchCode(branchInfo.getCode());
        shareAccount.setCountry(branchInfo.getCountry());

        String sharePrice = "";
        if (StringUtils.equals("31", shareAccount.getProductCode())) {
            sharePrice = initSystemService.findByPropertyName(BVMicroUtils.UNIT_SHARE_PREFERENCE_PRICE, user.getOrgId());
            shareAccount.setAccountType(BVMicroUtils.PREFERENCE_SHARE_TYPE);
        } else if (StringUtils.equals("30", shareAccount.getProductCode())) {
            sharePrice = initSystemService.findByPropertyName(BVMicroUtils.UNIT_SHARE_PRICE, user.getOrgId());
            shareAccount.setAccountType(BVMicroUtils.ORDINARY_SHARE_TYPE);

        } else {

            String[] gl_s = shareAccount.getProductCode().split("_GL_");

            String shareType = gl_s[0];
            String productCode = gl_s[1];
            shareAccount.setAccountType(shareType);
            shareAccount.setProductCode(productCode);
            sharePrice = initSystemService.findByPropertyName(shareType, user.getOrgId()) == null ? "0" : initSystemService.findByPropertyName(shareType, user.getOrgId());



        }
        shareAccount.setUnitSharePrice(Double.parseDouble(sharePrice));

        shareAccountService.createShareAccount(shareAccount, user);
        return findUserByUserName(user, model, request);
    }

    @GetMapping(value = "/registerShareAccount")
    public String registerShareAccount(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        List<AccountType> byOrgIdAndCategory = accountTypeRepository.findByOrgIdAndCategoryAndActiveTrue(user.getOrgId(), BVMicroUtils.SHARE);
        model.put("accountTypes", byOrgIdAndCategory);


        ShareAccount shareAccount = new ShareAccount();
        String byPropertyName = initSystemService.findByPropertyName(BVMicroUtils.UNIT_SHARE_PRICE, user.getOrgId());
        shareAccount.setUnitSharePrice(Double.parseDouble(byPropertyName));
        model.put("shareAccount", shareAccount);

        model.put("sharePriceInfo", getShareSettings() );
        return "shareAccount";
    }


    public List<String> getShareSettings() {
        User user = userRepository.findByUserName(getLoggedInUserName());
        Iterable<RuntimeProperties> all = runtimePropertiesRepository.findByOrgId(user.getOrgId());
        Iterator<RuntimeProperties> iterator = all.iterator();
        List<String> list = new ArrayList<String>();

        while (iterator.hasNext()) {
            RuntimeProperties rp = iterator.next();
            if (rp.getPropertyName().toLowerCase().contains("share")) { // weak TODO change this.
                list.add( rp.getPropertyName() + ": " + rp.getPropertyValue());
            }

        }
        return list;
    }

    @GetMapping(value = "/shareAccounts")
    public String shareAccounts(ModelMap model, HttpServletRequest request) {

        User user = userRepository.findByUserName(getLoggedInUserName());
        Iterable<ShareAccount> shares = shareAccountRepository.findByOrgId(user.getOrgId());
        Iterator<ShareAccount> iterator = shares.iterator();
        model.put("shareAccountsList", iterator);
        return "shareAccounts";
    }


    @GetMapping(value = "/printShareDetail/{id}")
    public ResponseEntity<ByteArrayResource> currentSharePDF(@PathVariable("id") long id, ModelMap model, HttpServletRequest request,
                                                             HttpServletResponse response) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-disposition", "inline; filename=" + "statementShares.pdf");
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        ShareAccount shareAccount = shareAccountRepository.findById(new Long(id)).get();
        String htmlInput = pdfService.generateShareDetailsPDF(shareAccount.getShareAccountTransaction().get(0), initSystemService.findByOrgId(user.getOrgId()));
        byte[] pdfBytes = pdfService.generatePDF(htmlInput, response).toByteArray();
        ByteArrayResource resources = new ByteArrayResource(pdfBytes);
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resources);

    }

}