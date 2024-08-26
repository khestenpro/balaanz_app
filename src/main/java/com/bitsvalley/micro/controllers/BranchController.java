package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.BranchRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
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
public class BranchController extends SuperController {

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    UserRepository userRepository;

    @PostMapping(value = "/registerBranchForm")
    public String registerSavingForm(@ModelAttribute("branch") Branch branch, ModelMap model) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        if (user == null) {
            branch.setOrgId(0);
        } else {
            branch.setOrgId(user.getOrgId());
        }
        if (branchRepository.findByCodeAndOrgId(branch.getCode(), branch.getOrgId()) == null) {
            branchRepository.save(branch);
            model.put("branch", branch);
            model.put("branchInfo", branch.getName() + " - New Branch Created");
        } else {
            model.put("branchError", branch.getName() + " - Branch exists code " + branch.getCode());
        }
        return "branch";
    }


    @GetMapping(value = "/registerBranch")
    public String registerBranch(ModelMap model, HttpServletRequest request) {
        Branch branch = new Branch();
        model.put("branch", branch);
        return "branch";
    }


    @GetMapping(value = "/branches")
    public String branches(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());

        Iterable<Branch> branches = branchRepository.findByOrgId(user.getOrgId());

        Iterator<Branch> iterator = branches.iterator();
        model.put("branchList", iterator);
        return "branches";
    }

    @GetMapping(value = "/branch/{id}")
    public String showBranchCustomers(@PathVariable("id") Long id, ModelMap model, HttpServletRequest request) {
        Optional<Branch> branch = branchRepository.findById(id);
        ArrayList<User> branchUserList = new ArrayList<User>();
        ArrayList<User> otherUserList = new ArrayList<User>();
        ArrayList<User> noBranchUserList = new ArrayList<User>();
        ArrayList<User> allUsers = userRepository.findByOrgId(branch.get().getOrgId());
        for (User aUser : allUsers) {
            if (aUser.getBranch() != null && aUser.getBranch().getId() == branch.get().getId()) {
                branchUserList.add(aUser);
            } else if (null == aUser.getBranch()) {
                noBranchUserList.add(aUser);
            } else {
                otherUserList.add(aUser);
            }
        }
        model.put("otherUserList", otherUserList);
        model.put("branchUserList", branchUserList);
        model.put("noBranchUserList", noBranchUserList);

        model.put("branch", branch.get());
        return "branchEmployees";
    }


    @GetMapping(value = "/addCustomerToBranch/{userId}/{branchId}")
    public String showCustomer(@PathVariable("userId") Long userId, @PathVariable("branchId") Long branchId, ModelMap model, HttpServletRequest request) {

        User user = userRepository.findById(userId).get();
        user.setBranch(branchRepository.findById(branchId).get());
        userRepository.save(user);
        return showBranchCustomers(branchId, model, request);
    }

    @GetMapping(value = "/removeCustomerFromBranch/{userId}/{branchId}")
    public String removeCustomer(@PathVariable("userId") Long userId, @PathVariable("branchId") Long branchId,
                                 ModelMap model, HttpServletRequest request) {

        User user = userRepository.findById(userId).get();
        user.setBranch(null);
        userRepository.save(user);
        return showBranchCustomers(branchId, model, request);
    }
}