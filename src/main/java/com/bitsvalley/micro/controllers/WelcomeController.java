package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.AccountType;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class WelcomeController extends SuperController {

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    SavingAccountService savingAccountService;

    @Autowired
    DailySavingAccountService dailySavingAccountService;

    @Autowired
    LoanAccountService loanAccountService;

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    ShareAccountService shareAccountService;

    @Autowired
    UserRepository userRepository;

    @GetMapping(value = "/")
    public String showIndexPage(ModelMap model, HttpServletRequest request) {
        String name = getLoggedInUserName();
        if(request.getSession().getAttribute("runtimeSettings") != null && null != name){ // TODO: Why is a session still active here
            model.put("name", name);
            return "welcome";
        }
        return "login";
    }

    @GetMapping(value = "/login/{bid}")
    public String showBranchAccountTypes(@PathVariable("bid") String bid, ModelMap model) {
        model.put("bid", bid+".png");
        return "login";
    }

    @GetMapping(value = "/tbc")
    public String logintbc(ModelMap model, HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "logintbc";
    }

    @GetMapping(value = "/mgv")
    public String loginmgv(HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "loginmgv";
    }

    @GetMapping(value = "/unics")
    public String loginunics(HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "unics";
    }


    @GetMapping(value = "/azi")
    public String loginazi(ModelMap model, HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "loginazi";
    }

    @GetMapping(value = "/termcondition")
    public String termcondition(ModelMap model, HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "termcondition";
    }

    @GetMapping(value = "/privacypolicy")
    public String privacypolicy(ModelMap model, HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "privacypolicy";
    }

    @GetMapping(value = "/evolution")
    public String loginevo(ModelMap model, HttpServletRequest request) {
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        return "loginevo";
    }

    @GetMapping(value = "/login")
    public String login(ModelMap model, HttpServletRequest request) {
        RuntimeSetting referrer = (RuntimeSetting)request.getSession().getAttribute("runtimeSettings");
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }
        if(referrer != null)
            return "logintbc";
        return "login";
    }


//    @GetMapping(value = "/verify/{bid}")
    public String verifu(ModelMap model, HttpServletRequest request) {
        String output = BVMicroUtils.get_SHA_512_SecurePassword("qqqqqq");
        User manager101 = userRepository.findByUserName("manager101");
        return "true";
    }

    @GetMapping(value = "/welcome")
    public String welcome(ModelMap model, HttpServletRequest request) {


        String referer = request.getHeader("referer");
        if(referer == null){
            referer = "login";
        }
        referer = referer.substring(referer.lastIndexOf("/")+1,referer.length());
        request.getSession().setAttribute("referer", referer);

        final String loggedInUserName = getLoggedInUserName();
        request.getSession().setAttribute("name", loggedInUserName);

        User customerInUse = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        User loggedInCustomer = userRepository.findByUserName(loggedInUserName);

        if (null == loggedInCustomer) {
            initSystemService.initSystem(0);
            request.getSession().setAttribute("runtimeSettings", initSystemService.findByOrgId(0));
            request.getSession().setAttribute(BVMicroUtils.CURRENT_ORG, 0);
        } else {
            RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
            runtimeSetting = initSystemService.findByOrgId(loggedInCustomer.getOrgId());
            request.getSession().setAttribute("runtimeSettings", runtimeSetting);
            request.getSession().setAttribute(BVMicroUtils.CURRENT_ORG, loggedInCustomer.getOrgId());
            request.getSession().setAttribute("userFirstName",loggedInCustomer.getFirstName());
            request.getSession().setAttribute("userBranch",loggedInCustomer.getBranch().getName());

        }

        //ONLINE BANKING FLOW
        if (loggedInCustomer != null && loggedInCustomer.getUserRole().size() == 1 && StringUtils.equals(loggedInCustomer.getUserRole().get(0).getName(), BVMicroUtils.ROLE_CUSTOMER)) {
            customerInUse = loggedInCustomer;
            request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, customerInUse);
        }

        if (null != customerInUse) {
            RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
            request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, customerInUse);
            SavingBilanzList savingBilanzByUserList = savingAccountService.getSavingBilanzByUser(loggedInCustomer, false, runtimeSetting.getCountryCode());
            SavingBilanzList dailySavingBilanzByUserList = dailySavingAccountService.getSavingBilanzByUser(loggedInCustomer, false, runtimeSetting.getCountryCode());
            LoanBilanzList loanBilanzByUserList = loanAccountService.getLoanBilanzByUser(loggedInCustomer, true, runtimeSetting.getCountryCode());
            CurrentBilanzList currentBilanzByUserList = currentAccountService.getCurrentBilanzByUser(loggedInCustomer, false, runtimeSetting);
            ShareAccountBilanzList shareAccountBilanzList = shareAccountService.getShareAccountBilanzByUser(loggedInCustomer,runtimeSetting.getCountryCode());

//            Collections.reverse(savingBilanzByUserList.getSavingBilanzList()); //TODO: reverse during search?
//            Collections.reverse(dailySavingBilanzByUserList.getSavingBilanzList()); //TODO: reverse during search?

            request.getSession().setAttribute("savingBilanzList", savingBilanzByUserList);
            request.getSession().setAttribute("dailySavingBilanzList", dailySavingBilanzByUserList);
            request.getSession().setAttribute("loanBilanzList", loanBilanzByUserList);
            request.getSession().setAttribute("currentBilanzList", currentBilanzByUserList);
            request.getSession().setAttribute("shareAccountBilanzList", shareAccountBilanzList);
            request.getSession().setAttribute("telephone1", customerInUse.getTelephone1());
            return homeType(loggedInCustomer, customerInUse);
        }

        return "welcome";
    }



    @GetMapping(value = "/welcomeGlobal")
    public String welcomeGlobal(ModelMap model, HttpServletRequest request) {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        if (authorities.size() == 1 && StringUtils.equals(authorities.iterator().next().toString(), BVMicroUtils.ROLE_CUSTOMER)) {
            return welcome(model, request);
        } else {
            if( request.getSession(false) == null ){
                return "login";
            }
            String loggedInUserName = getLoggedInUserName();
            User loggedInCustomer = userRepository.findByUserName(loggedInUserName);
            request.getSession().setAttribute("userFirstName",loggedInCustomer.getFirstName());
            request.getSession().setAttribute(BVMicroUtils.CUSTOMER_IN_USE, null);
            return "welcome";
        }
    }

    @GetMapping(value = "/searchCustomer")
    public String searchCustomer(ModelMap model, HttpServletRequest request) {
        model.put("name", getLoggedInUserName());
        return "welcome";
    }

    @GetMapping(value = "/getImage")
    @ResponseBody
    public byte[] getImage(HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user != null && user.getIdFilePath() != null) {
            Path path = Paths.get(user.getIdFilePath());
            if (StringUtils.isNotEmpty(path.toString())) {
                byte[] data = Files.readAllBytes(path);
                return data;
            }
        }
        return null;
    }

    @GetMapping(value = "/getImage4")
    @ResponseBody
    public byte[] getImage4(HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user != null) {
            Path path = Paths.get(user.getIdFilePath4());
            if (StringUtils.isNotEmpty(path.toString())) {
                byte[] data = Files.readAllBytes(path);
                return data;
            }
        }
        return null;
    }

    @GetMapping(value = "/getImage2")
    @ResponseBody
    public byte[] getImage2(HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user != null) {
            Path path = Paths.get(user.getIdFilePath2());
            if (StringUtils.isNotEmpty(path.toString())) {
                byte[] data = Files.readAllBytes(path);
                return data;
            }
        }
        return null;
    }

    @GetMapping(value = "/getImage3")
    @ResponseBody
    public byte[] getImage3(HttpServletRequest request) throws IOException {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        if (user != null) {
            Path path = Paths.get(user.getIdFilePath3());
            if (StringUtils.isNotEmpty(path.toString())) {
                byte[] data = Files.readAllBytes(path);
                return data;
            }
        }
        return null;
    }

    @GetMapping(value = "/getLogoImage")
    @ResponseBody
    public byte[] getLogoImage(HttpServletRequest request) throws IOException {
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        if(runtimeSetting == null) return null;

        Path path = Paths.get(runtimeSetting.getLogo());
        if (StringUtils.isNotEmpty(path.toString())) {
            byte[] data = Files.readAllBytes(path);
            return data;
        }
        return null;
    }

    @GetMapping(value = "/getLogoImageBid")
    @ResponseBody
    public byte[] getLogoImageBid(HttpServletRequest request, ModelMap model) throws IOException {
        RuntimeSetting runtimeSetting = (RuntimeSetting) model.getAttribute("businessInfo");
        Path path = Paths.get(runtimeSetting.getLogo());
        if (StringUtils.isNotEmpty(path.toString())) {
            byte[] data = Files.readAllBytes(path);
            return data;
        }
        return null;
    }

    @GetMapping(value = "/getUnionLogoImage")
    @ResponseBody
    public byte[] getUnionLogoImage(HttpServletRequest request) throws IOException {
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        Path path = Paths.get(runtimeSetting.getUnionLogo());
        byte[] data = Files.readAllBytes(path);
        return data;
    }


//    @CrossOrigin()
//    @PostMapping("/landing")
//    public String logUserOut(@RequestBody User user, HttpServletRequest request) {
//
//        User byUserName = userRepository.findByUserName(user.getUserName());
//        if (byUserName.getPassword().equals(user.getPassword())) {
//            return "SUCCESS";
//        }
//        return "FAILURE";
//    }

}
