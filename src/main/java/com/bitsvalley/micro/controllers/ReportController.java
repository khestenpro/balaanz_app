package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.model.ReportsResponseModel;
import com.bitsvalley.micro.repositories.LedgerAccountRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.ReportService;
import com.bitsvalley.micro.webdomain.GLSearchDTO;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import com.bitsvalley.micro.webdomain.SavingReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Slf4j
public class ReportController extends SuperController {

    @Autowired
    ReportService reportService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    @GetMapping(value = "/reports")
    public String reports(ModelMap model, HttpServletRequest request) {

        User user = userRepository.findByUserName(getLoggedInUserName());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");

        ReportsResponseModel activeUsersReport = reportService.getActiveUsersReport(request);
        model.put("active_users",activeUsersReport);

//        SavingReportDTO savingReportDTO = reportService.savingReports(getLoggedInUserName(), runtimeSetting.getCountryCode());
//        GLSearchDTO glSearchDTO = new GLSearchDTO();
//        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
//        model.put("glSearchDTO", glSearchDTO);
//
//        model.put("descriptionValues", savingReportDTO.getDescriptionValues());
//        model.put("paidValues", savingReportDTO.getPaidValues());
//        model.put("dueValues", savingReportDTO.getDueValues());
//        model.put("sumTotalPaid", savingReportDTO.getSumTotalPaid());
//        model.put("sumTotalDue", savingReportDTO.getSumTotalDue());
        return "reports";

    }


    @GetMapping(value = "/filterReports")
    public String filterReports(ModelMap model, HttpServletRequest request) {

//        User user = userRepository.findByUserName(getLoggedInUserName());
//        SavingReportDTO savingReportDTO = reportService.savingReports(getLoggedInUserName());
//        GLSearchDTO glSearchDTO = new GLSearchDTO();
//        model.put("allLedgerAccount", ledgerAccountRepository.findByOrgIdAndActiveTrue(user.getOrgId()));
//        model.put("glSearchDTO", glSearchDTO);
//
//        model.put("descriptionValues", savingReportDTO.getDescriptionValues());
//        model.put("paidValues", savingReportDTO.getPaidValues());
//        model.put("dueValues", savingReportDTO.getDueValues());
//        model.put("sumTotalPaid", savingReportDTO.getSumTotalPaid());
//        model.put("sumTotalDue", savingReportDTO.getSumTotalDue());
        return "reports";
    }

    @PostMapping(value = "/filteredReports")
    public String filteredReports(ModelMap model, HttpServletRequest request){
        ReportsResponseModel activeUsersReport = reportService.getActiveUsersReport(request);
        model.put("active_users",activeUsersReport);
        return "reports";
    }

    @PostMapping("/accountsTotal")
    public void cumulateAllAccountsBalance(
      ModelMap model, HttpServletRequest request, HttpServletResponse response
    ) throws IOException {
        User user = userRepository.findByUserName(getLoggedInUserName());
        long orgId = user.getOrgId();
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        String html = reportService.allAccountsSum(orgId, runtimeSetting);
        response.setHeader("Content-disposition","attachment;filename="+ orgId+"_cumulative_accounts.pdf");
        generateByteOutputStream(response, html);
    }
}
