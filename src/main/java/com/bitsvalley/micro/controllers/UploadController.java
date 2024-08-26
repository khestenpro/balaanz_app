package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.RuntimeProperties;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.RuntimePropertiesRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.InitSystemService;
import com.bitsvalley.micro.services.UserService;
import com.bitsvalley.micro.utils.BVMicroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class UploadController extends SuperController{

    @Autowired
    UserService userService;

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RuntimePropertiesRepository runtimePropertiesRepository;

    @GetMapping("/file")
    public String uploadFile(){
        return "upload";
    }

    @GetMapping(value = "/fileLogo/{userName}")
    public String uploadLogoUsername(@PathVariable("userName") String userName, ModelMap model, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());
        request.getSession().setAttribute("userName",user.getUserName());
        model.put("userName", user.getUserName());
        return "uploadLogo";
    }

    @GetMapping(value = "/file/{userName}")
    public String uploadFileUsername(@PathVariable("userName") String userName, ModelMap model, HttpServletRequest request) {
        User loggedInUser = userRepository.findByUserName(getLoggedInUserName());
        User user = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        request.getSession().setAttribute("userName",user.getUserName());
        model.put("userName", user.getUserName());
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {
        User loggedInUser = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        String userName = (String)request.getSession().getAttribute("userName");
        // check if file is empty
        if (file.isEmpty() ) {
            attributes.addFlashAttribute("message", "Please select files to upload.");
            return "redirect:/";
        }
        // normalize the file path
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

//        String rpath = request.getRealPath("/");
        String rpath = "";
//        rpath = rpath + "/assets/images/" + imageId; // whatever path you used for storing the file

        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", loggedInUser.getOrgId()).getPropertyValue();
        String completePAth = rpath + UPLOAD_DIR +"_"+ userName +"_id_"+fileName;

        // save the file on the local file system
        Path path = null;
        try {
//            for(int i = 0; i< 4; i++ ){
            path = Paths.get(completePAth);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // return success response
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');
        User aUser = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        aUser.setIdFilePath(completePAth);
        userService.saveUser(aUser);
        return "upload";
    }


    @PostMapping("/upload2")
    public String uploadFile2(@RequestParam("file2") MultipartFile file2, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {
        String userName = (String)request.getSession().getAttribute("userName");
        User loggedInUser = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        // check if file is empty
        if (file2.isEmpty() ) {
            attributes.addFlashAttribute("message", "Please select files to upload.");
            return "redirect:/";
        }
        // normalize the file path
        String fileName = StringUtils.cleanPath(file2.getOriginalFilename());
        String rpath = "";

        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", loggedInUser.getOrgId()).getPropertyValue();
        String completePAth = rpath + UPLOAD_DIR +"_"+ userName +"_id_"+fileName;

        // save the file on the local file system
        Path path = null;
        try {
            path = Paths.get(completePAth);
            Files.copy(file2.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // return success response
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');
        User aUser = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        aUser.setIdFilePath2(completePAth);
        userService.saveUser(aUser);
        model.put("user",aUser);
        return "upload";
    }


    @PostMapping("/upload3")
    public String uploadFile3(@RequestParam("file3") MultipartFile file3, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {
        String userName = (String)request.getSession().getAttribute("userName");
        User loggedInUser = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        // check if file is empty
        if (file3.isEmpty() ) {
            attributes.addFlashAttribute("message", "Please select files to upload.");
            return "redirect:/";
        }
        // normalize the file path
        String fileName = StringUtils.cleanPath(file3.getOriginalFilename());
        String rpath = "";

        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", loggedInUser.getOrgId()).getPropertyValue();
        String completePAth = rpath + UPLOAD_DIR +"_"+ userName +"_id_"+fileName;

        // save the file on the local file system
        Path path = null;
        try {
            path = Paths.get(completePAth);
            Files.copy(file3.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // return success response
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');
        User aUser = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        aUser.setIdFilePath3(completePAth);
        userService.saveUser(aUser);
        model.put("user",aUser);
        return "upload";
    }

    @PostMapping("/upload4")
    public String uploadFile4(@RequestParam("file4") MultipartFile file4, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {
        String userName = (String)request.getSession().getAttribute("userName");
        User loggedInUser = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        // check if file is empty
        if (file4.isEmpty() ) {
            attributes.addFlashAttribute("message", "Please select files to upload.");
            return "redirect:/";
        }
        // normalize the file path
        String fileName = StringUtils.cleanPath(file4.getOriginalFilename());
        String rpath = "";

        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", loggedInUser.getOrgId()).getPropertyValue();
        String completePAth = rpath + UPLOAD_DIR +"_"+ userName +"_id_"+fileName;

        // save the file on the local file system
        Path path = null;
        try {
            path = Paths.get(completePAth);
            Files.copy(file4.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // return success response
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');

        User aUser = userService.findByUserNameAndOrgId(loggedInUser.getUserName(),loggedInUser.getOrgId());

        aUser.setIdFilePath4(completePAth);
        userService.saveUser(aUser);
        model.put("user",aUser);
        return "upload";
    }



    @PostMapping("/uploadLogo")
    public String uploadLogo(@RequestParam("file") MultipartFile file, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {

        if (file.isEmpty()) {
            attributes.addFlashAttribute("message", "Please select a logo to upload.");
            return "redirect:/";
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        User user = userRepository.findByUserName(getLoggedInUserName());
        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory",user.getOrgId()).getPropertyValue();
        String completePath = UPLOAD_DIR +fileName;
        RuntimeProperties logo = runtimePropertiesRepository.findByPropertyNameAndOrgId("logo", user.getOrgId());
        if(logo == null){
            logo = new RuntimeProperties();
        }

        logo.setPropertyValue(completePath);
        runtimePropertiesRepository.save(logo);
        Path path = null;
        try {
            path = Paths.get(completePath);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.put("runtimeSetting",initSystemService.findByOrgId(user.getOrgId()));
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');
        return "settings";
    }


    @PostMapping("/uploadUnionLogo")
    public String uploadUnionLogo(@RequestParam("file") MultipartFile file, RedirectAttributes attributes,HttpServletRequest request, ModelMap model) {

        User user = userRepository.findByUserName(getLoggedInUserName());

        if (file.isEmpty()) {
            attributes.addFlashAttribute("message", "Please select a union logo to upload.");
            return "redirect:/";
        }
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//        String rpath = request.getRealPath("/");

        final String UPLOAD_DIR = runtimePropertiesRepository.findByPropertyNameAndOrgId("Upload Directory", user.getOrgId()).getPropertyValue();
        String completePath =  UPLOAD_DIR +fileName;
        RuntimeProperties unionLogo = runtimePropertiesRepository.findByPropertyNameAndOrgId("unionLogo", user.getOrgId());
        unionLogo.setPropertyValue(completePath);
        Path path = null;
        try {
            path = Paths.get(completePath);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        runtimePropertiesRepository.save(unionLogo);
        model.put("runtimeSetting",initSystemService.findByOrgId(user.getOrgId()));
        attributes.addFlashAttribute("message", "You successfully uploaded " + fileName + '!');
        return "settings";
    }
}
