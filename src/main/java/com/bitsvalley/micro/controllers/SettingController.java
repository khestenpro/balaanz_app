package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.RuntimeProperties;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.RuntimePropertiesRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.InitSystemService;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class SettingController extends SuperController{


    @Autowired
    InitSystemService initSystemService;

    @Autowired
    RuntimePropertiesRepository runtimePropertiesRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping(value = "/oneil93")
    public String initSystem(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
            initSystemService.initSystem(user.getOrgId());
            model.put("runtimeSetting", initSystemService.findByOrgId(user.getOrgId()));
            return "settings";
    }
    @GetMapping(value = "/initUserRoles")
    public String initUserRoles(ModelMap model) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        initSystemService.initUserRoles();
        model.put("runtimeSetting", initSystemService.findByOrgId(user.getOrgId()));
        return "settings";
    }


    @GetMapping(value = "/viewsettings")
    public String viewSystem(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        model.put("runtimeSetting",initSystemService.findByOrgId(user.getOrgId()));
        return "settings";
    }


    @GetMapping(value = "/registerSetting")
    public String settingForm( ModelMap model) {
        RuntimeProperties rp = new RuntimeProperties();
        model.put("settingProperty", rp);
        return "settingProperty";
    }


    @PostMapping(value = "/registerSettingForm")
    public String createListSetting(@ModelAttribute("runtimeSetting") RuntimeProperties rp, ModelMap model) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        RuntimeProperties byPropertyName = runtimePropertiesRepository.findByPropertyNameAndOrgId(rp.getPropertyName(), user.getOrgId());

        rp.setOrgId(user.getOrgId());
        if( byPropertyName != null ){
            runtimePropertiesRepository.delete(byPropertyName);
        }
        runtimePropertiesRepository.save(rp);
        model.put("settingInfo","Setting Saved");
        return settingForm(model);

    }


    @PostMapping(value = "/saveSettingForm")
    public String createListSetting(@ModelAttribute("runtimeSetting") RuntimeSetting runtimeSetting,
                                    ModelMap model,
                                    HttpServletRequest request) {

        User user = userRepository.findByUserName(getLoggedInUserName());
        createRuntimeOrgProperties( runtimeSetting, user.getOrgId() );
        request.getSession().setAttribute("runtimeSettings", initSystemService.findByOrgId(user.getOrgId()));
        model.put("settingsInfo", " SETTINGS SAVED ");
        model.put("runtimeSetting", initSystemService.findByOrgId(user.getOrgId()));
        return "settings";
    }

}
