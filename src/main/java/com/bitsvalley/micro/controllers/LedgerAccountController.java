package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.CobacCodes;
import com.bitsvalley.micro.webdomain.CustomersDTO;
import com.bitsvalley.micro.webdomain.LedgerEntryDTO;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class LedgerAccountController extends SuperController{

    private static final String GENERAL_LEDGER_COBAC = "generalLedgerClass";

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    LedgerAccountService ledgerAccountService;

    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    SavingAccountRepository savingAccountRepository;

    @Autowired
    SavingAccountService savingAccountService;

    @Autowired
    CallCenterService callCenterService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GeneralLedgerService generalLedgerService;

    @Autowired
    GeneralLedgerRepository generalLedgerRepository;

    @Autowired
    UserService userService;


    @GetMapping(value = "/newLedgerAccount")
    public String initLedgerAccount(ModelMap model, HttpServletRequest request) {

        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        if(CollectionUtils.isEmpty( ledgerAccountRepository.findByOrgId(user.getOrgId()))){
            //init ledgerAccounts
            generalLedgerService.getLedgerAccounts(user);
        }
        extracted(model, user.getOrgId());
        return "ledgerAccount";
    }

    @GetMapping(value = "/newCobacLedgerAccount")
    public String initCobacLedgerAccount(ModelMap model, HttpServletRequest request) {

        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        if(CollectionUtils.isEmpty( ledgerAccountRepository.findByOrgId(user.getOrgId()))){
            //init ledgerAccounts
            generalLedgerService.getLedgerAccounts(user);
        }
        extracted(model, user.getOrgId());
        return "cobacLedgerAccount";
    }

    @PostMapping(value = "/generalLedgerClass")
    public String initGeneralLedgerClass1(ModelMap model, HttpServletRequest request) {
        String ledgerAccount = request.getParameter("selectedLabel");
        Map<String, String> codes = CobacCodes.cobacCodes();
        String redirectTo = "";
        if(codes.containsKey(ledgerAccount)){
            redirectTo = GENERAL_LEDGER_COBAC.concat(codes.get(ledgerAccount));
        }
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        if(CollectionUtils.isEmpty( ledgerAccountRepository.findByOrgId(user.getOrgId()))){
            //init ledgerAccounts
            generalLedgerService.getLedgerAccounts(user);
        }
        extracted(model, user.getOrgId());
        LedgerAccount ledgerAccount1 = (LedgerAccount) model.get("ledgerAccount");
        ledgerAccount1.setCategory(ledgerAccount);
        return redirectTo;
    }
    @PostMapping("/saveCobacGLAccount")
    public String saveCobacGLAccount(@ModelAttribute("ledgerAccount") LedgerAccount ledgerAccount,
                                     HttpServletRequest request,
                                   ModelMap model ){
        String category = ledgerAccount.getCategory();
        ledgerAccount.setCategory(extractCategory(category));
        return saveLedgerAccountForm(ledgerAccount, request, model);
    }
    private String extractCategory(String input){
        int startIndex = input.indexOf("(");
        int endIndex = input.indexOf(")");
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return input.substring(startIndex + 1, endIndex).trim();
        }
        return StringUtils.EMPTY;
    }

    private void extracted(ModelMap model, long orgId) {
//        List<LedgerAccount> byOrgId = ledgerAccountRepository.findByOrgId(orgId);
        Iterable<LedgerAccount> activeAccount = ledgerAccountRepository.findByOrgIdAndActiveTrue(orgId);
        Iterable<LedgerAccount> inActiveAccount = ledgerAccountRepository.findByOrgIdAndActiveFalse(orgId);

        model.put("activeAccount", activeAccount );
        model.put("ledgerAccountList", inActiveAccount );
        model.put("ledgerAccount",new LedgerAccount());
    }



    @GetMapping(value = "/editLedgerAccountStatus/{id}")
    public String editLedgerAccountStatus(@PathVariable("id") long id, ModelMap model) {
        LedgerAccount userById = ledgerAccountRepository.findById(id).get();
        if(userById.isActive()){
            userById.setActive(false);
            userById.setStatus(BVMicroUtils.INACTIVE);

        }else if (!userById.isActive()){
            userById.setActive(true);
            userById.setStatus(BVMicroUtils.ACTIVE);
        }
        ledgerAccountRepository.save(userById);
        List<AccountType> byOrgIdAndName = accountTypeRepository.findByOrgIdAndName(userById.getOrgId(), userById.getName());
        if(byOrgIdAndName != null && byOrgIdAndName.size() > 0){
            AccountType accountType = byOrgIdAndName.get(0);
            accountType.setActive(userById.isActive());
            accountTypeRepository.save(accountType);
        }
        extracted(model,userById.getOrgId());
        return "ledgerAccount";
    }


    @PostMapping(value = "/saveLedgerAccountForm")
    public String saveLedgerAccountForm(@ModelAttribute("ledgerAccount") LedgerAccount ledgerAccount, HttpServletRequest request,
                                        ModelMap model ) {
        User user = userRepository.findByUserName(getLoggedInUserName());

        if(null == ledgerAccount.getStatus()) ledgerAccount.setStatus(BVMicroUtils.INACTIVE);
        if(null == ledgerAccount.getInterAccountTransfer()) ledgerAccount.setInterAccountTransfer("false");
        if(null == ledgerAccount.getCashAccountTransfer()) ledgerAccount.setCashAccountTransfer("false");
        if(null == ledgerAccount.getCashTransaction()) ledgerAccount.setCashTransaction("false");
        if(null == ledgerAccount.getCreditBalance()) ledgerAccount.setCreditBalance("false");

        if(!ledgerAccount.getCode().contains("_GL_")){
            ledgerAccount.setCode(ledgerAccount.getName()+"_GL_"+ledgerAccount.getCode());
        }

        ledgerAccount.setOrgId(user.getOrgId());

        if(ledgerAccount.getId()>0){

            LedgerAccount aLedgerAccount = ledgerAccountService.extractedLedgerAccount(ledgerAccount, user.getOrgId());
            updateAccountTypeName(ledgerAccount.getDisplayName(), aLedgerAccount, user.getOrgId(), aLedgerAccount.getDisplayName());

            //Update DisplayName Too. TODO: Relationship in Domain Layer needed
            List<AccountType> byOrgIdAndName = accountTypeRepository.findByOrgIdAndName(aLedgerAccount.getOrgId(), aLedgerAccount.getName());
            if(byOrgIdAndName != null && byOrgIdAndName.size() > 0){
                AccountType byName = byOrgIdAndName.get(0);
                if(null != byName){
                    byName.setDisplayName(ledgerAccount.getDisplayName());
//                    byName.setCategory(ledgerAccount.getCategory());
//                    byName.setActive(ledgerAccount.isActive());
                    accountTypeRepository.save(byName);
                }
            }
            model.put("ledgerAccountInfo", "Created "+aLedgerAccount.getName()+ " successfully ");
        }else{
            ledgerAccountRepository.save(ledgerAccount);
            model.put("ledgerAccountInfo", "Created "+ledgerAccount.getName()+ " successfully ");
        }

        Iterable<LedgerAccount> activeAccount = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());
        Iterable<LedgerAccount> inActiveAccount = ledgerAccountRepository.findByOrgIdAndActiveFalse(user.getOrgId());

        model.put("activeAccount", activeAccount );
        model.put("ledgerAccountList", inActiveAccount );

        return "ledgerAccount";
    }

    private void updateAccountTypeName(String newDisplayName, LedgerAccount aLedgerAccount, long orgId, String oldDisplayName) {
        List<AccountType> byOrgIdAndName = accountTypeRepository.findByOrgIdAndName(orgId, aLedgerAccount.getName());
        if(byOrgIdAndName != null && byOrgIdAndName.size()>0){
            AccountType accountType = byOrgIdAndName.get(0);
            accountType.setDisplayName(newDisplayName);
            accountType.setActive(aLedgerAccount.isActive());
            accountTypeRepository.save(accountType);
            callCenterService.saveCallCenterLog("",getLoggedInUserName(),"","Renamed GL "+aLedgerAccount.getName() +"  "+oldDisplayName);
        }
    }


    @PostMapping(value = "/updateLedgerAccountForm")
    public String updateLedgerAccountForm(HttpServletRequest request, ModelMap model ) {
        String id = request.getParameter("aLedgerAccountId");
        String newName =  request.getParameter("accountName");
        LedgerAccount currentLedgerAccount = ledgerAccountRepository.findById(Long.parseLong(id)).get();
        currentLedgerAccount.setName(newName);

        ledgerAccountRepository.save(currentLedgerAccount);
        updateAccountTypeName(newName,currentLedgerAccount, currentLedgerAccount.getOrgId(), currentLedgerAccount.getName());
        //update AccountType too

        model.put("ledgerAccount",new LedgerAccount());

        Iterable<LedgerAccount> activeAccount = ledgerAccountRepository.findByOrgIdAndActiveTrue(currentLedgerAccount.getOrgId());
        Iterable<LedgerAccount> inActiveAccount = ledgerAccountRepository.findByOrgIdAndActiveFalse(currentLedgerAccount.getOrgId());

        model.put("activeAccount", activeAccount );
        model.put("ledgerAccountList", inActiveAccount );

        model.put("ledgerAccountInfo", "Created "+ newName + " successfully ");
        return "ledgerAccount";
    }

    @GetMapping(value = "/addGeneralLedgerEntry/{id}")
    public String addGeneralLedgerEntry(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {

        User user = userRepository.findByUserName(getLoggedInUserName());

        List<LedgerAccount> destinationLedgerAccount = ledgerAccountRepository.findAllExceptActive(id, user.getOrgId(), true);
        LedgerAccount originLedgerAccount = ledgerAccountRepository.findById(id).get();

        model.put("ledgerEntryDTO", new LedgerEntryDTO());
        model.put("destinationLedgerAccounts", destinationLedgerAccount);
        model.put("originLedgerAccount", originLedgerAccount);
        model.put("glAddEntryTitle", "Transfer Between GL Account");
        return "glAddEntry";
    }


    @GetMapping(value = "/deleteGL/{id}")
    public String deleteGL(@PathVariable("id") long id, ModelMap model) {
        LedgerAccount ledgerAccount = ledgerAccountRepository.findById(id).get();
        ledgerAccountRepository.delete(ledgerAccount);
        Iterable<LedgerAccount> all = ledgerAccountRepository.findByOrgIdAndActiveTrue(ledgerAccount.getOrgId());

        model.put("ledgerAccountInfo","Deleted " + ledgerAccount.getName() + " "+ledgerAccount.getCode());
        model.put("ledgerAccount",new LedgerAccount());
        model.put("ledgerAccountList", all);
        return "ledgerAccount";
    }


    @GetMapping(value = "/userHomeLedgerEntry")
    public String userHomeLedgerEntry( ModelMap model, HttpServletRequest request) {

        User user = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        Iterable<LedgerAccount> originLedgerAccounts = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());

        model.put("ledgerEntryDTO", new LedgerEntryDTO());
        model.put("originLedgerAccounts", originLedgerAccounts);
        return "glAddEntryToAccounts";
    }

    @GetMapping(value = "/glAddEntryFromCurrentAccount")
    public String glAddEntryFromCurrentAccount( ModelMap model, HttpServletRequest request) {

        User user = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        Iterable<LedgerAccount> originLedgerAccounts = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());

        model.put("ledgerEntryDTO", new LedgerEntryDTO());
        model.put("originLedgerAccounts", originLedgerAccounts);
        return "glAddEntryFromCurrentAccount";
    }

    @GetMapping(value = "/glAddEntryFromSavingAccount")
    public String glAddEntryFromSavingAccount( ModelMap model, HttpServletRequest request) {

        User user = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        Iterable<LedgerAccount> originLedgerAccounts = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());

        model.put("ledgerEntryDTO", new LedgerEntryDTO());
        model.put("originLedgerAccounts", originLedgerAccounts);
        return "glAddEntryFromSavingAccount";

    }


    @PostMapping(value = "/userHomeLedgerEntryFromAccountForm")
    public String userHomeLedgerEntryFromAccountForm( ModelMap model, HttpServletRequest request, @ModelAttribute("ledgerEntryDTO") LedgerEntryDTO ledgerEntryDTO) {
//        LedgerEntryDTO newLedgerEntryDTO = new LedgerEntryDTO();
        double fromTotal = 0.0;
        double toTotal = 0.0;
        Date recordDate = null;

        Enumeration<String> parameterNames = request.getParameterNames();
        while(parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            String paramValue = request.getParameter(parameterName);
            if(StringUtils.equals(parameterName, "recordDate")){
//                recordDate = BVMicroUtils.formatDate(paramValue);
                ledgerEntryDTO.setRecordDate(paramValue);
                continue;
            }
            if(parameterName.equals("ledgerAmount") || parameterName.equals("glAccountAmount") ){
                fromTotal = new Double(paramValue);
                continue;
            }
            if(parameterName.equals("originLedgerAccount") || parameterName.equals("notes") ){
                continue;
            }

            if(parameterName.equals("fromAccountToLedger") || parameterName.equals("_fromAccountToLedger")){
                continue;
            }
            if(StringUtils.isNotEmpty(paramValue)){
                toTotal = toTotal + Double.parseDouble(paramValue);
                String pair = parameterName+"_"+paramValue;
                ledgerEntryDTO.getParamValueString().add(pair);
            }
        }
        if(toTotal != fromTotal){

            User user = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
            Iterable<LedgerAccount> originLedgerAccounts = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());

            model.put("error", "Amounts entered do not add up");
            model.put("ledgerEntryDTO", ledgerEntryDTO);
            model.put("originLedgerAccounts", originLedgerAccounts);
            return "glAddEntryToAccounts";
        }else{
            String reference = BVMicroUtils.getSaltString();
            generalLedgerService.updateGLAfterLedgerAccountMultipleGLEntry(ledgerEntryDTO);
        }
        model.put("glAddEntryFromAccountsInfo", "TRANSFER WAS SUCCESSFULL");
        return glAddEntryFromCurrentAccount(model, request);
    }


    @PostMapping(value = "/userHomeLedgerEntryForm")
    public String userHomeLedgerEntryForm( ModelMap model, HttpServletRequest request, @ModelAttribute("ledgerEntryDTO") LedgerEntryDTO ledgerEntryDTO) {
//        LedgerEntryDTO newLedgerEntryDTO = new LedgerEntryDTO();
        double fromTotal = 0.0;
        double toTotal = 0.0;
        Date recordDate = null;
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        String reference = BVMicroUtils.getSaltString();
        Enumeration<String> parameterNames = request.getParameterNames();
        while(parameterNames.hasMoreElements()) {
                String parameterName = (String) parameterNames.nextElement();
                String paramValue = request.getParameter(parameterName);
                if(StringUtils.equals(parameterName, "recordDate")){
                    recordDate = BVMicroUtils.formatDate(paramValue);
                    ledgerEntryDTO.setRecordDate(paramValue);
                    continue;
                }
                if(parameterName.equals("ledgerAmount") || parameterName.equals("glAccountAmount") ){
                    fromTotal = new Double(paramValue);
                    continue;
                }
                if(parameterName.equals("originLedgerAccount") || parameterName.equals("notes")){
                    continue;
                }

                if(parameterName.equals("fromAccountToLedger") || parameterName.equals("_fromAccountToLedger")){
                    continue;
                }

                toTotal = toTotal + new Double(paramValue);
                String pair = parameterName+"_"+paramValue;

            ledgerEntryDTO.getParamValueString().add(pair);
            }
            if(toTotal != fromTotal){

                User user = (User)request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
                Iterable<LedgerAccount> originLedgerAccounts = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());
                model.put("error", "Amounts entered do not add up");
                model.put("ledgerEntryDTO", ledgerEntryDTO);
                model.put("originLedgerAccounts", originLedgerAccounts);
                return "glAddEntryToAccounts";
            }else{
                generalLedgerService.updateGLAfterLedgerAccountMultipleAccountEntry(ledgerEntryDTO, loggedInUser.getOrgId(), reference, runtimeSetting);
            }
        model.put("glAddEntryToAccountsInfo", "TRANSFER WAS SUCCESSFULL");
        return userHomeLedgerEntry(model, request);
    }


    @GetMapping(value = "/addGeneralLedgerEntryToAccounts/{id}")
    public String addGeneralLedgerEntryToAccounts(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {

        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        final User aUser = userRepository.findById(user.getId()).get();
        final List<SavingAccount> savingAccounts = aUser.getSavingAccount();

        LedgerAccount originLedgerAccount = ledgerAccountRepository.findById(id).get();
        LedgerEntryDTO ledgerEntryDTO = new LedgerEntryDTO();
        ledgerEntryDTO.setSavingAccounts(savingAccounts);

        model.put("ledgerEntryDTO",ledgerEntryDTO );
        model.put("originLedgerAccount", originLedgerAccount);
        model.put("glAddEntryTitle", "Transfer from "+originLedgerAccount.getName()+" Account");
        return "glAddEntryToAccounts";
    }

    @GetMapping(value = "/addGeneralLedgerEntryFromAccounts/{id}")
    public String addGeneralLedgerEntryFromAccounts(@PathVariable("id") long id, ModelMap model) {

        LedgerAccount originLedgerAccount = ledgerAccountRepository.findById(id).get();

        model.put("ledgerEntryDTO", new LedgerEntryDTO());
        model.put("destinationLedgerAccount", originLedgerAccount);
        model.put("glAddEntryTitle", "Transfer from "+originLedgerAccount.getName()+" Account");
        return "glAddEntryFromAccounts";
    }


    @GetMapping(value = "/cashToLedgerAccount/{id}")
    public String cashToLedgerAccount(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        LedgerAccount fromLedgerAccount = ledgerAccountRepository.findById(id).get();
        LedgerAccount cashAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CASH, fromLedgerAccount.getOrgId());

        LedgerEntryDTO ledgerEntryDTO = new LedgerEntryDTO();
        ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.CREDIT);
        model.put("ledgerEntryDTO",ledgerEntryDTO );
        model.put("destinationLedgerAccounts", fromLedgerAccount);
        model.put("originLedgerAccount", cashAccount);
        model.put("creditOrDebit","CREDIT");
        model.put("glAddEntryTitle", " "+ BVMicroUtils.CASH_GL_5001  + " --> " +  fromLedgerAccount.getCode());
        return "glAddEntry";
    }

    @GetMapping(value = "/cashFromLedgerAccount/{id}")
    public String cashFromLedgerAccount(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        LedgerAccount toLedgerAccount = ledgerAccountRepository.findById(id).get();
        LedgerAccount cashAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CASH_GL_5001,toLedgerAccount.getOrgId());
        if(null==cashAccount){
            cashAccount = ledgerAccountRepository.findByCodeAndOrgIdAndActiveTrue(BVMicroUtils.CASH_GL_5001, toLedgerAccount.getOrgId() );
        }
        LedgerEntryDTO ledgerEntryDTO = new LedgerEntryDTO();
        ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.DEBIT);
        model.put("ledgerEntryDTO",ledgerEntryDTO );
        model.put("destinationLedgerAccounts", toLedgerAccount);
        model.put("originLedgerAccount", cashAccount);
        model.put("creditOrDebit","DEBIT");
        model.put("glAddEntryTitle", " "+  toLedgerAccount.getCode() + " --> "+ BVMicroUtils.CASH_GL_5001);
        return "glAddEntry";
    }

    @GetMapping(value = "/cashFromLedgerAccountToAccounts/{id}")
    public String cashFromLedgerAccountToAccounts(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        LedgerAccount toLedgerAccount = ledgerAccountRepository.findById(id).get();
        LedgerAccount cashAccount = ledgerAccountRepository.findByNameAndOrgIdAndActiveTrue(BVMicroUtils.CASH_GL_5001, toLedgerAccount.getOrgId());
        LedgerEntryDTO ledgerEntryDTO = new LedgerEntryDTO();
        ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.DEBIT);
        model.put("ledgerEntryDTO",ledgerEntryDTO );
        model.put("destinationLedgerAccounts", toLedgerAccount);
        model.put("originLedgerAccount", cashAccount);
        model.put("glAddEntryTitle", " "+ BVMicroUtils.CASH_GL_5001 + " --> "+ toLedgerAccount.getCode());
        return "glAddEntry";
    }


    @PostMapping(value = "/addLedgerEntryFormReviewForm")
    public String addLedgerEntryFormReviewForm(@ModelAttribute("ledgerEntryDTO") LedgerEntryDTO ledgerEntryDTO,
                                        ModelMap model, HttpServletRequest request ) {
        LedgerAccount toAccount = ledgerAccountRepository.findById(ledgerEntryDTO.getDestinationLedgerAccount()).get();
        LedgerAccount fromAccount = ledgerAccountRepository.findById(ledgerEntryDTO.getOriginLedgerAccount()).get();
//        String creditOrDebit = request.getParameter("creditOrDebit");
        User user = userRepository.findByUserName(getLoggedInUserName());
        ledgerEntryDTO.setOrgId(user.getOrgId());

        model.put("ledgerAccount", ledgerEntryDTO);
        model.put("destinationLedgerAccounts", toAccount );
        model.put("originLedgerAccount", fromAccount );
        model.put("glAddEntryTitle", " "+ fromAccount.getName() + " To "+ toAccount.getName());

        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        if(!checkBillSelectionMatchesEnteredAmount(ledgerEntryDTO) && "true".equals(runtimeSetting.getBillSelectionEnabled()) )
        {
            model.put("error", "Bill Selection Does Not Match" );
            return "glAddEntry";
        }

        if( StringUtils.isEmpty( ledgerEntryDTO.getCreditOrDebit() )){
            model.put("error", "PLEASE SELECT A TRANSACTION TYPE" );
            return "glAddEntry";
        }
        generalLedgerService.updateManualAccountTransaction(ledgerEntryDTO, false, runtimeSetting.getCountryCode(), user.getBranch().getCode());
        model.put("ledgerConfirmInfo", "Successfully Registered" );
        return "ledgerConfirm";
    }


    private boolean checkBillSelectionMatchesEnteredAmount(LedgerEntryDTO sat) {

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

        if(sat.getLedgerAmount() == selection)
        return true;
        return false;
    }

    @GetMapping(value = "/updateAccountLedger/{id}")
    public String updateAccountLedger(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {

        LedgerAccount byId = ledgerAccountRepository.findById(id).get();
        Iterable<LedgerAccount> all = ledgerAccountRepository.findByOrgId(byId.getOrgId());

        model.put("ledgerAccount", byId);
        model.put("ledgerAccountList", all);

        return "ledgerAccountUpdate";
    }

    @GetMapping(value = "/bulkLedgerAccount/{id}")
    public String bulkLedgerAccount(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {

        User aUser = userRepository.findByUserName(getLoggedInUserName());
        LedgerAccount ledgerAccount = ledgerAccountRepository.findById(id).get();
        request.getSession().setAttribute("bulkLedgerSelected", ledgerAccount);

            CustomersDTO customersDTO = userService.extractCustomers(aUser);
            model.put("customersDTO", customersDTO);

        return "customers";
    }


    @GetMapping(value = "/updateAccountLedger")
    public String updateAccountLedgers( ModelMap model, HttpServletRequest request) {

        User user = userRepository.findByUserName(getLoggedInUserName());
        Iterable<LedgerAccount> all = ledgerAccountRepository.findByOrgId(user.getOrgId());
        for (LedgerAccount ledgerAccount: all) {
            ledgerAccount.setActive(true);
            ledgerAccountRepository.save(ledgerAccount);
        }
        model.put("ledgerAccount",new LedgerAccount());

        model.put("ledgerAccountList", all);
        return "ledgerAccount";
    }

}
