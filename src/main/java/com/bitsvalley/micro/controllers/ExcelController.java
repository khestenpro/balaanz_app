package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.LedgerAccount;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.GeneralLedgerRepository;
import com.bitsvalley.micro.repositories.LedgerAccountRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.GeneralLedgerService;
import com.bitsvalley.micro.services.LedgerAccountService;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.LedgerEntryDTO;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
public class ExcelController extends SuperController {

    @Autowired
    GeneralLedgerRepository generalLedgerRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    LedgerAccountService ledgerAccountService;

    @Autowired
    GeneralLedgerService generalLedgerService;

//    @PostMapping("/import")
//    public void readExpenses(@RequestParam("file") MultipartFile reapExcelDataFile) throws IOException {
//
//        User user = userRepository.findByUserName(getLoggedInUserName());
////        List<Test> tempStudentList = new ArrayList<Test>();
//        XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
//        XSSFSheet worksheet = workbook.getSheetAt(0);
//        int j = 0;
//        //Run to Create ledger Accounts
//        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
//            XSSFRow row = worksheet.getRow(i);
////            Date dateCellValue = row.getCell(1).getDateCellValue();
//            String effectiveDate = row.getCell(2).getRawValue();
//            effectiveDate = effectiveDate.substring(0, 4)+"-"+effectiveDate.substring(4, 6) + "-01" ;
//
//            String notes = row.getCell(3).getStringCellValue();
//            String ledgerName = row.getCell(4).getStringCellValue();
//            String methodOfPayment = row.getCell(5).getStringCellValue();
//            double income = row.getCell(6).getNumericCellValue();
//            double expense = row.getCell(7).getNumericCellValue();
//            double amount = income == 0 ? expense : income;
//            LedgerAccount ledgerAccount = ledgerAccountRepository.findByNameAndOrgId(ledgerName, user.getOrgId());
//            LedgerEntryDTO ledgerEntryDTO = new LedgerEntryDTO();
//
////            if (ledgerAccount == null) {
////                if(income>0) {
////                    ledgerAccount = ledgerAccountService.createLedger(user.getOrgId(), ledgerName, "true", "3000 – 3999");
////                    ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.CREDIT);
////                    ledgerEntryDTO.setLedgerAmount(amount);
////                }
////                else {
////                    ledgerAccount = ledgerAccountService.createLedger(user.getOrgId(), ledgerName, "false", "6000 – 6999");
////                    ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.DEBIT);
////                    ledgerEntryDTO.setLedgerAmount(-1*amount);
////                }
////            } else {
////                if(income>0) {
////                    ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.CREDIT);
////                    ledgerEntryDTO.setLedgerAmount(amount);
////                }
////                else {
////                    ledgerEntryDTO.setCreditOrDebit(BVMicroUtils.DEBIT);
////                    ledgerEntryDTO.setLedgerAmount(-1*amount);
////                }
////            }
//            ledgerEntryDTO.setRecordDate(effectiveDate);
//            ledgerEntryDTO.setOrgId(user.getOrgId());
//            ledgerEntryDTO.setNotes(methodOfPayment + " - " + notes);
//            ledgerEntryDTO.setOriginLedgerAccount(ledgerAccount.getId());
//
//            LedgerAccount byNameAndOrgId = ledgerAccountRepository.findByNameAndOrgId(BVMicroUtils.CASH, user.getOrgId());
//            ledgerEntryDTO.setDestinationLedgerAccount(byNameAndOrgId.getId());
////            generalLedgerService.updateManualAccountTransaction(ledgerEntryDTO, true);
//        }
//        //Run to create transactions
//
//
//    }



    @PostMapping("/import")
    public void readGLs(@RequestParam("file") MultipartFile reapExcelDataFile) throws IOException {

        User user = userRepository.findByUserName(getLoggedInUserName());
//        List<Test> tempStudentList = new ArrayList<Test>();

//      OPCPackage pkg = OPCPackage.open(reapExcelDataFile.getInputStream());

//        HSSFWorkbook workbook = new HSSFWorkbook(reapExcelDataFile.getInputStream());
//        HSSFSheet worksheet = workbook.getSheetAt(0);

        XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);


        //Run to Create ledger Accounts
        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            XSSFRow row = worksheet.getRow(i);

            String ledgerNumber = row.getCell(0).getStringCellValue();
            String ledgerName = row.getCell(3).getStringCellValue();
            String credit_debit = row.getCell(7).getStringCellValue();

            LedgerAccount ledgerAccount = ledgerAccountRepository.findByNameAndOrgId(ledgerName, user.getOrgId());

            if (ledgerAccount == null) {
                ledgerAccount = new LedgerAccount();
                if(credit_debit.equals("C")) {
                    ledgerAccount.setCreditBalance("true");
                }
                else {
                    ledgerAccount.setCreditBalance("false");
                }
                ledgerAccount.setDisplayName(ledgerName);
                ledgerAccount.setName(ledgerName);
                ledgerAccount.setCode(ledgerName+"_GL_"+ledgerNumber);
                ledgerAccount.setCreatedDate(new Date());
                ledgerAccount.setCreatedBy(user.getFirstName());
                ledgerAccount.setOrgId(user.getOrgId());
                ledgerAccount.setActive(true);
                ledgerAccount.setCashAccountTransfer("true");
                ledgerAccount.setInterAccountTransfer("true");
                ledgerAccount.setCashTransaction("true");
                ledgerAccount.setStatus("ACTIVE");
//                ledgerAccount.setCategory(getCategoryCode(ledgerNumber));

                ledgerAccount.setOrgId(user.getOrgId());
                ledgerAccountRepository.save(ledgerAccount);

            }

        }

    }

    private String getCategoryCode(String ledgerNumber) {
        char c = ledgerNumber.charAt(0);
        if("1".equals(c)){
            return "100000 - 199999";
        }
        if("2".equals(c)){
            return "200000 - 299999";
        }
        if("3".equals(c)){
            return "300000 - 399999";
        }
        if("4".equals(c)){
            return "400000 - 499999";
        }
        if("5".equals(c)){
            return "500000 - 599999";
        }        if("6".equals(c)){
            return "600000 - 699999";
        }
        if("7".equals(c)){
            return "700000 - 799999";
        }
        if("8".equals(c)){
            return "800000 - 899999";
        }
//        ("9".equals(c)){
            return "900000 - 999999";
        }

}
