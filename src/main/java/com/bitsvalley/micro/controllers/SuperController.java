package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
public class SuperController {


    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private SavingAccountTransactionRepository savingAccountTransactionRepository;

    @Autowired
    private DailySavingAccountTransactionRepository dailySavingAccountTransactionRepository;

    @Autowired
    private ShareAccountTransactionRepository shareAccountTransactionRepository;

    @Autowired
    private CurrentAccountTransactionRepository currentAccountTransactionRepository;

    @Autowired
    private DailySavingAccountService dailySavingAccountService;

    @Autowired
    private SavingAccountService savingAccountService;

    @Autowired
    private LoanAccountService loanAccountService;

    @Autowired
    private LoanAccountTransactionRepository loanAccountTransactionRepository;

    @Autowired
    private CurrentAccountService currentAccountService;

    @Autowired
    private LoanAccountTransactionService loanAccountTransactionService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private ShareAccountService shareAccountService;

    @Autowired
    private GeneralLedgerRepository generalLedgerRepository;

    @Autowired
    private RuntimePropertiesRepository runtimePropertiesRepository;


    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    SavingAccountRepository savingAccountRepository;


    public String homeType(User loggedInCustomer, User customerInUse) {
        if( loggedInCustomer != null && customerInUse != null)
        if (loggedInCustomer.getId() == customerInUse.getId()){
            return "userHomeCustomer";
        }
         return "userHome";
    }

    @NotNull
    public LocalDateTime getFirstDayOfMonth(LocalDateTime now ) {
        LocalDateTime localDateStart = now.minusDays(now.getDayOfMonth() - 1);
        localDateStart = localDateStart.minusHours(now.getHour());
        localDateStart = localDateStart.minusMinutes(now.getMinute());
        localDateStart = localDateStart.minusSeconds(now.getSecond());
        return localDateStart;
    }

    public void createRuntimeOrgProperties(RuntimeSetting runtimeSetting, long orgId) {
        List<RuntimeProperties> list = new ArrayList<RuntimeProperties>();
        User user = userRepository.findByUserName(getLoggedInUserName());
        RuntimeProperties business_name = runtimePropertiesRepository.findByPropertyNameAndOrgId("Business Name", orgId);
        if (business_name == null) {
            business_name = new RuntimeProperties();
            business_name.setPropertyName("Business Name");
        }
        business_name.setOrgId(orgId);
        business_name.setPropertyValue(runtimeSetting.getBusinessName());
        list.add(business_name);


        RuntimeProperties upload_directory = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", orgId);
        if (upload_directory == null) {
            upload_directory = new RuntimeProperties();
            upload_directory.setPropertyName("Upload Directory");
        }
        upload_directory.setOrgId(orgId);
        upload_directory.setPropertyValue(runtimeSetting.getUploadDirectory());
        list.add(upload_directory);

        RuntimeProperties slogan = runtimePropertiesRepository.findByPropertyNameAndOrgId("Slogan", orgId);
        if (slogan == null) {
            slogan = new RuntimeProperties();
            slogan.setPropertyName("Slogan");
        }
        slogan.setPropertyValue(runtimeSetting.getSlogan());
        slogan.setOrgId(orgId);
        list.add(slogan);

        RuntimeProperties address = runtimePropertiesRepository.findByPropertyNameAndOrgId("address", orgId);
        if (address == null) {
            address = new RuntimeProperties();
            address.setPropertyName("address");
        }
        address.setPropertyValue(runtimeSetting.getAddress());
        address.setOrgId(orgId);
        list.add(address);

        RuntimeProperties logo = runtimePropertiesRepository.findByPropertyNameAndOrgId("logo", orgId);
        if (logo == null) {
            logo = new RuntimeProperties();
            logo.setPropertyName("logo");
        }
        logo.setPropertyValue(runtimeSetting.getLogo());
        logo.setOrgId(orgId);
        list.add(logo);

        RuntimeProperties unionLogo = runtimePropertiesRepository.findByPropertyNameAndOrgId("unionLogo", orgId);
        if (unionLogo == null) {
            unionLogo = new RuntimeProperties();
            unionLogo.setPropertyName("unionLogo");
        }
        unionLogo.setPropertyValue(runtimeSetting.getUnionLogo());
        unionLogo.setOrgId(orgId);
        list.add(unionLogo);

        RuntimeProperties momoOrgAccount = runtimePropertiesRepository.findByPropertyNameAndOrgId("momoOrgAccount", orgId);
        if (momoOrgAccount == null) {
            momoOrgAccount = new RuntimeProperties();
            momoOrgAccount.setPropertyName("momoOrgAccount");
        }
        momoOrgAccount.setPropertyValue(runtimeSetting.getMomoOrgAccount());
        momoOrgAccount.setOrgId(orgId);
        list.add(momoOrgAccount);

        RuntimeProperties platformFee = runtimePropertiesRepository.findByPropertyNameAndOrgId("platformFee", orgId);
        if (platformFee == null) {
            platformFee = new RuntimeProperties();
            platformFee.setPropertyName("platformFee");
        }
        platformFee.setPropertyValue(String.valueOf(runtimeSetting.getPlatformFee()));
        platformFee.setOrgId(orgId);
        list.add(platformFee);

        RuntimeProperties walletEnabled = runtimePropertiesRepository.findByPropertyNameAndOrgId("walletEnabled", orgId);
        if (walletEnabled == null) {
            walletEnabled = new RuntimeProperties();
            walletEnabled.setPropertyName("walletEnabled");
        }
        walletEnabled.setPropertyValue(String.valueOf(runtimeSetting.getWalletEnabled()));
        walletEnabled.setOrgId(orgId);
        list.add(walletEnabled);

        RuntimeProperties savingMinBalance = runtimePropertiesRepository.findByPropertyNameAndOrgId("savingMinBalance", orgId);
        if (savingMinBalance == null) {
            savingMinBalance = new RuntimeProperties();
            savingMinBalance.setPropertyName("savingMinBalance");
        }
        savingMinBalance.setPropertyValue(String.valueOf(runtimeSetting.getSavingMinBalance()));
        savingMinBalance.setOrgId(orgId);
        list.add(savingMinBalance);


        RuntimeProperties imgPrefix = runtimePropertiesRepository.findByPropertyNameAndOrgId("Image Prefix", orgId);
        if (imgPrefix == null) {
            imgPrefix = new RuntimeProperties();
            imgPrefix.setPropertyName("Image Prefix");
        }
        imgPrefix.setPropertyValue(runtimeSetting.getImagePrefix());
        imgPrefix.setOrgId(orgId);
        list.add(imgPrefix);

        RuntimeProperties telephone = runtimePropertiesRepository.findByPropertyNameAndOrgId("telephone1", orgId);
        if (telephone == null) {
            telephone = new RuntimeProperties();
            telephone.setPropertyName("telephone1");
        }
        telephone.setPropertyValue(runtimeSetting.getTelephone());
        telephone.setOrgId(orgId);
        list.add(telephone);

        RuntimeProperties telephone2 = runtimePropertiesRepository.findByPropertyNameAndOrgId("telephone2", orgId);
        if (telephone2 == null) {
            telephone2 = new RuntimeProperties();
            telephone2.setPropertyName("telephone2");
        }
        telephone2.setPropertyValue(runtimeSetting.getTelephone2());
        telephone2.setOrgId(orgId);
        list.add(telephone2);

        RuntimeProperties email = runtimePropertiesRepository.findByPropertyNameAndOrgId("email", orgId);
        if (email == null) {
            email = new RuntimeProperties();
            email.setPropertyName("email");
        }
        email.setPropertyValue(runtimeSetting.getEmail());
        email.setOrgId(orgId);
        list.add(email);

        RuntimeProperties fax = runtimePropertiesRepository.findByPropertyNameAndOrgId("fax", orgId);
        if (fax == null) {
            fax = new RuntimeProperties();
            fax.setPropertyName("fax");
        }
        fax.setPropertyValue(runtimeSetting.getFax());
        fax.setOrgId(orgId);
        list.add(fax);

        RuntimeProperties website = runtimePropertiesRepository.findByPropertyNameAndOrgId("website", orgId);
        if (website == null) {
            website = new RuntimeProperties();
            website.setPropertyName("website");
        }
        website.setPropertyValue(runtimeSetting.getWebsite());
        website.setOrgId(orgId);
        list.add(website);


        RuntimeProperties logoSize = runtimePropertiesRepository.findByPropertyNameAndOrgId("logoSize", orgId);
        if (logoSize == null) {
            logoSize = new RuntimeProperties();
            logoSize.setPropertyName("logoSize");
        }
        logoSize.setPropertyValue(runtimeSetting.getLogoSize());
        logoSize.setOrgId(orgId);
        list.add(logoSize);

        RuntimeProperties currency = runtimePropertiesRepository.findByPropertyNameAndOrgId("currency", orgId);
        if (currency == null) {
            currency = new RuntimeProperties();
            currency.setPropertyName("currency");
        }
        currency.setPropertyValue(runtimeSetting.getCurrency());
        currency.setOrgId(orgId);
        list.add(currency);


        RuntimeProperties themeColor = runtimePropertiesRepository.findByPropertyNameAndOrgId("themeColor", orgId);
        if (themeColor == null) {
            themeColor = new RuntimeProperties();
            themeColor.setPropertyName("themeColor");
        }
        themeColor.setPropertyValue(runtimeSetting.getThemeColor());
        themeColor.setOrgId(orgId);
        list.add(themeColor);

        RuntimeProperties countryCode = runtimePropertiesRepository.findByPropertyNameAndOrgId("countryCode", orgId);
        if (countryCode == null) {
            countryCode = new RuntimeProperties();
            countryCode.setPropertyName("countryCode");
        }
        countryCode.setPropertyValue(runtimeSetting.getCountryCode());
        countryCode.setOrgId(orgId);
        list.add(countryCode);

        RuntimeProperties themeColor2 = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.THEME_COLOR_2, orgId);
        if (themeColor2 == null) {
            themeColor2 = new RuntimeProperties();
            themeColor2.setPropertyName(BVMicroUtils.THEME_COLOR_2);
        }
        themeColor2.setPropertyValue(runtimeSetting.getThemeColor2());
        themeColor2.setOrgId(orgId);
        list.add(themeColor2);

        RuntimeProperties unitSharePrice = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.UNIT_SHARE_PRICE, orgId);
        if (unitSharePrice == null) {
            unitSharePrice = new RuntimeProperties();
            unitSharePrice.setPropertyName(BVMicroUtils.UNIT_SHARE_PRICE);
        }
        unitSharePrice.setPropertyValue(runtimeSetting.getUnitSharePrice());
        unitSharePrice.setOrgId(orgId);
        list.add(unitSharePrice);

        RuntimeProperties unitPreferenceSharePrice = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.UNIT_SHARE_PREFERENCE_PRICE, orgId);
        if (unitPreferenceSharePrice == null) {
            unitPreferenceSharePrice = new RuntimeProperties();
            unitPreferenceSharePrice.setPropertyName(BVMicroUtils.UNIT_SHARE_PREFERENCE_PRICE);
        }
        unitPreferenceSharePrice.setPropertyValue(runtimeSetting.getUnitPreferenceSharePrice());
        unitPreferenceSharePrice.setOrgId(orgId);
        list.add(unitPreferenceSharePrice);

        RuntimeProperties vatPercent = runtimePropertiesRepository.findByPropertyNameAndOrgId("vatPercent", orgId);
        if (vatPercent == null) {
            vatPercent = new RuntimeProperties();
            vatPercent.setPropertyName("vatPercent");

        }
        Double vat = runtimeSetting.getVatPercent();
        vat = (vat == null ? 0.195 : vat);
        vatPercent.setPropertyValue(vat.toString());
        vatPercent.setOrgId(orgId);
        list.add(vatPercent);

        RuntimeProperties bid = runtimePropertiesRepository.findByPropertyNameAndOrgId("bid", orgId);
        if (bid == null) {
            bid = new RuntimeProperties();
            bid.setPropertyName("bid");
        }
        bid.setPropertyValue(runtimeSetting.getBid());
        bid.setOrgId(orgId);
        list.add(bid);


        RuntimeProperties footerInvoice = runtimePropertiesRepository.findByPropertyNameAndOrgId("invoiceFooter", orgId);
        if (footerInvoice == null) {
            footerInvoice = new RuntimeProperties();
            footerInvoice.setPropertyName("invoiceFooter");
        }
        footerInvoice.setPropertyValue(runtimeSetting.getInvoiceFooter());
        footerInvoice.setOrgId(orgId);
        list.add(footerInvoice);

        RuntimeProperties makeAPayment = runtimePropertiesRepository.findByPropertyNameAndOrgId("makeAPayment", orgId);
        if (makeAPayment == null) {
            makeAPayment = new RuntimeProperties();
            makeAPayment.setPropertyName("makeAPayment");
        }
        makeAPayment.setPropertyValue(runtimeSetting.getMakeAPayment());
        makeAPayment.setOrgId(orgId);
        list.add(makeAPayment);

        RuntimeProperties billSelectionEnabled = runtimePropertiesRepository.findByPropertyNameAndOrgId("billSelectionEnabled", orgId);
        if (billSelectionEnabled == null) {
            billSelectionEnabled = new RuntimeProperties();
            billSelectionEnabled.setPropertyName("billSelectionEnabled");
        }
        billSelectionEnabled.setPropertyValue(runtimeSetting.getBillSelectionEnabled());
        billSelectionEnabled.setOrgId(orgId);
        list.add(billSelectionEnabled);

        RuntimeProperties contextName = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.CONTEXT_NAME, orgId);
        if (contextName == null) {
            contextName = new RuntimeProperties();
            contextName.setPropertyName(BVMicroUtils.CONTEXT_NAME);
        }
        contextName.setPropertyValue(runtimeSetting.getContextName());
        contextName.setOrgId(orgId);
        list.add(contextName);

        RuntimeProperties emailDescription1 = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.EMAIL_DESCRIPTION_1, orgId);
        if (emailDescription1 == null) {
            emailDescription1 = new RuntimeProperties();
            emailDescription1.setPropertyName(BVMicroUtils.EMAIL_DESCRIPTION_1);
        }
        emailDescription1.setPropertyValue(runtimeSetting.getEmailDescription1());
        emailDescription1.setOrgId(orgId);
        list.add(emailDescription1);

        RuntimeProperties emailDescription2 = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.EMAIL_DESCRIPTION_2, orgId);
        if (emailDescription2 == null) {
            emailDescription2 = new RuntimeProperties();
            emailDescription2.setPropertyName(BVMicroUtils.EMAIL_DESCRIPTION_2);
        }
        emailDescription2.setPropertyValue(runtimeSetting.getEmailDescription2());
        emailDescription2.setOrgId(orgId);
        list.add(emailDescription2);

        RuntimeProperties organizationProvidedServices = runtimePropertiesRepository.findByPropertyNameAndOrgId(BVMicroUtils.ORG_PROVIDED_SERVICES, orgId);
        if (organizationProvidedServices == null) {
            organizationProvidedServices = new RuntimeProperties();
            organizationProvidedServices.setPropertyName(BVMicroUtils.ORG_PROVIDED_SERVICES);
        }
        organizationProvidedServices.setPropertyValue(runtimeSetting.getOrganizationProvidedServices());
        organizationProvidedServices.setOrgId(orgId);
        list.add(organizationProvidedServices);

        runtimePropertiesRepository.saveAll(list);
    }


    public String findUserByUserName(User user, ModelMap model, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        user.setUserName(user.getUserName().replaceAll("\\s", ""));
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        User aUser = null;
        if (null != loggedInUser) {

            aUser = userService.findByUserNameAndOrgId(user.getUserName(), loggedInUser.getOrgId());
            if (aUser == null) {
                SavingAccount savingAccount = savingAccountService.findByAccountNumberAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                if (null != savingAccount) {
                    aUser = savingAccount.getUser();
                }
                if (aUser == null) {
                    Optional<SavingAccountTransaction> byReference
                            = savingAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        aUser = byReference.get().getSavingAccount().getUser();
                    }
                }
                if (aUser == null) {
                    DailySavingAccount byReference
                            = dailySavingAccountService.findByAccountNumberAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference != null) {
                        aUser = byReference.getUser();
                    }
                }
                if (aUser == null) {
                    Optional<DailySavingAccountTransaction> byReference
                            = dailySavingAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        aUser = byReference.get().getDailySavingAccount().getUser();
                    }
                }
                if (aUser == null) {
                    Optional<LoanAccountTransaction> byReference
                            = loanAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        aUser = byReference.get().getLoanAccount().getUser();
                    }
                }
                if (aUser == null) {
                    LoanAccount byReference
                            = loanAccountService.findByAccountNumberAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference != null) {
                        aUser = byReference.getUser();
                    }
                }
//                if (aUser == null) {
//                    Optional<LoanAccountTransaction> byReference
//                            = loanAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
//                    if (byReference.isPresent()) {
//                        aUser = byReference.get().getLoanAccount().getUser();
//                    }
//                }
                if (aUser == null) {
                    CurrentAccount byReference
                            = currentAccountService.findByAccountNumberAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference != null) {
                        aUser = byReference.getUser();
                    }
                }
                if (aUser == null) {
                    Optional<CurrentAccountTransaction> byReference
                            = currentAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        aUser = byReference.get().getCurrentAccount().getUser();
                    }
                }
                if (aUser == null) {
                    ShareAccount byReference
                            = shareAccountService.findByAccountNumberAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference != null) {
                        aUser = byReference.getUser();
                    }
                }
                if (aUser == null) {
                    Optional<ShareAccountTransaction> byReference
                            = shareAccountTransactionRepository.findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        aUser = byReference.get().getShareAccount().getUser();
                    }
                }
//                if (aUser == null) {
//                    Optional<DailySavingAccountTransaction> byReference
//                            = dailySavingAccountTransactionRepository.findByReferenceAndOrgId(user.getDailyCustomerNumber(), loggedInUser.getOrgId());
//                    if (byReference.isPresent()) {
//                        aUser = byReference.get().getDailySavingAccount().getUser();
//                    }
//                }
                if (aUser == null) { //LoanReference
                    Optional<LoanAccountTransaction> byReference = loanAccountTransactionService.
                            findByReferenceAndOrgId(user.getUserName(), loggedInUser.getOrgId());
                    if (byReference.isPresent()) {
                        LoanAccountTransaction loanAccountTransaction = byReference.get();
                        if (loanAccountTransaction != null) { //TODO: Identical code in loanAccountController
                            LoanAccount aLoanAccount = loanAccountTransaction.getLoanAccount();

                            List<LoanAccountTransaction> loanAccountTransactionList = aLoanAccount.getLoanAccountTransaction();
                            LoanBilanzList loanBilanzByUserList = loanAccountService.calculateAccountBilanz(loanAccountTransactionList, false, runtimeSetting.getCountryCode());
                            model.put("name", user.getUserName());
                            model.put("loanBilanzList", loanBilanzByUserList);
                            byReference.get().setLoanAccount(aLoanAccount);
                            model.put("loanAccountTransaction", loanAccountTransaction);
                            return "loanBilanzNoInterest";
                        }
                    }

                }
            }

            if (aUser != null && "ROLE_CUSTOMER".equals(aUser.getUserRole().get(0).getName())) {
                model.put("createSavingAccountEligible", true);
                model.put("createDailySavingAccountEligible", true);
                model.put("createLoanAccountEligible", true);
                model.put("createCurrentAccountEligible", true);
                model.put("createShareAccountEligible", true);
            } else if (aUser != null && BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER.equals(aUser.getUserRole().get(0).getName())) {

                model.put("createDailySavingAccountEligible", true);
                model.put("createLoanAccountEligible", true);
                model.put("createSavingAccountEligible", false);
                model.put("createShareAccountEligible", false);
                model.put("createCurrentAccountEligible", false);

            } else {
                model.put("createSavingAccountEligible", false);
                model.put("createDailySavingAccountEligible", false);
                model.put("createLoanAccountEligible", false);
                model.put("createCurrentAccountEligible", false);
                model.put("createShareAccountEligible", false);
            }

            if (null != aUser) {
                model.put("user", aUser); //TODO: stay consitent session or model
                request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, aUser);

                SavingBilanzList savingBilanzByUserList = savingAccountService.getSavingBilanzByUser(aUser, false, runtimeSetting.getCountryCode());
                SavingBilanzList dailySavingBilanzByUserList = dailySavingAccountService.getSavingBilanzByUser(aUser, false, runtimeSetting.getCountryCode());
                LoanBilanzList loanBilanzByUserList = loanAccountService.getLoanBilanzByUser(aUser, true, runtimeSetting.getCountryCode());
                CurrentBilanzList currentBilanzByUserList = currentAccountService.getCurrentBilanzByUser(aUser, false, runtimeSetting);
                ShareAccountBilanzList shareAccountBilanzList = shareAccountService.getShareAccountBilanzByUser(aUser, runtimeSetting.getCountryCode());

                request.getSession().setAttribute("savingBilanzList", savingBilanzByUserList);
                request.getSession().setAttribute("dailySavingBilanzList", dailySavingBilanzByUserList);
                request.getSession().setAttribute("loanBilanzList", loanBilanzByUserList);
                request.getSession().setAttribute("currentBilanzList", currentBilanzByUserList);
                request.getSession().setAttribute("shareAccountBilanzList", shareAccountBilanzList);

                if (aUser.getSavingAccount().size() == 0 && aUser.getDailySavingAccount().size() == 0 && aUser.getLoanAccount().size() == 0 &&
                        aUser.getShareAccount().size() == 0 && aUser.getCurrentAccount().size() == 0) {
                    model.put("name", getLoggedInUserName());
                    request.getSession().setAttribute("savingBilanzList", savingBilanzByUserList);
                    request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, aUser);
                    return "userHomeNoAccount";
                }
            }
        }
            if (aUser == null) {
                model.put("findUserInfo", "No user records found");
                return "welcome";
            } else {
                return "userHome";
            }
            
    }
    public ArrayList<String> getAllNonCustomers(long orgId) {
        ArrayList<String> customerList = generalLedgerRepository.findAllDistinctByCreatedBy(orgId);
        return customerList;

    }

    public ArrayList<User> getAllCustomers(long orgId) {
        ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
        UserRole customer = userRoleService.findUserRoleByName("ROLE_CUSTOMER", orgId);
        userRoleList.add(customer);
        ArrayList<User> customerList = userService.findAllByUserRoleIn(userRoleList, orgId);
        return customerList;
    }

    public ArrayList<User> getAllUsers(long orgId) {
        ArrayList<User> userList = userRepository.findByOrgId(orgId);
        return userList;
    }


    public String getLoggedInUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    public void generateByteOutputStream(HttpServletResponse response, String htmlInput) throws IOException {
        response.setContentType("application/pdf");
        ByteArrayOutputStream byteArrayOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        OutputStream responseOutputStream = null;
        try {
            responseOutputStream = response.getOutputStream();
            byteArrayOutputStream = pdfService.generatePDF(htmlInput, response);
            byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            int bytes;
            while ((bytes = byteArrayInputStream.read()) != -1) {
                responseOutputStream.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            responseOutputStream.close();
            responseOutputStream.flush();
            byteArrayInputStream.close();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
        }
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
    }


    public boolean containsAuthority(String userRole) {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().
                getAuthentication().getAuthorities();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities1 = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> itr = authorities1.iterator();
        String authority = "";
        while(itr.hasNext()) {
            GrantedAuthority element = (GrantedAuthority)itr.next();
            if(StringUtils.equals(userRole,element.getAuthority()))
                return true;
        }
        return false;
    }

    public void getEmployeesInModel(ModelMap model, long orgId) {
        ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
        UserRole customer = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_EMPLOYEE, orgId);
        userRoleList.add(customer);

        List<User> employees = userRepository.findAllByUserRoleInAndOrgId(userRoleList, orgId);
        model.put("employees", employees);
    }

}
