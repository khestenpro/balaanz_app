package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.model.response.AgentsMetaData;
import com.bitsvalley.micro.model.response.DailySavingAccountTransactions;
import com.bitsvalley.micro.model.response.DailyStats;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.AccountStatus;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.TrsansactionType;
import com.bitsvalley.micro.webdomain.CustomersDTO;
import com.bitsvalley.micro.webdomain.EventDTO;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class UserController extends SuperController {

    @Autowired
    private UserService userService;

    @Autowired
    SavingAccountTransactionService savingAccountTransactionService;

    @Autowired
    DailySavingAccountTransactionRepository dailySavingAccountTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserControlRepository userControlRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    CallCenterService callCenterService;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    BranchService branchService;

    @Autowired
    RuntimePropertiesRepository runtimePropertiesRepository;


    @Autowired
    NotificationService notificationService;

    @Autowired
    DailySavingsAccountService dailySavingsAccountService;

//    @Autowired
//    PasswordEncoder passwordEncoder;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }


    @GetMapping(value = "/registerUser")
    public String registerUser(ModelMap model) {
        User user = new User();
        model.put("user", user);
        return "user";
    }

    @GetMapping(value = "/registerUserCustomerPreview")
    public String registerUserPreview(ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        model.put("user", user);
        String userRoleTemp = (String) request.getSession().getAttribute("userRoleTemp");

        if (StringUtils.equals(userRoleTemp, BVMicroUtils.ROLE_CUSTOMER))
            return "userCustomer";
        return "user";
    }

    @GetMapping(value = "/registerCustomer")
    public String registerCustomer(ModelMap model) {
        User user = new User();
        model.put("user", user);
        return "userCustomer";
    }

    @GetMapping(value = "/reloadUser/{id}")
    public String reloadUser(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        Optional<User> userById = userRepository.findById(id);
        User user = userById.get();
        model.put("user", user);
        return "reloadUser";
    }


    @GetMapping(value = "/updateProfile")
    public String updateProfile(ModelMap model) {
        String loggedInUserName = getLoggedInUserName();
        User user = userRepository.findByUserName(loggedInUserName);
        user.setTerminalCode("");
        model.put("user", user);
        return "updateProfile";
    }

    @GetMapping(value = "/updateProfile/{id}")
    public String updateProfileCustomer(@PathVariable("id") long id, ModelMap model) {
        String loggedInUserName = getLoggedInUserName();
        if (loggedInUserName == null) {
            return "login";
        }
        User user = userRepository.findById(id).get();
        model.put("user", user);
        return "updateProfile";
    }

    @GetMapping(value = "/updateUserProfile/{id}")
    public String updateUserProfileCustomer(@PathVariable("id") long id, ModelMap model) {
        String loggedInUserName = getLoggedInUserName();
        if (loggedInUserName == null) {
            return "login";
        }
        User user = userRepository.findById(id).get();
        model.put("user", user);
        return "updateUserProfile";
    }


    @PostMapping(value = "/updateUserEmail")
    public String updateUserEmail(HttpServletRequest request, ModelMap model) {
        String id = request.getParameter("userId");
        String newEmail = request.getParameter("userEmail");
        User currentUser = userRepository.findById(new Long(id)).get();
        currentUser.setEmail(newEmail);
        userRepository.save(currentUser);
        CustomersDTO customersDTO = userService.extractCustomers(currentUser);
        model.put("customersDTO", customersDTO);
        return "customers";
    }

    @PostMapping(value = "/updateUserTelephone1")
    public String updateTelephone(HttpServletRequest request, ModelMap model) {
        String id = request.getParameter("userId");
        String input = request.getParameter("userTelephone1");
        String[] pieces = input.split(":");
        User currentUser = userRepository.findById(Long.parseLong(id)).get();

        UserControl userControl = currentUser.getUserControl() == null ? new UserControl() : currentUser.getUserControl();

        if (pieces[0] != null)
            currentUser.setTelephone1(pieces[0]);

        if (pieces[1] != null && pieces[1].equalsIgnoreCase("true")) {
            userControl.setMobileMoneyActive(true);
        } else {
            userControl.setMobileMoneyActive(false);
        }

        if (pieces[2] != null) {
            try {
                userControl.setMobileMoneyDailyLimit(Double.parseDouble(pieces[2]));
            } catch (NumberFormatException e) {
                CustomersDTO customersDTO = userService.extractCustomers(currentUser);
                model.put("customersInfo", "Enter Valid amount. Number");
                model.put("customersDTO", customersDTO);
                return "customers";
            }
//            currentUser.getUserControl().setMobileMoneyDailyLimit(10*Double.parseDouble(pieces[2]));
        } else {
            userControl.setMobileMoneyDailyLimit(0);
        }

        userControlRepository.save(userControl);
        currentUser.setUserControl(userControl);

        userRepository.save(currentUser);
        CustomersDTO customersDTO = userService.extractCustomers(currentUser);
        model.put("customersDTO", customersDTO);
        return "customers";
    }


    @Transactional
    @PostMapping(value = "/registerUserPreviewForm")
    public String registerUserPreviewForm(@ModelAttribute("user") User user, ModelMap model, HttpServletRequest request) {
        String aUserRole = (String) request.getParameter("userRoleTemp");
        user = getUserRoleFromRequest(user, aUserRole);
        String loggedInUserName = getLoggedInUserName();
        user.setCreatedBy(loggedInUserName);
        User aUser = userRepository.findByUserName(getLoggedInUserName());
        if (aUser == null) {
            user.setOrgId(0);
        } else {
            user.setOrgId(aUser.getOrgId());
        }

//        int generalCustomerCounts = userRepository.countByUserRole(BVMicroUtils.ROLE_CUSTOMER, user.getOrgId());
        if (StringUtils.equals(user.getUserRole().get(0).getName(), BVMicroUtils.ROLE_CUSTOMER)) {

            // TODO: replace with count customers QUERY
            ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
            UserRole customer = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_CUSTOMER, user.getOrgId());
            userRoleList.add(customer);
            ArrayList<User> customerList = userService.findAllByUserRoleIn(userRoleList, aUser.getOrgId());
            String generalCustomerCount = BVMicroUtils.leftJustify("0", 7, String.valueOf(customerList.size() + 0000001));
            user.setCustomerNumber("10" + BVMicroUtils.getTwoDigitInt(aUser.getOrgId()) + generalCustomerCount);
        } else if (StringUtils.equals(user.getUserRole().get(0).getName(), BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER)) {

            // TODO: replace with count customers QUERY
            ArrayList<UserRole> userRoleList = new ArrayList<UserRole>();
            UserRole dailyCustomer = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER, user.getOrgId());
            userRoleList.add(dailyCustomer);
            ArrayList<User> dailyCustomerList = userService.findAllByUserRoleIn(userRoleList, aUser.getOrgId());
            String generalDailyCustomerCount = BVMicroUtils.leftJustify("0", 7, String.valueOf(dailyCustomerList.size() + 0000001));
            user.setDailyCustomerNumber("11" + BVMicroUtils.getTwoDigitInt(aUser.getOrgId()) + generalDailyCustomerCount);
            user.setCustomerNumber("11" + generalDailyCustomerCount);
//            int generalCustomerCountsss = userRepository.countByUserRole(BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER, user.getOrgId());

        }

        if (user.getId() > 0) { //TODO: hmmm operations movin' accounts
            Optional<User> byId = userRepository.findById(user.getId());
            List<SavingAccount> savingAccount = byId.get().getSavingAccount();
            user.setSavingAccount(savingAccount);
            userService.saveUser(user);
        } else {

            Branch branch = branchService.getBranchInfo(loggedInUserName);
            user.setBranch(branch);
//            user.setAccountStatus(AccountStatus.PENDING_APPROVAL);

            if (userRepository.findByUserName(user.getUserName()) == null) {

                //init a User Control
                UserControl userControl = new UserControl();
                userControlRepository.save(userControl);
                user.setUserControl(userControl);
                user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
                userService.createUser(user);
                RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

                if (StringUtils.isBlank(user.getEmail()) && request.getParameter("sendWelcomeEmail") != null && request.getParameter("sendWelcomeEmail").equals("on"))
                    notificationService.sampleWelcomeEmail(runtimeSetting, user);

            } else {
                model.put("updatedInfo", "Select a different username  ... ");
                model.put("user", user);
                return "reloadUser";
            }

        }
        return findUserByUsername(user, model, request);
    }

    @GetMapping(value = "/laChance21")
    public String newOrg(ModelMap model) {
        User user = new User();
        model.put("user", user);
        return "userOrg";
    }


    @Transactional
    @PostMapping(value = "/createNewOrg")
    public String createNewOrg(@ModelAttribute("user") User user, ModelMap model, HttpServletRequest request) {

        long orgId = new Long(request.getParameter("orgId"));
        Iterable<RuntimeProperties> byOrgId = runtimePropertiesRepository.findByOrgId(orgId);
        if (byOrgId != null && byOrgId.iterator().hasNext()) {
            return "userOrg";
        }
        Branch branch = branchService.createBranch(orgId);
        user.setBranch(branch);

        if (userRepository.findByUserName(user.getUserName()) != null) {
            model.put("errorUserInfo", "Username already exists.");
            return "userOrg";
        }

        User savedUser = userService.createUser(user);
        if (null == savedUser) {
            model.put("updatedInfo", "Select a different username  ... ");
            model.put("user", user);
            return "reloadUser";
        }

        Long fromOrgId = new Long(request.getParameter("fromOrgId"));

        RuntimeSetting runtimeSetting = new RuntimeSetting();
        createRuntimeOrgProperties(runtimeSetting, orgId);

        user = userRepository.findByUserName(user.getUserName());
//        Iterable<UserRole> all = userRoleService.findAll();

        List<UserRole> userRoleList = new ArrayList<UserRole>();

        UserRole roleAdmin = userRoleRepository.findByName(BVMicroUtils.ROLE_ADMIN);
        userRoleList.add(roleAdmin);

        user.setUserRole(userRoleList);
        initNewLedgerAccounts(orgId, fromOrgId);

        userRepository.save(user);

        model.put("updatedInfo", "Created new ORGANIZATION ... ");
        model.put("user", user);
        return "reloadUser";

    }

//    //initializes a new new org ledger accounts
//    @GetMapping(value = "/createNewOrgAccounts/{orgId}")
//    public void createNewLedgerAccounts(@PathVariable("orgId") long orgId) {
//        initNewLedgerAccounts(orgId);
//        initNewAccountTypes(orgId);
//
//    }

    //initializes a new new org ledger accounts
    @GetMapping(value = "/createNewOrgAccountType/{orgId}")
    public void createNewOrgAccountType(@PathVariable("orgId") long orgId) {
        initNewAccountTypes(orgId);
    }

    private void initNewAccountTypes(long newOrgId) {
        List<AccountType> allOrgId = accountTypeRepository.findByOrgIdAndActiveTrue(0);
        List<AccountType> newOrgAccountTypes = new ArrayList<AccountType>();
        for (AccountType accountType : allOrgId) {
            AccountType at = new AccountType();
            at.setName(accountType.getName());
            at.setCategory(accountType.getCategory());
            at.setOrgId(newOrgId);
            at.setDisplayName(accountType.getDisplayName());
            at.setNumber(accountType.getNumber());
            newOrgAccountTypes.add(at);
        }
        accountTypeRepository.saveAll(newOrgAccountTypes);
    }

    private void initNewLedgerAccounts(long newOrgId, long fromOrgId) {
//        List<LedgerAccount> allOrgId = ledgerAccountRepository.findByOrgIdAndActiveTrue(fromAccount);
        List<LedgerAccount> allOrgId = ledgerAccountRepository.findByOrgId(fromOrgId);

        List<LedgerAccount> newOrgLedgerAccounts = new ArrayList<LedgerAccount>();
        for (LedgerAccount ledgerAccount : allOrgId) {
            LedgerAccount la = new LedgerAccount();
            la.setName(ledgerAccount.getName());
            la.setDisplayName(ledgerAccount.getDisplayName());
            la.setCode(ledgerAccount.getCode());
            la.setInterAccountTransfer(ledgerAccount.getInterAccountTransfer());
            la.setCashTransaction(ledgerAccount.getCashTransaction());
            la.setStatus(ledgerAccount.getStatus());
            la.setCreditBalance(ledgerAccount.getCreditBalance());
            la.setCreatedBy("system");
            la.setCreatedDate(new Date());
            la.setActive(ledgerAccount.isActive());
            la.setCategory(ledgerAccount.getCategory());
            la.setOrgId(newOrgId);
            newOrgLedgerAccounts.add(la);
        }
        ledgerAccountRepository.saveAll(newOrgLedgerAccounts);
    }


    @PostMapping(value = "/updateBeneficiaryForm")
    public String updateBeneficiaryForm(ModelMap model, HttpServletRequest request) {

        ArrayList<Beneficiary> beneficiaryList = getBeneficiaryList(request);
        if ((beneficiaryList == null || beneficiaryList.size() == 0)) {
            model.put("updatedInfo", "Enter a Beneficiary");
        }

        String userId = request.getParameter("userId");
        User aUser = userRepository.findById(Long.parseLong(userId)).get();

        String beneficiaryInfo = userService.getBeneficiary(aUser.getUserRole().get(0).getName(), beneficiaryList);
        if ((beneficiaryInfo != null)) {
            model.put("updatedInfo", beneficiaryInfo);

        } else {
            aUser.setBeneficiary(beneficiaryList);
            userRepository.save(aUser);
            model.put("updatedInfo", "Updated Beneficiary");
        }
        model.put("reloadUserInfo", "true");
        model.put("user", aUser);
        return "reloadUser";

    }

    @PostMapping(value = "/registerUserForm")
    public String registerUserForm(@ModelAttribute("user") User user, ModelMap model, HttpServletRequest request) {
        String aUserRole = (String) request.getParameter("aUserRole");
        String gender = (String) request.getParameter("gender");
        String returnPage = "";
        if (StringUtils.equals(aUserRole, BVMicroUtils.ROLE_CUSTOMER) || StringUtils.equals(aUserRole, BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER)) {
            returnPage = "userCustomer";
        } else {
            returnPage = "user";
        }

        User byUserName = userRepository.findByUserName(user.getUserName());
        if (byUserName != null) {
            model.put("updatedError", "Select a different username");
            model.put("user", user);
            return returnPage;
        }

        if (!BVMicroUtils.isValidPassword(user.getPassword())) {
            model.put("updatedError", "Enter a valid Password");
            model.put("user", user);
            return returnPage;
        }

        if (BVMicroUtils.formatLocaleDate(user.getIdentityCardExpiry()).isBefore(LocalDateTime.now())) {
            model.put("updatedError", "User Identity has expired");
            model.put("user", user);
            return returnPage;
        }

        List<User> users = userRepository.findByIdentityCardNumber(user.getIdentityCardNumber());
        if (users != null && users.size() > 0) {
            model.put("updatedError", "User Identity Number already exists in the system");
            model.put("user", user);
            return returnPage;
        }

        if (!BVMicroUtils.formatLocaleDate(user.getDateOfBirth()).plusYears(15).isBefore(LocalDateTime.now())) {
            model.put("updatedError", "Date of birth must be 15yrs old");
            model.put("user", user);
            return returnPage;
        }

        if (user.getId() != 0) {
            User byId = userRepository.findById(user.getId()).get();
            if (byId.getUserRole().size() != 1 && !byId.getUserRole().get(0).getName().equals("ROLE_CUSTOMER")) {
                model.put("updatedError", "Only Role Customer can be updated");
                model.put("user", user);
                return returnPage;
            }
        }
        User aUser = userRepository.findByUserName(getLoggedInUserName());
        if (null == aUser) {
            user.setOrgId(0);
        } else {
            user.setOrgId(aUser.getOrgId());
        }
        if (user.getId() < 1) {
            String errorMessage = userService.getBeneficiary(aUserRole, getBeneficiaryList(request));
            if (errorMessage != null) {
                model.put("updatedError", errorMessage);
            }
        } else {
            User byId = userRepository.findById(user.getId()).get();
            user.setUserName(byId.getUserName());
        }

        user.setGender(gender); //TODO: Check thymeleaf! should map automatically
        model.put("userRoleTemp", aUserRole);
        request.getSession().setAttribute("userRoleTemp", aUserRole);
        model.put("user", user);
        request.getSession().setAttribute("user", user);

        if (returnPage.equals("userCustomer")) {
            return "userCustomerSavedPreview";
        } else {
            return "userSavedPreview";
        }
    }


    @PostMapping(value = "/updateUserControl")
    public String updateUserControl(@ModelAttribute("user") User user, ModelMap model) {
        UserControl userControl = user.getUserControl();
        User currentUser = userRepository.findById(user.getId()).get();

        if (currentUser.getUserControl() == null) {
            currentUser.setUserControl(new UserControl());
        }

        model.put("updatedInfo", "Customer Access Successfully Updated ... ");
        User aUser = userRepository.findById(user.getId()).get();
        model.put("user", aUser);

        //TODO: Not clean
        if ("ROLE_CUSTOMER".equals(aUser.getUserRole().get(0).getName())) {
            currentUser.getUserControl().setCurr2currActive(userControl.isCurr2currActive());
            currentUser.getUserControl().setCurr2currLimit(userControl.getCurr2currLimit());
            currentUser.getUserControl().setMobileMoneyDailyLimit(userControl.getMobileMoneyDailyLimit());
            currentUser.getUserControl().setMobileMoneyActive(userControl.isMobileMoneyActive());
            currentUser.getUserControl().setMobileMoneyMonthlyLimit(userControl.getMobileMoneyMonthlyLimit());
            userRepository.save(currentUser);
            return "updateProfile";
        }else{
            currentUser.getUserControl().setNotSignedCollectionLimit(userControl.getNotSignedCollectionLimit());
            userRepository.save(currentUser);
            return "updateUserProfile";
        }
    }


    @PostMapping(value = "/updatePasswordForm")
    public String updatePassword(@ModelAttribute("user") User user, ModelMap model) {
        String inputPassword = user.getPassword();
        User aUser = userRepository.findById(user.getId()).get();
        if (StringUtils.isNotEmpty(inputPassword) && !BVMicroUtils.isValidPassword(inputPassword)) {
            model.put("updatedError", "Not Updated Password (Min 8 Chars & Special Char)");
            aUser.setTerminalCode("******");
            model.put("user", aUser);
            return "updateProfile";
        }
        if (StringUtils.isNotEmpty(inputPassword)) {
            String pass = userService.enccodeInput(inputPassword);
            aUser.setPassword(pass);
        }

        if (StringUtils.isNotEmpty(user.getEmail())) {
            aUser.setTelephone1(user.getEmail());
        }

        if (StringUtils.isNotEmpty(user.getTelephone1())) {
            aUser.setTelephone1(user.getTelephone1());
        }

        if (StringUtils.isNotEmpty(user.getTelephone2())) {
            aUser.setTelephone2(user.getTelephone2());
        }

        if (StringUtils.isNotEmpty(user.getUserName())) {
            aUser.setUserName(user.getUserName());
        }

        if (StringUtils.isNotEmpty(user.getFirstName())) {
            aUser.setFirstName(user.getFirstName());
        }

        if (StringUtils.isNotEmpty(user.getLastName())) {
            aUser.setLastName(user.getLastName());
        }

        if (StringUtils.isNotEmpty(user.getAddress())) {
            aUser.setAddress(user.getAddress());
        }

        if (StringUtils.isNotEmpty(user.getEmail())) {
            aUser.setEmail(user.getEmail());
        }

        if (StringUtils.isNotEmpty(user.getReferral())) {
            aUser.setReferral(user.getReferral());
        }

        if (user.isReceiveEmailNotifications()) {
            aUser.setReceiveEmailNotifications(user.isReceiveEmailNotifications());
        }

        if (StringUtils.isNotEmpty(user.getTerminalCode())) {
            aUser.setTerminalCode(userService.enccodeInput(user.getTerminalCode()));
        }

        userRepository.save(aUser);
        model.put("updatedInfo", "Profile Successfully Updated ... ");
        aUser.setTerminalCode("");
        model.put("user", aUser);
        return "updateProfile";
    }


    @NotNull
    private ArrayList<Beneficiary> getBeneficiaryList(HttpServletRequest request) {

        String perc1 = (String) request.getParameter("perc1");
        String perc2 = (String) request.getParameter("perc2");
        String perc3 = (String) request.getParameter("perc3");
        String perc4 = (String) request.getParameter("perc4");
        String perc5 = (String) request.getParameter("perc5");


        String beneficiary1 = (String) request.getParameter("beneficiary1");
        String beneficiary2 = (String) request.getParameter("beneficiary2");
        String beneficiary3 = (String) request.getParameter("beneficiary3");
        String beneficiary4 = (String) request.getParameter("beneficiary4");
        String beneficiary5 = (String) request.getParameter("beneficiary5");

        String relation1 = (String) request.getParameter("relation1");
        String relation2 = (String) request.getParameter("relation2");
        String relation3 = (String) request.getParameter("relation3");
        String relation4 = (String) request.getParameter("relation4");
        String relation5 = (String) request.getParameter("relation5");

        String notes1 = (String) request.getParameter("notes1");
        String notes2 = (String) request.getParameter("notes2");
        String notes3 = (String) request.getParameter("notes3");
        String notes4 = (String) request.getParameter("notes4");
        String notes5 = (String) request.getParameter("notes5");


        ArrayList<Beneficiary> beneficiaryList = new ArrayList<Beneficiary>();

        if (StringUtils.isNotEmpty(perc1) && StringUtils.isNotEmpty(beneficiary1)) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setName(beneficiary1);
            beneficiary.setPercentage(perc1);
            beneficiary.setRelation(relation1);
            beneficiary.setNotes(notes1);
            beneficiaryList.add(beneficiary);
        }

        if (StringUtils.isNotEmpty(perc2) && StringUtils.isNotEmpty(beneficiary2)) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setName(beneficiary2);
            beneficiary.setPercentage(perc2);
            beneficiary.setRelation(relation2);
            beneficiary.setNotes(notes2);
            beneficiaryList.add(beneficiary);
        }

        if (StringUtils.isNotEmpty(perc3) && StringUtils.isNotEmpty(beneficiary3)) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setName(beneficiary3);
            beneficiary.setPercentage(perc3);
            beneficiary.setRelation(relation3);
            beneficiary.setNotes(notes3);
            beneficiaryList.add(beneficiary);
        }

        if (StringUtils.isNotEmpty(perc4) && StringUtils.isNotEmpty(beneficiary4)) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setName(beneficiary4);
            beneficiary.setPercentage(perc4);
            beneficiary.setRelation(relation4);
            beneficiary.setNotes(notes4);
            beneficiaryList.add(beneficiary);
        }

        if (StringUtils.isNotEmpty(perc5) && StringUtils.isNotEmpty(beneficiary5)) {
            Beneficiary beneficiary = new Beneficiary();
            beneficiary.setName(beneficiary5);
            beneficiary.setPercentage(perc5);
            beneficiary.setRelation(relation5);
            beneficiary.setNotes(notes5);
            beneficiaryList.add(beneficiary);
        }
        return beneficiaryList;
    }

    private User getUserRoleFromRequest(User user, String aUserRoleInput) {
        UserRole aUserRole = userRoleService.findUserRoleByName(aUserRoleInput, user.getOrgId());
        if (aUserRole == null) {
            aUserRole = new UserRole();
            aUserRole.setName(aUserRoleInput);
            userRoleService.saveUserRole(aUserRole, user.getOrgId());
            aUserRole = userRoleService.findUserRoleByName(aUserRoleInput, user.getOrgId());
        }
        ArrayList<UserRole> roles = new ArrayList<UserRole>();
        roles.add(aUserRole);
        user.setUserRole(roles);
        return user;
    }

    @PostMapping(value = "/findUserByUserName")
    public String findUserByUsername(ModelMap model, HttpServletRequest request) {
        User user = new User();
        user.setUserName(request.getParameter("aUserName"));
        return findUserByUserName(user, model, request);
    }

    public String findUserByUsername(User user, ModelMap model, HttpServletRequest request) {
        return findUserByUserName(user, model, request);
    }

    @GetMapping(value = "/findAllCustomers")
    public String findUserByUserRole(ModelMap model) {
        CustomersDTO customersDTO = new CustomersDTO();

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        User user = userRepository.findByUserName(getLoggedInUserName());

        if (null != user) {
            customersDTO = userService.extractCustomers(user);
        }
//        else {
//            customersDTO.setUserList(userRepository.findByOrgId(0)); //Not sure if needed
//        }
        model.put("customersDTO", customersDTO);
        return "customers";
    }

    @GetMapping(value = "/findAllAgents")
    public String findAgents(ModelMap model) {
        AgentsMetaData customersDTO = new AgentsMetaData();
        User user = userRepository.findByUserName(getLoggedInUserName());
        if (null != user) {
            customersDTO = userService.extractAgents(user);
        }
        model.put("customersDTO", customersDTO);
        return "agents";
    }


    @GetMapping(value = "/showCustomer/{id}")
    public String showCustomer(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        Optional<User> userById = userRepository.findById(id);
        User user = userById.get();
        return findUserByUserName(user, model, request);
    }

    @GetMapping(value = "/memberStatus/{status}")
    public String showCustomerStatus(@PathVariable("status") String status, ModelMap model) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        AccountStatus accountStatus = AccountStatus.valueOf(status);
        List<User> usersList = userRepository.findByOrgIdAndAccountStatus(user.getOrgId(), accountStatus);
        model.put("userList", usersList);
        return "customers";
    }


    @GetMapping(value = "/editAccountStatus/{id}")
    public String showCustomerStatus(@PathVariable("id") long id, ModelMap model) {
        User userById = userRepository.findById(id).get();
        List<User> usersList = null;
        if (userById.getAccountStatus().name().equals(AccountStatus.ACTIVE.name())) {
            userById.setAccountStatus(AccountStatus.IN_ACTIVE);
            userRepository.save(userById);
        } else if (userById.getAccountStatus().name().equals(AccountStatus.IN_ACTIVE.name())) {
            userById.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(userById);
        }
        else if (userById.getAccountStatus().name().equals(AccountStatus.PENDING_APPROVAL.name())) {
            userById.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(userById);
        }
        return findUserByUserRole(model);
    }

    @GetMapping(value = "/editAgentAccountStatus/{id}")
    public String showAgentStatus(@PathVariable("id") long id, ModelMap model) {
        User userById = userRepository.findById(id).get();
        List<User> usersList = null;
        if (userById.getAccountStatus().name().equals(AccountStatus.ACTIVE.name())) {
            userById.setAccountStatus(AccountStatus.IN_ACTIVE);
            userRepository.save(userById);
        } else if (userById.getAccountStatus().name().equals(AccountStatus.IN_ACTIVE.name())) {
            userById.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(userById);
        }
        return findAgents(model);
    }

    @GetMapping(value = "/getAgentStats/{id}")
    public String getAgentStats(@PathVariable("id") long id, ModelMap model) {
        User userById = userRepository.findById(id).get();
        List<DailyStats> dailyStats = dailySavingsAccountService.dailyStats(String.valueOf(userById.getId()));
        List<DailySavingAccountTransactions> dailySavingAccountTransactions = dailySavingsAccountService
          .agentTransactions((userById.getId()), TrsansactionType.COLLECTED.name());
        model.put("dailyStats", dailyStats);
        model.put("dailySavingAccountTransactions", dailySavingAccountTransactions);
        model.addAttribute("agentId", id);
        return "agentstats";
    }

    @PostMapping("/submitRemittanceConfirmation")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public String handleSelectedTransactions(@RequestParam("id") Long id,
                                             @RequestParam(value = "selectedItems", required = false)
                                          List<Long> selectedItems, ModelMap model) {
        if (selectedItems != null) {
            // Process the selected transactions IDs
            dailySavingsAccountService.remitTransactions(selectedItems);
        }
        return getAgentStats(id, model); // Redirect back to the agent stats page after processing
    }


    @GetMapping(value = "/editUserRole/{id}")
    public String editUserRole(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findById(id).get();
        UserRole role_customer = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_CUSTOMER, 0);
        UserRole role_daily_customer = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_DAILY_COLLECTION_CUSTOMER, 0);
        if (user.getUserRole().contains(role_customer) || user.getUserRole().contains(role_daily_customer)) {
////            if(null != user.getAccountStatus()) {
////                if(StringUtils.equals(AccountStatus.ACTIVE.name(),user.getAccountStatus().name())){
//            model.put("error", "Cannot Edit A Customer Role");
//            model.put("userList", userRepository.findByOrgId(user.getOrgId()));
//            model.put("name", getLoggedInUserName());
//            return "customers";
//        } else if (StringUtils.equals(AccountStatus.PENDING_APPROVAL.name(), user.getAccountStatus().name())) {
//            model.put("user", user);
            return "welcome";
        }


        model.put("user", user);

        List<UserRole> currentUserRoles = user.getUserRole();

        List<UserRole> allUserRoles = userRoleRepository.findByOrgId(0);
        allUserRoles.remove(role_daily_customer);
        allUserRoles.remove(role_customer);

        currentUserRoles = updateRoleNamesWithContextName(request, currentUserRoles);
        allUserRoles = updateRoleNamesWithContextName(request, allUserRoles);

        model.put("currentUserRoles", currentUserRoles);
        model.put("allUserRoles", separateList(currentUserRoles, allUserRoles));

        return "editUserRole";
    }

    private List<UserRole> updateRoleNamesWithContextName(HttpServletRequest request, List<UserRole> currentUserRoles) {
        List<UserRole> allContextNameRoles = new ArrayList<UserRole>();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        for (UserRole aUserRole : currentUserRoles) {
            if (aUserRole.getName().contains("CUSTOMER")) {
                aUserRole.setName(aUserRole.getName().replace("CUSTOMER", runtimeSetting.getContextName().toUpperCase()));
                allContextNameRoles.add(aUserRole);
            }
//            if (StringUtils.equals(aUserRole.getName(), aUserRole.getName())) {
//                allUserRoles.remove(aUserRole);
//            }
        }
        return currentUserRoles;
    }

    private List<UserRole> separateList(List<UserRole> currentUserRoles, List<UserRole> remainderUserRoles) {

        for (UserRole aUserRole : currentUserRoles) {
            if (remainderUserRoles.contains(aUserRole)) {
                remainderUserRoles.remove(aUserRole);
            }
        }
        return remainderUserRoles;
    }

    @GetMapping(value = "/removeRole/{id}/{userId}")
    public String removeRole(@PathVariable("id") long id, @PathVariable("userId") long userId, ModelMap model,
                             HttpServletRequest request) {
//        String userId1 = request.getParameter("userId");
        long aUserId = new Long(userId);
        UserRole byId = userRoleRepository.findById(id).get();
        User aUser = userRepository.findById(aUserId).get();
        aUser.getUserRole().remove(byId);
        userRepository.save(aUser);

        return editUserRole(userId, model, request);

    }

    @GetMapping(value = "/addRole/{id}/{userId}")
    public String addRole(@PathVariable("id") long id, @PathVariable("userId") long userId, ModelMap model,
                          HttpServletRequest request) {
        UserRole byId = userRoleRepository.findById(id).get();
        User aUser = userRepository.findById(userId).get();
        aUser.getUserRole().add(byId);
        userRepository.save(aUser);

        return editUserRole(userId, model, request);

    }


    @PostMapping(value = "/registerEventForm")
    public String registerEventForm(ModelMap model, HttpServletRequest request) {
        String[] bulkCustomers = request.getParameterValues("bulkCustomer");
        LedgerAccount bulkLedgerSelected = (LedgerAccount) request.getSession().getAttribute("bulkLedgerSelected");
        request.getSession().setAttribute("bulkCustomersSelected", bulkCustomers);

        return "newEvent";

    }

    @PostMapping(value = "/saveEventReview")
    public String saveEventReview(ModelMap model, HttpServletRequest request) {
        //TODO Use Event Object

        EventDTO eventDTO = new EventDTO();
        eventDTO.setEventAmount(new Double(request.getParameter("eventAmount")));
        eventDTO.setEventDescription(request.getParameter("eventDescription"));
        eventDTO.setLedgerAccount((LedgerAccount) request.getSession().getAttribute("bulkLedgerSelected"));
        eventDTO.setBulkCustomers((String[]) request.getSession().getAttribute("bulkCustomersSelected"));

        User user = userRepository.findByUserName(getLoggedInUserName());
        eventDTO.setOrgId(user.getOrgId());
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());//TODO Create branch repo

        userService.createBulkEvent(eventDTO, branchInfo);
        model.put("newEventInfo", "Event transaction created");
        return "newEvent";

    }


    @PostMapping(value = "/editUserRoleForm")
    public String editUserRoleForm(ModelMap model, HttpServletRequest req, @ModelAttribute("user") User user) {

        User aUser = userRepository.findById(user.getId()).get();
        String[] userRole = req.getParameterValues("aUserRole");
        int length = userRole.length;

        List<UserRole> userRolesList = new ArrayList<UserRole>();
        boolean userRoleCustomerExists = false;
        for (String newUserRole : userRole) {
            if (StringUtils.equals("ROLE_CUSTOMER", newUserRole)) {
                userRoleCustomerExists = true;
            }
            UserRole aUserRole = userRoleService.findUserRoleByName(newUserRole, user.getOrgId());
            if (null == aUserRole) {
                aUserRole = new UserRole();
                aUserRole.setName(newUserRole);
                userRoleService.saveUserRole(aUserRole, user.getOrgId());
            }
            userRolesList.add(aUserRole);
        }

        if (length > 1 && userRoleCustomerExists) {
            model.put("user", aUser);
            model.put("updatedInfoError", "You cannot select ROLE_CUSTOMER");
            return "editUserRole";
        }

        aUser.setUserRole(userRolesList);
        userRepository.save(aUser);
        model.put("user", aUser);
        model.put("updatedInfo", "User Role updated ");
        return "editUserRole";
    }

    @GetMapping(value = "/lockAccount/{id}")
    public String lockAccount(@PathVariable("id") long id, ModelMap model,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        Optional<User> userById = userRepository.findById(id);
        User user = userById.get();
        user.setAccountLocked(!user.isAccountLocked());
        userService.saveUser(user);
        String blocked = user.isAccountLocked() ? "Blocked" : "UnBlocked";
        callCenterService.callCenterUserAccount(user, "Account has been switched " + "Account is now " + blocked + "by " + getLoggedInUserName());
        model.put("userList", userRepository.findByOrgId(user.getOrgId()));
        model.put("name", getLoggedInUserName());
        model.put("customersDTO", userService.extractCustomers(user));
        return "customers";
    }

    @GetMapping(value = "/lockAgentAccount/{id}")
    public String lockAgentAccount(@PathVariable("id") long id, ModelMap model) {
        lockAccountForOnlineBanking(id);
        return findAgents(model);
    }

    private void lockAccountForOnlineBanking(long id) {
        Optional<User> userById = userRepository.findById(id);
        User user = userById.get();
        user.setAccountLocked(!user.isAccountLocked());
        userService.saveUser(user);
        String blocked = user.isAccountLocked() ? "Blocked" : "UnBlocked";
        callCenterService.callCenterUserAccount(user, "Account has been switched " + "Account is now " + blocked + "by " + getLoggedInUserName());
    }

    @GetMapping(value = "/createSavingAccountReceiptPdf/{id}")
    public void savingReceiptPDF(@PathVariable("id") long id, ModelMap model,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_saving_receipt.pdf");
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        Optional<SavingAccountTransaction> savingAccountTransaction = savingAccountTransactionService.findById(new Long(id));
        SavingAccountTransaction aSavingAccountTransaction = savingAccountTransaction.get();
        boolean displayBalance = containsAuthority(BVMicroUtils.ROLE_ACCOUNT_BALANCES);
        String htmlInput = pdfService.generateSavingTransactionReceiptPDF(aSavingAccountTransaction, initSystemService.findByOrgId(user.getOrgId()), displayBalance);
        generateByteOutputStream(response, htmlInput);
    }

    @GetMapping(value = "/createDailySavingAccountReceiptPdf/{id}")
    public void savingDailySavingAccountReceiptPDF(@PathVariable("id") long id, ModelMap model,
                                                   HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=" + id + "dailySaving_receipt.pdf");
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String n= auth.getName();
//        String r= auth.getAuthorities().toString();


        Optional<DailySavingAccountTransaction> savingAccountTransaction = dailySavingAccountTransactionRepository.findById(new Long(id));
        DailySavingAccountTransaction aSavingAccountTransaction = savingAccountTransaction.get();
        boolean displayBalance = containsAuthority(BVMicroUtils.ROLE_ACCOUNT_BALANCES);
        String htmlInput = pdfService.generateSavingTransactionReceiptPDF(aSavingAccountTransaction, initSystemService.findByOrgId(user.getOrgId()), displayBalance);
        generateByteOutputStream(response, htmlInput);
    }


    @GetMapping(value = "/bulkProcessCustomer")
    public void bulkProcessCustomer() {
        String loggedInUserName = getLoggedInUserName();
        User user = userRepository.findByUserName(loggedInUserName);

        ArrayList<UserRole> customers = new ArrayList<UserRole>();
        UserRole roleByName = userRoleRepository.findByName(BVMicroUtils.ROLE_CUSTOMER);
        customers.add(roleByName);
//        ArrayList<User> allActiveCustomers = userRepository.findAllByUserRoleInAndOrgIdAndAccountStatus(customers, user.getOrgId(),AccountStatus.ACTIVE);
//        ArrayList<User> allInActiveCustomers = userRepository.findAllByUserRoleInAndOrgIdAndAccountStatus(customers, user.getOrgId(),AccountStatus.IN_ACTIVE);

    }


}









