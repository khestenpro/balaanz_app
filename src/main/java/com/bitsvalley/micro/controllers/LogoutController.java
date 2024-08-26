package com.bitsvalley.micro.controllers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LogoutController extends SuperController{

    @GetMapping(value = "/welcomeout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        String referrer = (String)request.getSession().getAttribute("referer");
        if(null == referrer){
            referrer = "login";
        }
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response,
                    authentication);
        }
        if(StringUtils.equals(referrer,"evolution") || StringUtils.equals(referrer,"tbc") || StringUtils.equals(referrer,"mgv"))
        return "redirect:/"+referrer;
        return "redirect:/";
    }
}
