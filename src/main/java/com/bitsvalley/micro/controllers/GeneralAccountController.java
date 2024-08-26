package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.services.GeneralLedgerService;
import com.bitsvalley.micro.services.PdfService;
import com.bitsvalley.micro.services.UserRoleService;
import com.bitsvalley.micro.services.UserService;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class GeneralAccountController extends SuperController {

    @Autowired
    CallCenterRepository callCenterRepository;

    @Autowired
    GeneralLedgerService generalLedgerService;

    @Autowired
    GeneralLedgerRepository generalLedgerRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    PdfService pdfService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    UserService userService;

    /*
    @GetMapping(value = "/cleangl")
    public String cleangl(ModelMap model, HttpServletRequest request) {
        int counterNo = 0;
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());
        Iterable<GeneralLedger> all = generalLedgerRepository.findByOrgId(user.getOrgId());
        for (GeneralLedger aGeneralLedger: all) {
            if(StringUtils.equals(aGeneralLedger.getType(), BVMicroUtils.DEBIT) && aGeneralLedger.getAmount() > 0){
                aGeneralLedger.setAmount(aGeneralLedger.getAmount()*-1);
                generalLedgerRepository.save(aGeneralLedger);
                counterNo++;
            } else if(StringUtils.equals(aGeneralLedger.getType(), BVMicroUtils.CREDIT) && aGeneralLedger.getAmount() < 0){
                aGeneralLedger.setAmount(aGeneralLedger.getAmount()*-1);
                generalLedgerRepository.save(aGeneralLedger);
                counterNo++;
            }
        }
        if(counterNo > 0){
            model.put("counterNo", counterNo + "records updated");
        }

        return showAllGL(model,request);
    }
    */


    @GetMapping(value = "/billSelection")
    public String billSelection(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        GLSearchDTO glSearchDTO = new GLSearchDTO();

        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);

        ArrayList<String> allGlEntryUserNames = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGlEntryUserNames);
        model.put("allGLEntryUsers", allGlEntryUserNames);

        model.put("billSelectionBilanz", new BillSelectionBilanz());
        model.put("showBillSelectionTable", "true");
        model.put("glSearchDTO", glSearchDTO);
        return "billSelection";
    }

    @PostMapping(value = "/filterBillSelection")
    public String filterBillSelection(ModelMap model, HttpServletRequest request,
                                      @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO) {

        glSearchDTO.setStartDate(glSearchDTO.getStartDate() + " 00:00:00.000");
        glSearchDTO.setEndDate(glSearchDTO.getEndDate() + " 23:59:59.999");

        User user = userRepository.findByUserName(getLoggedInUserName());

        BillSelectionBilanz billSelectionBilanz = generalLedgerService.searchCriteriaBillSelection(glSearchDTO.getStartDate(), glSearchDTO.getEndDate(), glSearchDTO.getAllGLEntryUsers().get(0), user.getOrgId());

        model.put("billSelectionBilanz", billSelectionBilanz);
        model.put("showBillSelectionTable", "false");
        model.put("headerText", glSearchDTO.getStartDate().substring(0, 16) + " - " + glSearchDTO.getEndDate().substring(0, 16));
        ArrayList<String> allGlEntryUserNames = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGlEntryUserNames);
        model.put("allGLEntryUsers", allGlEntryUserNames);
        model.put("glSearchDTO", glSearchDTO);

        return "billSelection";
    }

    @PostMapping(value = "/filterGenaralLedger")
    public String filterGenaralLedger(ModelMap model, HttpServletRequest request,
                                  @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO) {

        GeneralLedgerBilanz generalLedgerBilanz = null;
        User user = userRepository.findByUserName(getLoggedInUserName());
        UserRole role_all_branch_gl = userRoleService.findUserRoleByName(BVMicroUtils.ROLE_ALL_BRANCH_GL, 0);

        if (user.getUserRole().contains(role_all_branch_gl)) {
            generalLedgerBilanz =
                    generalLedgerService.searchCriteria(glSearchDTO.getStartDate() + " 00:00:00.000", glSearchDTO.getEndDate() + " 23:59:59.999",
                            glSearchDTO.getAllGLEntryUsers().get(0), glSearchDTO.getAllLedgerAccount().get(0), user.getOrgId());
        }else{
            generalLedgerBilanz =
                    generalLedgerService.searchCriteria(glSearchDTO.getStartDate() + " 00:00:00.000", glSearchDTO.getEndDate() + " 23:59:59.999",
                            glSearchDTO.getAllGLEntryUsers().get(0), glSearchDTO.getAllLedgerAccount().get(0), user.getOrgId(), user.getBranch().getCode() );
        }

        model.put("generalLedgerBilanz", generalLedgerBilanz);

        ArrayList<String> allGlEntryUserNames = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGlEntryUserNames);
        model.put("accountNameHeader", "GENERAL LEDGER TRANSACTIONS");
        model.put("allGLEntryUsers", allGlEntryUserNames);
        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
        model.put("glSearchDTO", glSearchDTO);
        GeneralLedgerWeb posGl3333 = generalLedgerBilanz.getGeneralLedgerWeb()
          .stream()
          .filter(Objects::nonNull)
          .filter(item -> item.getLedgerAccount().getCode().equals("POS_GL_3333"))
          .findFirst().orElse(new GeneralLedgerWeb());
        model.put("setPdfLink",posGl3333.getLedgerAccount() == null
          ? null : posGl3333.getLedgerAccount().getId());
        return "gls";
    }

    @GetMapping(value = "/gl/reference/{reference}")
    public String showGlReference(@PathVariable("reference") String reference, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());

        if(reference.indexOf("_") != -1){
            reference = reference.substring(0,reference.indexOf("_"));
        }
        GeneralLedgerBilanz glList = generalLedgerService.findByReference(reference,user.getOrgId());
        String accountsInvolved = "";
        for (GeneralLedgerWeb generalLedgerWeb : glList.getGeneralLedgerWeb()) {
            if (generalLedgerWeb.getLedgerAccount() != null) {
                accountsInvolved = accountsInvolved + generalLedgerWeb.getLedgerAccount().getName() + ", ";
            }
        }
        model.put("accountNameHeader", accountsInvolved.substring(0, accountsInvolved.length() - 2));
        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
        model.put("generalLedgerBilanz", glList);
        model.put("glSearchDTO", new GLSearchDTO());
        return "gls";
    }


    @GetMapping(value = "/ref/{reference}")
    public String deleteReference(@PathVariable("reference") String reference, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());

        if(reference.indexOf("_") != -1){
            reference = reference.substring(0,reference.indexOf("_"));
        }
        List<GeneralLedger> glList = generalLedgerRepository.findByReferenceAndOrgId(reference, user.getOrgId());
        String accountsInvolved = "";
        for (GeneralLedger generalLedger : glList) {
            generalLedger.setLedgerAccount(null);
            generalLedgerRepository.save(generalLedger);
            generalLedgerRepository.delete(generalLedger);
//            callCenterRepository.
        }
        generalLedgerService.deleteTransactions( reference, user.getOrgId() );

        return showAllGL(model,request);
    }


    @GetMapping(value = "/createTrialBalanzReport")
    public void createTrialBalanzReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TrialBalanceBilanz trialBalanceBilanz = (TrialBalanceBilanz)request.getSession().getAttribute("trialBalanceBilanz");
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        String htmlInput = pdfService.generateTrialBalance(trialBalanceBilanz, runtimeSetting);

        response.setHeader("Content-disposition", "attachment;filename=" + "Balance_Report.pdf");
        generateByteOutputStream(response, htmlInput);

    }


    @GetMapping(value = "/trialBalance")
    public String trialBalance(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime localDateStart = getFirstDayOfMonth(now);

        TrialBalanceBilanz trialBalanceBilanz = generalLedgerService.getCurrentTrialBalance(localDateStart, now, user.getOrgId());

        GLSearchDTO glSearchDTO = new GLSearchDTO();

        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);

        model.put("accountNameHeader", "TRIAL BALANCE");
        model.put("trialBalanceBilanz", trialBalanceBilanz);
        model.put("glSearchDTO", glSearchDTO);
        model.put("startDate", BVMicroUtils.formatDateTime(localDateStart));
        model.put("endDate", BVMicroUtils.formatDateTime(now));

        return "trialBalance";
    }



    @PostMapping(value = "/filterTrialBalance")
    public String filterTrialBalance(ModelMap model, HttpServletRequest request,
                                     @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO) {

        String startDate = glSearchDTO.getStartDate();
        String endDate = glSearchDTO.getEndDate()+ " 23:59:59.999";

        User user = userRepository.findByUserName(getLoggedInUserName());

        TrialBalanceBilanz trialBalanceBilanz = generalLedgerService.getTrialBalanceWebs(startDate, endDate, user.getOrgId() );
        trialBalanceBilanz.setStartDate(startDate);
        trialBalanceBilanz.setEndDate(endDate.substring(0,endDate.indexOf(" 23:59:59.999")));

        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);

        model.put("accountNameHeader", "TRIAL BALANCE");
        model.put("trialBalanceBilanz", trialBalanceBilanz);

        model.put("glSearchDTO", glSearchDTO);
        model.put("startDate", startDate);
        model.put("endDate", endDate);

        request.getSession().setAttribute("trialBalanceBilanz",trialBalanceBilanz);
        return "trialBalance";

    }

//    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
//    @RolesAllowed("ROLE_CUSTOMER")
    @GetMapping(value = "/gl")
    public String showAllGL(ModelMap model, HttpServletRequest request) {
        GLSearchDTO glSearchDTO = new GLSearchDTO();

        Calendar instance = GregorianCalendar.getInstance();
        String day = instance.get(GregorianCalendar.YEAR) + "-" +(instance.get(GregorianCalendar.MONTH)+1) +"-"+ instance.get(GregorianCalendar.DAY_OF_MONTH);

        glSearchDTO.setStartDate(day);
        glSearchDTO.setEndDate(day);
        List<String> users = new ArrayList<String>();
        users.add("-1");
        glSearchDTO.setAllGLEntryUsers(users);

        List<Integer> employees = new ArrayList<Integer>();
        employees.add(-1);
        glSearchDTO.setAllLedgerAccount(employees);
        return filterGenaralLedger(model,request,glSearchDTO);
//        User user = userRepository.findByUserName(getLoggedInUserName());
//        model.put("accountNameHeader", "GENERAL LEDGER TRANSACTIONS");
//        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
//
//        GeneralLedgerBilanz generalLedgerBilanz1 = new GeneralLedgerBilanz();
//        generalLedgerBilanz1.setCreditTotal(0);
//        generalLedgerBilanz1.setDebitTotal(0);
//        generalLedgerBilanz1.setTotal(0);
//
//        model.put("generalLedgerBilanz", generalLedgerBilanz1);
//
////        GLSearchDTO glSearchDTO = new GLSearchDTO();
//        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
//        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);
//        model.put("allGLEntryUsers", allGLEntryUsers);
//        model.put("glSearchDTO", glSearchDTO);
//        return "gls";
    }


    @GetMapping(value = "/viewLedgerAccount/{id}")
    public String ledgerAccount(@PathVariable("id") long id, ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        Iterable<LedgerAccount> all = ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId());
        GeneralLedgerBilanz generalLedgerBilanz = generalLedgerService.findGLByLedgerAccount(id);
        model.put("allLedgerAccount", all);
        model.put("generalLedgerBilanz", generalLedgerBilanz);
        model.put("glSearchDTO", new GLSearchDTO());
        return "gls";
    }


    @GetMapping(value = "/findGlByType/{type}")
    public String findByGlType(@PathVariable("type") String type, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        GeneralLedgerBilanz generalLedgerBilanz = generalLedgerService.findGLByType(type, user.getOrgId());
        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
        model.put("generalLedgerBilanz", generalLedgerBilanz);
        model.put("glSearchDTO", new GLSearchDTO());
        return "gls";
    }

    @GetMapping(value = "/updateDisplayName")
    public String fixDisplayName() {
        User user = userRepository.findByUserName(getLoggedInUserName());
        Iterable<AccountType> all = accountTypeRepository.findByOrgId(user.getOrgId());
        for (AccountType aAccountType : all) {
            if (StringUtils.isEmpty(aAccountType.getDisplayName())) {
                aAccountType.setDisplayName(aAccountType.getName());
                accountTypeRepository.save(aAccountType);
            }
        }
        for (LedgerAccount aLedgerAccount : ledgerAccountRepository.findByOrgId(user.getOrgId())) {
            if (StringUtils.isEmpty(aLedgerAccount.getDisplayName())) {
                aLedgerAccount.setDisplayName(aLedgerAccount.getName());
                ledgerAccountRepository.save(aLedgerAccount);
            }
        }
        return "welcome";
    }



    @PostMapping(value = "/updateGLNotes")
    public String updateGLNotes(HttpServletRequest request, ModelMap model ) {
        String id = request.getParameter("generalLedgerId");
        String newNotes =  request.getParameter("glNotes");
        GeneralLedger generalLedger = generalLedgerRepository.findById(new Long(id)).get();
        generalLedger.setNotes(newNotes);
        generalLedgerRepository.save(generalLedger);
        model.put("glLedgerInfo", "Notes updated successfully ");
        return showAllGL(model, request);
    }

}
