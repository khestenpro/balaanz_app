package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.CallCenter;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.CallCenterRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.GLSearchDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
public class CallCenterController extends SuperController{

    @Autowired
    CallCenterRepository callCenterRepository;

    @Autowired
    UserRepository userRepository;


    @Secured({"ROLE_MAIN_CALL_CENTER"})
    @GetMapping(value = "/callcenter/{accountNumber}")
    public String showCustomer(@PathVariable("accountNumber") String accountNumber, ModelMap model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        List<CallCenter> callCenterList = callCenterRepository.findByAccountNumberAndOrgId(accountNumber, user.getOrgId());
        GLSearchDTO glSearchDTO = getGlSearchDTO(user);
        model.put("allGLEntryUsers", glSearchDTO.getAllGLEntryUsers());
        model.put("glSearchDTO", glSearchDTO);

        if( callCenterList != null && callCenterList.size() > 0){
            Collections.reverse(callCenterList);
            model.put("callCenterList", callCenterList);

            String aAccountNumber = callCenterList.get(0).getAccountNumber();
            String accountHolderName = callCenterList.get(0).getUserName();
            model.put("accountNumber",aAccountNumber );
            model.put("accountHolderName",accountHolderName );
        }
        return "callCenter";
    }


    @GetMapping(value = "/callcenter")
    public String callcenter( ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        GLSearchDTO glSearchDTO = new GLSearchDTO();
        Calendar instance = GregorianCalendar.getInstance();
        String day = instance.get(GregorianCalendar.YEAR) + "-" +(instance.get(GregorianCalendar.MONTH)+1) +"-"+ instance.get(GregorianCalendar.DAY_OF_MONTH);
        glSearchDTO.setStartDate(day);
        glSearchDTO.setEndDate(day);
        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);
        return filterCallCenter(model, glSearchDTO);
    }


    @PostMapping(value = "/filterCallCenter")
    public String filterCallCenter( ModelMap model,
                                    @ModelAttribute("glSearchDTO") GLSearchDTO glSearchDTO) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        glSearchDTO.setStartDate(glSearchDTO.getStartDate() + " 00:00:00.000");
        glSearchDTO.setEndDate(glSearchDTO.getEndDate() + " 23:59:59.999");
        model.put("allGLEntryUsers", glSearchDTO.getAllGLEntryUsers());
        model.put("glSearchDTO", glSearchDTO);

        List<CallCenter> callCenterList = callCenterRepository.findByCreatedDateBetween(glSearchDTO.getStartDate(),glSearchDTO.getEndDate(), user.getOrgId() );

        if( callCenterList != null && callCenterList.size() > 0){
            Collections.reverse(callCenterList);
            model.put("callCenterList", callCenterList);

            model.put("accountNumber","00000000000000000000000" );
            model.put("accountHolderName","-" );
        }
        return "callCenter";
    }

    @NotNull
    private GLSearchDTO getGlSearchDTO(User user) {
        GLSearchDTO glSearchDTO = new GLSearchDTO();
        glSearchDTO.setStartDate(glSearchDTO.getStartDate() + " 00:00:00.000");
        glSearchDTO.setEndDate(glSearchDTO.getEndDate() + " 23:59:59.999");

        ArrayList<String> allGLEntryUsers = getAllNonCustomers(user.getOrgId());
        glSearchDTO.setAllGLEntryUsers(allGLEntryUsers);
        return glSearchDTO;
    }

}
