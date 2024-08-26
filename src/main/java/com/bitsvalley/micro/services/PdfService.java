package com.bitsvalley.micro.services;


import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.model.CartUserDetails;
import com.bitsvalley.micro.model.PosReceiptPdf;
import com.bitsvalley.micro.repositories.BranchRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.utils.Amortization;
import com.bitsvalley.micro.utils.AmortizationRowEntry;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.bitsvalley.micro.utils.POSReceiptHtml.*;
import static java.lang.String.format;

@Service
@Slf4j
public class PdfService {

    public static final String CURRENCY = "frs CFA";
    @Autowired
    BranchRepository branchRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    public String generateSavingTransactionReceiptPDF(DailySavingAccountTransaction savingAccountTransaction, RuntimeSetting rt, boolean displayBalance) {
        String accountBalance = "";
        String accountDue = "";
        String representativeText = StringUtils.equals(savingAccountTransaction.getRepresentative(), BVMicroUtils.getFullName(savingAccountTransaction.getDailySavingAccount().getUser())) ? "" : "Remittance: " + savingAccountTransaction.getRepresentative();
        User aUser = userRepository.findByUserName(savingAccountTransaction.getCreatedBy());
        if (savingAccountTransaction.getAccountOwner() != null && StringUtils.equals("true", savingAccountTransaction.getAccountOwner()) && displayBalance) {
            accountBalance = BVMicroUtils.formatCurrency(savingAccountTransaction.getAccountBalance(), rt.getCountryCode());

            double dueAmount = savingAccountTransaction.getDailySavingAccount().getAccountMinBalance() - savingAccountTransaction.getDailySavingAccount().getAccountBalance();
            if (dueAmount > 0) {
                accountDue = "Balance Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
            }

        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<font style=\"font-size:1.4em;color:" + rt.getThemeColor() + ";\">" +
                "RECEIPT FOR <b>" + savingAccountTransaction.getDailySavingAccount().getAccountType().getDisplayName() + "</b> ACCOUNT TRANSACTION</font>" +
                "<table border=\"1\" width=\"100%\">" +
                "<tr> <td><table><tr><td>" +
                "<img width=\"80\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/> Reference No:<br/><b>" + savingAccountTransaction.getReference() +
                "</b></td><td><b><font style=\"font-size:1.0em;color:" + rt.getThemeColor() + ";\"> " + rt.getBusinessName() + "</font></b><br/><font style=\"font-size:0.7em;color:black;\"> " + rt.getSlogan() + "</font><br/>" + rt.getAddress() + "<br/>" + rt.getTelephone() + "<br/>" + rt.getEmail() + "<br/>" +
                "</td></tr></table></td>" +
                "<td>" +
                " Branch No: " + savingAccountTransaction.getBranchCode() +
                "<br/>" + savingAccountTransaction.getModeOfPayment() + ": " + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()) + "<br/> " + representativeText + "<br/>Date: " + BVMicroUtils.formatDateTime(savingAccountTransaction.getCreatedDate()) + "</td></tr>" +
                "<tr><td>" +
                "Account Number:<b><br/>" + BVMicroUtils.getFormatAccountNumber(savingAccountTransaction.getDailySavingAccount().getAccountNumber())

                + "</b><br/>Customer: <b>" + BVMicroUtils.getFullName(savingAccountTransaction.getDailySavingAccount().getUser()) +

                "</b> </td>" +
                "<td>Account Balance: <b>" + accountBalance + "</b>" + rt.getCurrency() + "<br/>" + accountDue + "Transaction Amount: <font style=\"font-size:1.6em;color:black;\">"
                + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()) + "</font> " + rt.getCurrency() + "</td></tr>" +
                "<tr><td style=\"width:50%;\">" +
                "<b>Agent Representative:</b> <br/>" + BVMicroUtils.getFullName(aUser) + "<br/><b>Notes:</b> <br/>" + savingAccountTransaction.getNotes() + "<br/><b>Amount in Letters: </b> <font color=\" " + rt.getThemeColor() + "\" size=\"8px\">" +
                " <br/>" + savingAccountTransaction.getSavingAmountInLetters() + "</font></td>\n" +
                " <td><table  border=\"1\" width=\"100%\" class=\"center\">" +
                "              <tr>" +
                "                      <th colspan=\"2\"><font style=\"font-size:1.2em;color:" + rt.getThemeColor() + ";\">Bill Selection - Cash Breakdown</font></th>" +
                "               </tr>" +
                "                 <tr>" +
                "                 <td> 10 000 x " + savingAccountTransaction.getTenThousand() + " = <b>" + BVMicroUtils.formatCurrency(10000 * savingAccountTransaction.getTenThousand(), rt.getCountryCode()) + "</b><br/>" +
                "                 5 000 x " + savingAccountTransaction.getFiveThousand() + " = <b>" + BVMicroUtils.formatCurrency(5000 * savingAccountTransaction.getFiveThousand(), rt.getCountryCode()) + "</b><br/>" +
                "                 2 000 x " + savingAccountTransaction.getTwoThousand() + " = <b>" + BVMicroUtils.formatCurrency(2000 * savingAccountTransaction.getTwoThousand(), rt.getCountryCode()) + "</b><br/>" +
                "                 1 000 x" + savingAccountTransaction.getOneThousand() + " = <b>" + BVMicroUtils.formatCurrency(1000 * savingAccountTransaction.getOneThousand(), rt.getCountryCode()) + "</b><br/>" +
                "                 500 x " + savingAccountTransaction.getFiveHundred() + " = <b>" + BVMicroUtils.formatCurrency(500 * savingAccountTransaction.getFiveHundred(), rt.getCountryCode()) + "</b><br/>" +
                "                 100 x " + savingAccountTransaction.getOneHundred() + " = <b>" + BVMicroUtils.formatCurrency(100 * savingAccountTransaction.getOneHundred(), rt.getCountryCode()) + "</b><br/>" +

                "</td><td>" +
                "                 50 x " + savingAccountTransaction.getFifty() + " = <b>" + 50 * savingAccountTransaction.getFifty() + "</b><br/>" +
                "                 25 x " + savingAccountTransaction.getTwentyFive() + " = <b>" + 25 * savingAccountTransaction.getTwentyFive() + "</b><br/>" +
                "                 10 x " + savingAccountTransaction.getTen() + " = <b>" + 10 * savingAccountTransaction.getTen() + "</b><br/>" +
                "                 5 x " + savingAccountTransaction.getFive() + " = <b>" + 5 * savingAccountTransaction.getFive() + "</b><br/>" +
                "                 1 x " + savingAccountTransaction.getOne() + " = <b>" + 1 * savingAccountTransaction.getOne() + "</b><br/>" +

                "                  </td></tr>" +
                "                 </table></td></tr></table>" +
                "       <table><tr><td><br/><br/>Cashier Signature: ------------------------------------- "+rt.getContextName()+" Signature: -------------------------------------<br/> " + branchRepository.findByCodeAndOrgId(savingAccountTransaction.getBranchCode(), savingAccountTransaction.getOrgId()).getName() + "</td>" +
                "</tr></table>";
        savingBilanzNoInterest = "<html><head></head><body>" + savingBilanzNoInterest +
                "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- <br/><br/>" + savingBilanzNoInterest + "</body></html>";
        return savingBilanzNoInterest;
    }

    public String generateSavingTransactionReceiptPDF(SavingAccountTransaction savingAccountTransaction, RuntimeSetting rt, boolean printBalance) {
        String accountBalance = "";
        String accountDue = "";
        String representativeText = StringUtils.equals(savingAccountTransaction.getRepresentative(), BVMicroUtils.getFullName(savingAccountTransaction.getSavingAccount().getUser())) ? "" : "Remittance: " + savingAccountTransaction.getRepresentative();
        User aUser = userRepository.findByUserName(savingAccountTransaction.getCreatedBy());
        if (savingAccountTransaction.getAccountOwner() != null && StringUtils.equals("true", savingAccountTransaction.getAccountOwner()) && printBalance) {
            accountBalance = BVMicroUtils.formatCurrency(savingAccountTransaction.getAccountBalance(), rt.getCountryCode());
            double dueAmount = savingAccountTransaction.getSavingAccount().getAccountMinBalance() - savingAccountTransaction.getSavingAccount().getAccountBalance();
            if (dueAmount > 0) {
                accountDue = "Balance Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
            }
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<table border=\"0\" width=\"100%\" >" +
                "    <tr>" +
                "        <td class=\"receipt-title\" align=\"center\">" +
                "            <p>RECEIPT FOR " + savingAccountTransaction.getSavingAccount().getAccountType().getDisplayName() + " ACCOUNT TRANSACTION</p>" +
                "        </td>" +
                "    </tr>" +
                "    <tr>" +
                "        <td class=\"transaction-receipt-container\">" +
                "            <table width=\"100%\" border=\"0\">" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col\" valign=\"top\">" +
                "                        <table border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"logo\" valign=\"middle\">" +
                "                                    <img src=\"" + prefix + rt.getUnionLogo() + "\" />" +
                "                                </td>" +
                "                                <td class=\"company-details\" valign=\"middle\">" +
                "                                    <table border=\"0\" width=\"100%\">" +
                "                                       <tr>" +
                "                                           <td class=\"header-title\">" + rt.getBusinessName() + "</td>" +
                "                                       </tr>" +
                "                                       <tr>" +
                "                                           <td class=\"receipt-parameters\"><p>" + rt.getSlogan() + "</p></td>" +
                "                                       </tr>" +
                "                                   </table>" +
                "                                </td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"col\" valign=\"top\">" +
                "                        <table border=\"0\" width=\"100%\">" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Address: <strong>" + rt.getAddress() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Phone: <strong>" + rt.getTelephone() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Email: <strong>" + rt.getEmail() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Date: <strong>" + BVMicroUtils.formatDateTime(savingAccountTransaction.getCreatedDate()) + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Reference No: <strong>" + savingAccountTransaction.getReference() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Branch No: <strong>" + savingAccountTransaction.getBranchCode() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>" + savingAccountTransaction.getModeOfPayment() + ": <strong>" + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Remittance: <strong>" + representativeText + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"col no-border bottom-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Account Number: <strong>" + BVMicroUtils.getFormatAccountNumber(savingAccountTransaction.getSavingAccount().getAccountNumber()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Customer: <strong>" + BVMicroUtils.getFullName(savingAccountTransaction.getSavingAccount().getUser()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Agent Representative:  <strong>" + BVMicroUtils.getFullName(aUser) + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col no-border bottom-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Account Balance: <strong>" + accountBalance + " " + rt.getCurrency() + " " + accountDue + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Transaction Amount: <strong>" + BVMicroUtils.formatCurrency(savingAccountTransaction.getSavingAmount(), rt.getCountryCode()) + " " + rt.getCurrency() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Amount in Letters: <strong>" + savingAccountTransaction.getSavingAmountInLetters() + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col top-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Notes: <strong>" + savingAccountTransaction.getNotes() + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col-full\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td colspan=\"4\" class=\"header-title-sub\">" +
                "                                    Bill Selection - Cash Breakdown" +
                "                                </td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td valign=\"top\">" +
                "                                    <table width=\"100%\" border=\"0\">  " +
                "                                        <tr>" +
                getDynamicSavingSelection(savingAccountTransaction, rt) +
                "                                        </tr> " +
                "                                    </table>" +
                "                                </td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "            </table>" +
                "        </td>" +
                "    </tr>" +
                "    <tr>" +
                "        <td class=\"signature\">" +
                "            <table width=\"100%\" border=\"0\">" +
                "                <tr>" +
                "                    <td class=\"col\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                           <tr>" +
                "                            <td class=\"sign-labelA\"><p>Cashier Signature:</p></td>" +
                "                            <td class=\"sign-empty\"></td>" +
                "                           </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                           <tr>" +
                "                            <td class=\"sign-labelB\"><p>"+rt.getContextName()+" Signature:</p></td>" +
                "                            <td class=\"sign-empty\"></td>" +
                "                           </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"signlocation\" colspan=\"3\">" + branchRepository.findByCodeAndOrgId(savingAccountTransaction.getSavingAccount().getUser().getBranch().getCode(), savingAccountTransaction.getOrgId()).getName() + "</td>" +
                "                </tr>" +
                "            </table>" +
                "        </td>" +
                "    </tr>" +
                "</table>";
        savingBilanzNoInterest = "<html>" +
                "<head>" +
                "    <style>" +
                "* { margin: 0px; padding: 0px; font-size: 14px; font-family: Arial, Helvetica, sans-serif; }" +
                "table { border:none; border-collapse: collapse; }" +
                ".logo { padding-right: 10px; }" +
                ".logo img { width: 75px; }" +
                ".company-details { }" +
                ".transaction-receipt-container { border:1px solid #000000; padding: 10px; }" +
                ".transaction-receipt-container .col { width: 47%; border-bottom: 1px dashed #000000; padding: 7px 0px; }" +
                ".transaction-receipt-container .col-space { width: 6%; }" +
                ".transaction-receipt-container .col-full { width: 100%; }" +
                ".transaction-receipt-container .col.no-border { border-bottom: 0px; }" +
                ".receipt-title { padding-top: 0px; padding-bottom: 10px; }" +
                ".receipt-title p { font-size: 16px; color:#000000; }" +
                ".header-title { font-size: 18px; color:#000000; font-weight: bold; padding: 0px; margin:0px; }" +
                ".header-title-sub {font-size: 16px; color:#000000; font-weight: bold; padding: 10px 0px; }" +
                "p { font-size: 14px; margin:0px; padding:0px; }" +
                "p strong { font-size: 14px; }" +
                ".address p { color:#000000; }" +
                ".receipt-parameters { padding: 2px 0px; }" +
                ".receipt-parameters p { color: #666666; }" +
                ".receipt-parameters p strong { color: #000000; }" +
                ".signature { padding-top: 10px; padding-bottom: 10px; }" +
                ".signature .col { width: 47%; }" +
                ".signature .col p { white-space: nowrap; }" +
                ".signature .col-space { width: 6%; }" +
                ".signature .sign-empty { border-bottom: 1px dashed #000000; width: 100%; }" +
                ".signature .sign-labelA { width: 130px; }" +
                ".signature .sign-labelB { width: 150px; }" +
                ".seperator { white-space: nowrap; overflow: hidden; }" +
                ".signlocation { padding: 7px 0px 0px; }" +
                ".transaction-receipt-container .col.bottom-pad-0 { padding-bottom: 0px; }" +
                ".transaction-receipt-container .col.top-pad-0 { padding-top: 0px; }" +
                "</style>" +
                "</head>" +
                "<body>" + savingBilanzNoInterest +
                "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- <br/>" + savingBilanzNoInterest + "</body></html>";
        return savingBilanzNoInterest;
    }


    public String generateShareDetailsPDF(ShareAccountTransaction shareAccountTransaction, RuntimeSetting rt) {
        User aUser = userRepository.findByUserName(shareAccountTransaction.getCreatedBy());

        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String shareBilanzNoInterest = "<html><head>" +
                "</head><body><br/><br/><font style=\"font-size:1.4em;color:black;\">" +
                "<b>RECEIPT FOR SHARE ACCOUNT TRANSACTION</b></font>" +
                "<table border=\"1\" width=\"100%\">" +
                "<tr> <td><img width=\"75\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/> Reference No:" + shareAccountTransaction.getReference() +
                "<br/>Date:<b>" + BVMicroUtils.formatDateTime(shareAccountTransaction.getCreatedDate()) + "</b> </td>" +
                "<td>" +
                "<b><font style=\"font-size:1.6em;color:black;\"> " + rt.getBusinessName() + "</font></b><br/> Branch No: " + shareAccountTransaction.getBranchCode() +
                "<br/>Address:" + rt.getAddress() + "<br/> Telephone:" + rt.getTelephone() +
                "<br/>" + shareAccountTransaction.getModeOfPayment() + " "+rt.getCurrentAccount() +" " + BVMicroUtils.formatCurrency(shareAccountTransaction.getShareAmount(), rt.getCountryCode()) + "</td></tr>" +
                "<tr><td>" +
                "Account Number: " + BVMicroUtils.getFormatAccountNumber(shareAccountTransaction.getShareAccount().getAccountNumber())
                + "<br/>Customer: <b>" + shareAccountTransaction.getShareAccount().getUser().getLastName() + ","
                + shareAccountTransaction.getShareAccount().getUser().getFirstName() + "</b> </td>" +
                "<td>Total Share Balance: <b>" + BVMicroUtils.formatCurrency(shareAccountTransaction.getShareAccount().getAccountBalance(), rt.getCountryCode()) + "</b><br/> Amount:<font style=\"font-size:1.6em;color:black;\">"
                + BVMicroUtils.formatCurrency(shareAccountTransaction.getShareAmount(), rt.getCountryCode()) + "</font></td></tr>" +
                "        <tr><td>" +
                "Representative: <b>" + shareAccountTransaction.getCreatedBy() + "</b> - " + BVMicroUtils.getFullName(aUser) + "<br/> </td><td> <font color=\"" + rt.getThemeColor() + "\" size=\"8px\"> " +
                "</font><br/>Notes:" + shareAccountTransaction.getNotes() + "</td>\n" +
                "    </tr></table>" +
                "    <table  border=\"1\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th><font style=\"font-size:1.2em;color:black;\">Bill Selection - Cash Breakdown</font><font style=\"font-size:1.6em;color:black;\"> " + BVMicroUtils.formatCurrency(shareAccountTransaction.getShareAmount(), rt.getCountryCode()) + " " + rt.getCurrency() + "</font></th>\n" +
                "            </tr>\n" +
                "        </table>" +
                "<br/> " +
                "       <table>" +
                "       <tr><td>Cashier Signature: ------------------------------ "+rt.getContextName()+" Signature: ------------------------------<br/>" + branchRepository.findByCodeAndOrgId(shareAccountTransaction.getBranchCode(), shareAccountTransaction.getOrgId()).getName() + "</td>" +
                "       <td></td></tr>" +
                "       </table>" +
                "<br/><br/>" +
                "" +
                "</body></html>";
        return shareBilanzNoInterest;
    }

    public String generateCurrentTransactionReceiptPDF(CurrentAccountTransaction currentAccountTransaction, RuntimeSetting rt, boolean displayBalance) {

        String accountBalance = "";
        String accountDue = "";

        String representativeText = StringUtils.equals(currentAccountTransaction.getRepresentative(), BVMicroUtils.getFullName(currentAccountTransaction.getCurrentAccount().getUser())) ? "" : "Remittance: " + currentAccountTransaction.getRepresentative();
        User aUser = userRepository.findByUserName(currentAccountTransaction.getCreatedBy());
        if (currentAccountTransaction.getAccountOwner() != null && StringUtils.equals("true", currentAccountTransaction.getAccountOwner()) && displayBalance) {
            accountBalance = BVMicroUtils.formatCurrency(currentAccountTransaction.getAccountBalance(), rt.getCountryCode());
            double dueAmount = currentAccountTransaction.getCurrentAccount().getAccountMinBalance() - currentAccountTransaction.getCurrentAccount().getAccountBalance();
            if (dueAmount > 0) {
                accountDue = "Account Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
            }
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String currentBilanzNoInterest = "<table border=\"0\" width=\"100%\" >" +
                "    <tr>" +
                "        <td class=\"receipt-title\" align=\"center\">" +
                "            <p>RECEIPT FOR " + currentAccountTransaction.getCurrentAccount().getAccountType().getDisplayName() + " ACCOUNT TRANSACTION</p>" +
                "        </td>" +
                "    </tr>" +
                "    <tr>" +
                "        <td class=\"transaction-receipt-container\">" +
                "            <table width=\"100%\" border=\"0\">" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col\" valign=\"top\">" +
                "                        <table border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"logo\" valign=\"middle\">" +
                "                                    <img src=\"" + prefix + rt.getUnionLogo() + "\" />" +
                "                                </td>" +
                "                                <td class=\"company-details\" valign=\"middle\">" +
                "                                    <table border=\"0\" width=\"100%\">" +
                "                                       <tr>" +
                "                                           <td class=\"header-title\">" + rt.getBusinessName() + "</td>" +
                "                                       </tr>" +
                "                                       <tr>" +
                "                                           <td class=\"receipt-parameters\"><p>" + rt.getSlogan() + "</p></td>" +
                "                                       </tr>" +
                "                                   </table>" +
                "                                </td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"col\" valign=\"top\">" +
                "                        <table border=\"0\" width=\"100%\">" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Address: <strong>" + rt.getAddress() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Phone: <strong>" + rt.getTelephone() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td colspan=\"2\" class=\"receipt-parameters\"><p>Email: <strong>" + rt.getEmail() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Date: <strong>" + BVMicroUtils.formatDateTime(currentAccountTransaction.getCreatedDate()) + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Reference No: <strong>" + currentAccountTransaction.getReference() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Branch No: <strong>" + currentAccountTransaction.getBranchCode() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>" + currentAccountTransaction.getModeOfPayment() + ": <strong>" + BVMicroUtils.formatCurrency(currentAccountTransaction.getCurrentAmount(), rt.getCountryCode()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Remittance: <strong>" + representativeText + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"col no-border bottom-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Account Number: <strong>" + BVMicroUtils.getFormatAccountNumber(currentAccountTransaction.getCurrentAccount().getAccountNumber()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Customer: <strong>" + BVMicroUtils.getFullName(currentAccountTransaction.getCurrentAccount().getUser()) + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Agent Representative:  <strong>" + BVMicroUtils.getFullName(aUser) + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col no-border bottom-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Account Balance: <strong>" + accountBalance + " " + rt.getCurrency() + " " + accountDue + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Transaction Amount: <strong>" + BVMicroUtils.formatCurrency(currentAccountTransaction.getCurrentAmount(), rt.getCountryCode()) + " " + rt.getCurrency() + "</strong></p></td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Amount in Letters: <strong>" + currentAccountTransaction.getCurrentAmountInLetters() + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col top-pad-0\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td class=\"receipt-parameters\"><p>Notes: <strong>" + currentAccountTransaction.getNotes() + "</strong></p></td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td colspan=\"3\" class=\"col-full\" valign=\"top\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                            <tr>" +
                "                                <td colspan=\"4\" class=\"header-title-sub\">" +
                "                                    Bill Selection - Cash Breakdown" +
                "                                </td>" +
                "                            </tr>" +
                "                            <tr>" +
                "                                <td valign=\"top\">" +
                "                                    <table width=\"100%\" border=\"0\">  " +
                "                                        <tr>" +
                getDynamicSelection(currentAccountTransaction, rt) +
                "                                        </tr> " +
                "                                    </table>" +
                "                                </td>" +
                "                            </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "            </table>" +
                "        </td>" +
                "    </tr>" +
                "    <tr>" +
                "        <td class=\"signature\">" +
                "            <table width=\"100%\" border=\"0\">" +
                "                <tr>" +
                "                    <td class=\"col\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                           <tr>" +
                "                            <td class=\"sign-labelA\"><p>Cashier Signature:</p></td>" +
                "                            <td class=\"sign-empty\"></td>" +
                "                           </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                    <td class=\"col-space\"></td>" +
                "                    <td class=\"col\">" +
                "                        <table width=\"100%\" border=\"0\">" +
                "                           <tr>" +
                "                            <td class=\"sign-labelB\"><p>"+rt.getContextName()+" Signature:</p></td>" +
                "                            <td class=\"sign-empty\"></td>" +
                "                           </tr>" +
                "                        </table>" +
                "                    </td>" +
                "                </tr>" +
                "                <tr>" +
                "                    <td class=\"signlocation\" colspan=\"3\">" + branchRepository.findByCodeAndOrgId(currentAccountTransaction.getBranchCode(), currentAccountTransaction.getOrgId()).getName() + "</td>" +
                "                </tr>" +
                "            </table>" +
                "        </td>" +
                "    </tr>" +
                "</table>";
        currentBilanzNoInterest = "<html>" +
                "<head>" +
                "    <style>" +
                "* { margin: 0px; padding: 0px; font-size: 14px; font-family: Arial, Helvetica, sans-serif; }" +
                "table { border:none; border-collapse: collapse; }" +
                ".logo { padding-right: 10px; }" +
                ".logo img { width: 75px; }" +
                ".company-details { }" +
                ".transaction-receipt-container { border:1px solid #000000; padding: 10px; }" +
                ".transaction-receipt-container .col { width: 47%; border-bottom: 1px dashed #000000; padding: 7px 0px; }" +
                ".transaction-receipt-container .col-space { width: 6%; }" +
                ".transaction-receipt-container .col-full { width: 100%; }" +
                ".transaction-receipt-container .col.no-border { border-bottom: 0px; }" +
                ".receipt-title { padding-top: 0px; padding-bottom: 10px; }" +
                ".receipt-title p { font-size: 16px; color:#000000; }" +
                ".header-title { font-size: 18px; color:#000000; font-weight: bold; padding: 0px; margin:0px; }" +
                ".header-title-sub {font-size: 16px; color:#000000; font-weight: bold; padding: 10px 0px; }" +
                "p { font-size: 14px; margin:0px; padding:0px; }" +
                "p strong { font-size: 14px; }" +
                ".address p { color:#000000; }" +
                ".receipt-parameters { padding: 2px 0px; }" +
                ".receipt-parameters p { color: #666666; }" +
                ".receipt-parameters p strong { color: #000000; }" +
                ".signature { padding-top: 10px; padding-bottom: 10px; }" +
                ".signature .col { width: 47%; }" +
                ".signature .col p { white-space: nowrap; }" +
                ".signature .col-space { width: 6%; }" +
                ".signature .sign-empty { border-bottom: 1px dashed #000000; width: 100%; }" +
                ".signature .sign-labelA { width: 130px; }" +
                ".signature .sign-labelB { width: 150px; }" +
                ".seperator { white-space: nowrap; overflow: hidden; }" +
                ".signlocation { padding: 7px 0px 0px; }" +
                ".transaction-receipt-container .col.bottom-pad-0 { padding-bottom: 0px; }" +
                ".transaction-receipt-container .col.top-pad-0 { padding-top: 0px; }" +
                "</style>" +
                "</head>" +
                "<body>" + currentBilanzNoInterest +
                "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- <br/>" + currentBilanzNoInterest + "</body></html>";
        return currentBilanzNoInterest;
    }


    private String getDynamicSelection(CurrentAccountTransaction currentAccountTransaction, RuntimeSetting rt) {
        StringBuilder current = new StringBuilder();
        if (currentAccountTransaction.getTenThousand() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>10 000 x " + currentAccountTransaction.getTenThousand() + " : <strong>" + BVMicroUtils.formatCurrency(10000 * currentAccountTransaction.getTenThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getFiveThousand() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>5 000 x " + currentAccountTransaction.getFiveThousand() + " : <strong>" + BVMicroUtils.formatCurrency(5000 * currentAccountTransaction.getFiveThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getTwoThousand() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>2 000 x " + currentAccountTransaction.getTwoThousand() + " : <strong>" + BVMicroUtils.formatCurrency(2000 * currentAccountTransaction.getTwoThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getOneThousand() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>1 000 x " + currentAccountTransaction.getOneThousand() + " : <strong>" + BVMicroUtils.formatCurrency(1000 * currentAccountTransaction.getOneThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getFiveHundred() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>500 x " + currentAccountTransaction.getFiveHundred() + " : <strong>" + BVMicroUtils.formatCurrency(500 * currentAccountTransaction.getFiveHundred(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getOneHundred() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>100 x " + currentAccountTransaction.getOneHundred() + " : <strong>" + BVMicroUtils.formatCurrency(100 * currentAccountTransaction.getOneHundred(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getFifty() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>50 x " + currentAccountTransaction.getFifty() + " : <strong>" + BVMicroUtils.formatCurrency(50 * currentAccountTransaction.getFifty(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getTwentyFive() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>25 x " + currentAccountTransaction.getTwentyFive() + " : <strong>" + BVMicroUtils.formatCurrency(25 * currentAccountTransaction.getTwentyFive(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getTen() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>10 x " + currentAccountTransaction.getTen() + " : <strong>" + BVMicroUtils.formatCurrency(10 * currentAccountTransaction.getTen(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (currentAccountTransaction.getFive() > 0) {
            current.append( "<td class=\"receipt-parameters\"><p>5x " + currentAccountTransaction.getFive() + " : <strong>" + BVMicroUtils.formatCurrency(5 * currentAccountTransaction.getOne(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        return current.toString();
    }

    private String getDynamicSavingSelection(SavingAccountTransaction savingAccountTransaction, RuntimeSetting rt) {
        StringBuilder saving = new StringBuilder();
        if (savingAccountTransaction.getTenThousand() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>10 000 x " + savingAccountTransaction.getTenThousand() + " : <strong>" + BVMicroUtils.formatCurrency(10000 * savingAccountTransaction.getTenThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getFiveThousand() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>5 000 x " + savingAccountTransaction.getFiveThousand() + " : <strong>" + BVMicroUtils.formatCurrency(5000 * savingAccountTransaction.getFiveThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getTwoThousand() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>2 000 x " + savingAccountTransaction.getTwoThousand() + " : <strong>" + BVMicroUtils.formatCurrency(2000 * savingAccountTransaction.getTwoThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getOneThousand() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>1 000 x " + savingAccountTransaction.getOneThousand() + " : <strong>" + BVMicroUtils.formatCurrency(1000 * savingAccountTransaction.getOneThousand(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getFiveHundred() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>500 x " + savingAccountTransaction.getFiveHundred() + " : <strong>" + BVMicroUtils.formatCurrency(500 * savingAccountTransaction.getFiveHundred(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getOneHundred() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>100 x " + savingAccountTransaction.getOneHundred() + " : <strong>" + BVMicroUtils.formatCurrency(100 * savingAccountTransaction.getOneHundred(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getFifty() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>50 x " + savingAccountTransaction.getFifty() + " : <strong>" + BVMicroUtils.formatCurrency(50 * savingAccountTransaction.getFifty(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getTwentyFive() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>25 x " + savingAccountTransaction.getTwentyFive() + " : <strong>" + BVMicroUtils.formatCurrency(25 * savingAccountTransaction.getTwentyFive(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getTen() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>10 x " + savingAccountTransaction.getTen() + " : <strong>" + BVMicroUtils.formatCurrency(10 * savingAccountTransaction.getTen(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        if (savingAccountTransaction.getFive() > 0) {
            saving.append( "<td class=\"receipt-parameters\"><p>5x " + savingAccountTransaction.getFive() + " : <strong>" + BVMicroUtils.formatCurrency(5 * savingAccountTransaction.getOne(), rt.getCountryCode())).append( "</strong></p></td>");
        }
        return saving.toString();
    }


    public String generateLoanTransactionReceiptPDF(LoanAccountTransaction loanAccountTransaction, RuntimeSetting rt, boolean displayBalance) {

        String showAmount = "";
        String representativeText = StringUtils.equals(loanAccountTransaction.getRepresentative(), BVMicroUtils.getFullName(loanAccountTransaction.getLoanAccount().getUser())) ? "" : "Remittance: " + loanAccountTransaction.getRepresentative();
        User aUser = userRepository.findByUserName(loanAccountTransaction.getCreatedBy());
        if (loanAccountTransaction.getAccountOwner() != null && StringUtils.equals("true", loanAccountTransaction.getAccountOwner()) && displayBalance) {
            showAmount = BVMicroUtils.formatCurrency(loanAccountTransaction.getAccountBalance(), rt.getCountryCode());
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String loanBilanzNoInterest = "<font style=\"font-size:1.4em;color:black;\">" +
                "<b>RECEIPT FOR " + loanAccountTransaction.getLoanAccount().getAccountType().getDisplayName() + " TRANSACTION</b></font>" +
                "<table border=\"1\" width=\"100%\">" +
                "<tr> <td><table><tr><td>" +
                "<img width=\"75\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/> Reference No:<br/>" + loanAccountTransaction.getReference() +
                "</td><td><b><font style=\"font-size:1.6em;color:black;\"> " + rt.getBusinessName() + "</font></b><br/>" + rt.getAddress() + "<br/>" + rt.getTelephone() + "<br/>" + rt.getEmail() + "<br/>" +
                "</td></tr></table></td>" +
                "<td>" +
                " Branch No: " + loanAccountTransaction.getBranchCode() +
                "<br/>" + loanAccountTransaction.getModeOfPayment() + ":" + BVMicroUtils.formatCurrency(loanAccountTransaction.getLoanAmount(), rt.getCountryCode()) + "<br/> " + representativeText + "<br/>Date:" + BVMicroUtils.formatDateTime(loanAccountTransaction.getCreatedDate()) + "</td></tr>" +
                "<tr><td>" +
                "Account Number:<b>" + BVMicroUtils.getFormatAccountNumber(loanAccountTransaction.getLoanAccount().getAccountNumber())

                + "</b><br/>Customer: <b>" + BVMicroUtils.getFullName(loanAccountTransaction.getLoanAccount().getUser()) +

                "</b> </td>" +
                "<td>Loan Balance: <b>" + showAmount + "</b><br/> Total Repayment Amount: <font style=\"font-size:1.6em;color:black;\">"
                + BVMicroUtils.formatCurrency(loanAccountTransaction.getAmountReceived(), rt.getCountryCode()) + "</font></td></tr>" +
                "        <tr><td colspan=\"2\">" +
                "Agent Representative: <b>" + BVMicroUtils.getFullName(aUser) + "</b><br/>Notes:" + loanAccountTransaction.getNotes() + "</td>\n" +
                "    </tr></table>" +
                "    <table  border=\"1\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th><font style=\"font-size:1.2em;color:black;\">Bill Selection - Cash Breakdown</font><font style=\"font-size:1.6em;color:black;\"> " + BVMicroUtils.formatCurrency(loanAccountTransaction.getAmountReceived(), rt.getCountryCode()) + " " + rt.getCurrency() + "</font></th>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "               <td> 10 000 x " + loanAccountTransaction.getTenThousand() + " = <b>" + 10000 * loanAccountTransaction.getTenThousand() + "</b>," +
                "               5 000 x " + loanAccountTransaction.getFiveThousand() + " = <b>" + 5000 * loanAccountTransaction.getFiveThousand() + "</b>," +
                "               2 000 x " + loanAccountTransaction.getTwoThousand() + " = <b>" + 2000 * loanAccountTransaction.getTwoThousand() + "</b>," +
                "               1 000 x " + loanAccountTransaction.getOneThousand() + " = <b>" + 1000 * loanAccountTransaction.getOneThousand() + "</b>" +
                "               500 x " + loanAccountTransaction.getFiveHundred() + " = <b>" + 500 * loanAccountTransaction.getFiveHundred() + "</b>," +
                "               100 x " + loanAccountTransaction.getOneHundred() + " = <b>" + 100 * loanAccountTransaction.getOneHundred() + "</b>," +
                "               50 x " + loanAccountTransaction.getFifty() + " = <b>" + 50 * loanAccountTransaction.getFifty() + "</b>," +
                "               25 x " + loanAccountTransaction.getTwentyFive() + " = <b>" + 25 * loanAccountTransaction.getTwentyFive() + "</b>" +
                "               Amount in Letters: <font color=\"" + rt.getThemeColor() + "\" size=\"8px\"> "
                + loanAccountTransaction.getLoanAmountInLetters() + "</font> </td></tr>" +
                "        </table>" +
                "       <table><tr><td><br/><br/>Cashier Signature: ------------------------------ "+rt.getContextName()+" Signature: ------------------------------<br/> " + branchRepository.findByCodeAndOrgId(loanAccountTransaction.getBranchCode(), loanAccountTransaction.getOrgId()).getName() + "</td>" +
                "</tr></table><br/>";
        loanBilanzNoInterest = "<html><head></head><body>" + loanBilanzNoInterest + loanBilanzNoInterest + "</body></html>";
        return loanBilanzNoInterest;
    }


    public String generatePDFSavingBilanzList(SavingBilanzList savingBilanzList, SavingAccount savingAccount, String logoPath, RuntimeSetting rt) throws IOException {
        String accountDue = "";
        double dueAmount = savingAccount.getAccountMinBalance() - savingAccount.getAccountBalance();
        if (dueAmount > 0) {
            accountDue = "Account Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
        }

        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<html>" +
                "<head>" +
                "    <style>" +
                "        * { font-family: Arial, Helvetica, sans-serif; }" +
                "        body { margin: 0px; padding: 0px; }" +
                "        table { width: 100%; border: none; }" +
                "        h6 { font-size: 18px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        h2 { font-size: 22px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        h4 { font-size: 18px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        p { font-size: 14px; font-weight: normal; margin: 0px; width: 100%; }" +
                "        .logo-section { padding-bottom: 15px; }" +
                "        .address-section h2 { margin-bottom: 7px; }" +
                "        .statement-blocks { border-top: 2px solid #000000; border-bottom:  2px solid #000000; padding: 10px 0px; }" +
                "        .statement-blocks table tr td { padding-top: 5px; padding-bottom: 5px; }" +
                "        .statement-blocks label { font-size: 12px; color:#999999; margin: 0px; }" +
                "        .statement-blocks p { font-size: 14px; color: #000000; margin: 0px; padding-top: 3px; }" +
                "        .statement-blocks h6 { padding-top: 3px; }" +
                "        .pdf-list { padding-top: 20px; }" +
                "        .pdf-list table, .pdf-list { border: none; border-collapse: collapse; }" +
                "        .pdf-list td, .pdf-list th { padding: 10px 10px; font-size: 12px; font-weight: normal; }" +
                "        .pdf-list th { background: " + rt.getThemeColor() + "; color: #ffffff; }" +
                "        .pdf-list th:first-child { border-radius: 5px 0 0 0; }" +
                "        .pdf-list th:last-child { border-radius: 0 5px 0 0; }" +
                "        .pdf-list td { color: #000000; border-bottom: 1px solid #cdcdcd; }" +
                "        .pdf-list td.total { text-align: right; font-size: 18px; font-weight: bold; }" +
                "        .pdf-list td.total label { font-weight: normal; }" +
                "        .pdf-list td.wordWrap { word-wrap: break-word; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "        <table>" +
                "            <tr>" +
                "                <td>" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td class=\"logo-section\" width=\"50%\" align=\"left\" valign=\"top\">" +
                "                                <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
                "                            </td>" +
                "                            <td class=\"address-section\" width=\"50%\" align=\"right\" valign=\"top\">" +
                "                                <h2>Saving Account Statement</h2>" +
                "                                <p>" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</p>" +
                "                            </td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td>" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td align=\"left\"><h4>" + rt.getBusinessName() + "</h4></td>" +
                "                            <td align=\"right\"><p>" + rt.getSlogan() + "</p></td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td class=\"statement-blocks\">" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Account Number:</label>" +
                "                                <p style=\"font:12px\" color=\"" + rt.getThemeColor() + "\">" + BVMicroUtils.getFormatAccountNumber(savingAccount.getAccountNumber()) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Period From:</label>" +
                "                                <p style=\"font:12px\" >" + BVMicroUtils.formatDate(savingAccount.getCreatedDate()) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Period To:</label>" +
                "                                <p style=\"font:12px\" >" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Product Number:</label>" +
                "                                <p style=\"font:12px\" >" + savingAccount.getProductCode() + "</p>" +
                "                            </td>" +
                "                        </tr>" +
                "                        <tr>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Branch Code:</label>" +
                "                                <p style=\"font:12px\" >" + savingAccount.getBranchCode() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Branch Name:</label>" +
                "                                <p style=\"font:12px\" >" + savingAccount.getUser().getBranch().getName() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Customer:</label>" +
                "                                <p style=\"font:12px\" >" + savingAccount.getUser().getGender() + ". " + savingAccount.getUser().getFirstName() + " " + savingAccount.getUser().getLastName() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Closing Balance:</label>" +
                "                                <h6>" + savingBilanzList.getTotalSaving() + " " + rt.getCurrency() + "</h6>" +
                "                            </td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td class=\"pdf-list\">" +
                "                    <table>" +
                "                        <tr>" +
                "                            <th align=\"left\">Date</th>" +
                "                            <th align=\"left\">Branch/MOP <br/> Notes</th>" +
                "                            <th align=\"right\">Debit</th>" +
                "                            <th align=\"right\">Credit</th>" +
                "                            <th align=\"right\">Balance</th>" +
                "                        </tr>" + getTableList(savingBilanzList, rt) +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "        </table>" +
                "</body>" +
                "</html>";
        return savingBilanzNoInterest;
    }


    public String generatePDFSavingBilanzListInterval(SavingBilanzList savingBilanzList, SavingAccount savingAccount, String logoPath, RuntimeSetting rt, GLSearchDTO glSearchDTO) throws IOException {
        Integer startYear = Integer.parseInt(glSearchDTO.getStartDate().substring(0, 4));
        Integer endYear = Integer.parseInt(glSearchDTO.getEndDate().substring(0, 4));
        Integer startMonth = Integer.parseInt(glSearchDTO.getStartDate().substring(5, 7));
        String startDayofMonth = glSearchDTO.getStartDate().substring(8, 10);
        Integer endMonth = Integer.parseInt(glSearchDTO.getEndDate().substring(5, 7));
        Integer endDayofMonth = Integer.parseInt(glSearchDTO.getEndDate().substring(8, 10));
        LocalDateTime startDate = LocalDateTime.of(startYear, startMonth, 5, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(endYear, endMonth, endDayofMonth, 1, 1);
        String accountDue = "";
        double dueAmount = savingAccount.getAccountMinBalance() - savingAccount.getAccountBalance();
        if (dueAmount > 0) {
            accountDue = "Account Due: <b>" + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + "</b> " + rt.getCurrency() + "<br/>";
        }

        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<html><head><style>\n" +
                "#transactions {\n" +
                "  border-collapse: collapse;\n" +
                "  width: 100%;\n" +
                "}\n" +
                "\n" +
                "#transactions td, #customers th {\n" +
                "  border: 1px solid #ddd;\n" +
                "  padding: 4px;\n" +
                "}\n" +
                "\n" +
                "#transactions tr:nth-child(even){background-color: \"" + rt.getThemeColor2() + "\";}\n" +
                "\n" +
                "#transactions tr:hover {background-color: #ddd;}\n" +
                "\n" +
                "#transactions th {\n" +
                "  padding-top: 6px;\n" +
                "  padding-bottom: 6px;\n" +
                "  text-align: left;\n" +
                "  background-color: #cda893;\n" +
                "  color: white;\n" +
                "}\n" +
                "</style>" +
                "</head><body><br/><br/>" +
                "    <table border=\"0\" width=\"100%\">" +
                "        <tr><td align=\"center\"> <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/><b><font size=\"6\">" + rt.getBusinessName() + "</font></b><br/><font size=\"2\"> " + rt.getSlogan() + "</font></td>" +
                "       <td colspan=\"2\"><b><font size=\"4\" color=\"" + rt.getThemeColor() + "\">" + savingAccount.getAccountSavingType().getDisplayName() + " STATEMENT</font></b></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period From:</font></td>" +
                "       <td align=\"right\"><font size=\"4\"> " + startDayofMonth + " " + startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + startYear + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period To:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + endDayofMonth + " " + endDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + endYear + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Account Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getAccountNumber() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Product Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getProductCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Code:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getBranchCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Name:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getUser().getBranch().getName() + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"4\" color=\"" + rt.getThemeColor() + "\">Customer:</font></td>" +
                "       <td align=\"right\">" + savingAccount.getUser().getGender() + ". " + savingAccount.getUser().getFirstName() + " " + savingAccount.getUser().getLastName() + "</td></tr>" +
                "       </table><br/><br/><br/>" +
                "       <p>Beginning Balance: <b>" + savingBilanzList.getSavingBilanzList().get(0).getCurrentBalance() + "</b> " + rt.getCurrency() + "</p>" + "<br/>" +
                "    <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th>Date</th>\n" +
                "                <th style=\"font:12px\"> Branch/MOP </th>\n" +
                "                <th>Agent</th>\n" +
                "                <th>Notes</th>\n" +
                "                <th>Debit</th>\n" +
                "                <th>Credit</th>\n" +
                "                <th></th>\n" +
                "            </tr>\n" + getTableList(savingBilanzList, rt) +
                "            <tr>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td colspan=\"3\">Total: <font size=\"10px\"><b>" + savingBilanzList.getTotalSaving() + "</b> " + rt.getCurrency() + "</font></td>\n" +
                "                \n" +
                "            </tr>" +
                "        </table><br/>" +
                "       <p><span style=\"margin-right: 58px;\">Closing Balance: <b>" + savingBilanzList.getTotalSaving() + "</b> " + rt.getCurrency() + "</span> " + accountDue + "</p>" +
                "      " + "<p>Ending Balance: <b>" + savingBilanzList.getSavingBilanzList().get(savingBilanzList.getSavingBilanzList().size() - 1).getCurrentBalance() + "</b> " + rt.getCurrency() + "</p>" +
                "</body></html>";
        return savingBilanzNoInterest;
    }


    public String generatePDFDailySavingBilanzList(SavingBilanzList savingBilanzList, DailySavingAccount savingAccount, String logoPath, RuntimeSetting rt) throws IOException {
        String accountDue = "";
        double dueAmount = savingAccount.getAccountMinBalance() - savingAccount.getAccountBalance();
        if (dueAmount > 0) {
            accountDue = "Account Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<html><head><style>\n" +
                "#transactions {\n" +
                "  border-collapse: collapse;\n" +
                "  width: 100%;\n" +
                "}\n" +
                "\n" +
                "#transactions td, #customers th {\n" +
                "  border: 1px solid #ddd;\n" +
                "  padding: 4px;\n" +
                "}\n" +
                "\n" +
                "#transactions tr:nth-child(even){background-color: \"" + rt.getThemeColor2() + "\";}\n" +
                "\n" +
                "#transactions tr:hover {background-color: #ddd;}\n" +
                "\n" +
                "#transactions th {\n" +
                "  padding-top: 6px;\n" +
                "  padding-bottom: 6px;\n" +
                "  text-align: left;\n" +
                "  background-color: #cda893;\n" +
                "  color: white;\n" +
                "}\n" +
                "</style>" +
                "</head><body><br/><br/>" +
                "    <table border=\"0\" width=\"100%\">" +
                "        <tr><td align=\"center\"> <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/><b><font size=\"6\">" + rt.getBusinessName() + "</font></b><br/><font size=\"2\"> " + rt.getSlogan() + "</font></td>" +
                "       <td colspan=\"2\"><b><font size=\"4\" color=\"" + rt.getThemeColor() + "\">" + savingAccount.getAccountSavingType().getDisplayName() + " STATEMENT</font></b></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period From:</font></td>" +
                "       <td align=\"right\"><font size=\"4\"> " + BVMicroUtils.formatDate(savingAccount.getCreatedDate()) + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period To:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Account Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getAccountNumber() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Product Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getProductCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Code:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getBranchCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Name:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + savingAccount.getUser().getBranch().getName() + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"4\" color=\"" + rt.getThemeColor() + "\">Customer:</font></td>" +
                "       <td align=\"right\">" + savingAccount.getUser().getGender() + ". " + savingAccount.getUser().getFirstName() + " " + savingAccount.getUser().getLastName() + "</td></tr>" +
                "       </table><br/><br/><br/>" +
                "    <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th>Date</th>\n" +
                "                <th style=\"font:12px\"> Branch/MOP </th>\n" +
                "                <th>Agent</th>\n" +
                "                <th>Reference</th>\n" +
                "                <th>Notes</th>\n" +
                "                <th>Debit</th>\n" +
                "                <th>Credit</th>\n" +
                "                <th></th>\n" +
                "            </tr>\n" + getTableList(savingBilanzList, rt) +
                "            <tr>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td colspan=\"3\">Total:<font size=\"10px\"><b>" + savingBilanzList.getTotalSaving() + "</b> " + rt.getCurrency() + "</font></td>\n" +
                "                \n" +
                "            </tr>" +
                "        </table><br/>" +
                "       <table><tr><th>Closing Balance:</th><th> " + savingBilanzList.getTotalSaving() + "  " + rt.getCurrency() + "<br/>" + accountDue + "</th></tr>" +
                "       </table>" +
                "</body></html>";
        return savingBilanzNoInterest;
    }


    public String generatePDFCurrentBilanzList(CurrentBilanzList currentBilanzList, CurrentAccount currentAccount,
                                               String logoPath, RuntimeSetting rt) throws IOException {

        String accountDue = "";
        double dueAmount = currentAccount.getAccountMinBalance() - currentAccount.getAccountBalance();
        if (dueAmount > 0) {
            accountDue = "Account Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String currentBilanzNoInterest = "<html>" +
                "<head>" +
                "    <style>" +
                "        * { font-family: Arial, Helvetica, sans-serif; }" +
                "        body { margin: 0px; padding: 0px; }" +
                "        table { width: 100%; border: none; }" +
                "        h6 { font-size: 18px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        h2 { font-size: 22px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        h4 { font-size: 18px; font-weight: bold; margin: 0px; width: 100%; }" +
                "        p { font-size: 14px; font-weight: normal; margin: 0px; width: 100%; }" +
                "        .logo-section { padding-bottom: 15px; }" +
                "        .address-section h2 { margin-bottom: 7px; }" +
                "        .statement-blocks { border-top: 2px solid #000000; border-bottom:  2px solid #000000; padding: 10px 0px; }" +
                "        .statement-blocks table tr td { padding-top: 5px; padding-bottom: 5px; }" +
                "        .statement-blocks label { font-size: 12px; color:#999999; margin: 0px; }" +
                "        .statement-blocks p { font-size: 14px; color: #000000; margin: 0px; padding-top: 3px; }" +
                "        .statement-blocks h6 { padding-top: 3px; }" +
                "        .pdf-list { padding-top: 20px; }" +
                "        .pdf-list table, .pdf-list { border: none; border-collapse: collapse; }" +
                "        .pdf-list td, .pdf-list th { padding: 10px 10px; font-size: 12px; font-weight: normal; }" +
                "        .pdf-list th { background: " + rt.getThemeColor() + "; color: #ffffff; }" +
                "        .pdf-list th:first-child { border-radius: 5px 0 0 0; }" +
                "        .pdf-list th:last-child { border-radius: 0 5px 0 0; }" +
                "        .pdf-list td { color: #000000; border-bottom: 1px solid #cdcdcd; }" +
                "        .pdf-list td.total { text-align: right; font-size: 18px; font-weight: bold; }" +
                "        .pdf-list td.total label { font-weight: normal; }" +
                "        .pdf-list td.wordWrap { word-wrap: break-word; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "        <table>" +
                "            <tr>" +
                "                <td>" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td class=\"logo-section\" width=\"50%\" align=\"left\" valign=\"top\">" +
                "                                <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
                "                            </td>" +
                "                            <td class=\"address-section\" width=\"50%\" align=\"right\" valign=\"top\">" +
                "                                <h2>"+ rt.getCurrentAccount()+" Statement</h2>" +
                "                                <p>" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</p>" +
                "                            </td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td>" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td align=\"left\"><h4>" + rt.getBusinessName() + "</h4></td>" +
                "                            <td align=\"right\"><p>" + rt.getSlogan() + "</p></td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td class=\"statement-blocks\">" +
                "                    <table>" +
                "                        <tr>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Account Number:</label>" +
                "                                <p style=\"font:12px\" color=\"" + rt.getThemeColor() + "\">" + BVMicroUtils.getFormatAccountNumber(currentAccount.getAccountNumber()) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Period From:</label>" +
                "                                <p style=\"font:12px\" >" + BVMicroUtils.formatDate(currentAccount.getCreatedDate()) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Period To:</label>" +
                "                                <p style=\"font:12px\" >" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Product Number:</label>" +
                "                                <p style=\"font:12px\" >" + currentAccount.getProductCode() + "</p>" +
                "                            </td>" +
                "                        </tr>" +
                "                        <tr>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Branch Code:</label>" +
                "                                <p style=\"font:12px\" >" + currentAccount.getBranchCode() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Branch Name:</label>" +
                "                                <p style=\"font:12px\" >" + currentAccount.getUser().getBranch().getName() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Customer:</label>" +
                "                                <p style=\"font:12px\" >" + currentAccount.getUser().getGender() + ". " + currentAccount.getUser().getFirstName() + " " + currentAccount.getUser().getLastName() + "</p>" +
                "                            </td>" +
                "                            <td valign=\"top\" width=\"25%\">" +
                "                                <label>Closing Balance:</label>" +
                "                                <h6>" + currentBilanzList.getTotalCurrent() + " " + rt.getCurrency() + "</h6>" +
                "                            </td>" +
                "                        </tr>" +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "            <tr>" +
                "                <td class=\"pdf-list\">" +
                "                    <table>" +
                "                        <tr>" +
                "                            <th align=\"left\">Date</th>" +
                "                            <th align=\"left\">Branch/MOP <br/> Notes</th>" +
                "                            <th align=\"right\">Debit</th>" +
                "                            <th align=\"right\">Credit</th>" +
                "                            <th align=\"right\">Balance</th>" +
                "                        </tr>" + getTableList(currentBilanzList, rt) +
                "                    </table>" +
                "                </td>" +
                "            </tr>" +
                "        </table>" +
                "</body>" +
                "</html>";
        return currentBilanzNoInterest;
    }


    public String generatePDFLoanBilanzList(LoanBilanzList loanBilanzList, LoanAccount loanAccount, RuntimeSetting rt) throws IOException {
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String savingBilanzNoInterest = "<html><head><style>\n" +
                "#transactions {\n" +
                "  border-collapse: collapse;\n" +
                "  width: 100%;\n" +
                "}\n" +
                "\n" +
                "#transactions td, #customers th {\n" +
                "  border: 1px solid #ddd;\n" +
                "  padding: 4px;\n" +
                "}\n" +
                "\n" +
                "#transactions tr:nth-child(even){background-color: " + rt.getThemeColor2() + ";}\n" +
                "\n" +
                "#transactions tr:hover {background-color: " + rt.getThemeColor2() + ";}\n" +
                "\n" +
                "#transactions th {\n" +
                "  padding-top: 6px;\n" +
                "  padding-bottom: 6px;\n" +
                "  text-align: left;\n" +
                "  background-color: " + rt.getThemeColor() + ";\n" +
                "  color: white;\n" +
                "}\n" +
                "</style>" +
                "</head><body><br/>" +
                "    <table border=\"0\" width=\"100%\">" +
                "        <tr><td align=\"center\"> <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/>" + rt.getBusinessName() + " <br/> Together each achieves more</td>" +
                "       <td colspan=\"2\"><b><font size=\"4\" color=\"green\">" + loanAccount.getAccountType().getDisplayName() + " STATEMENT</font></b></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Period From:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(loanAccount.getCreatedDate()) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Period To:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Account Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.getFormatAccountNumber(loanAccount.getAccountNumber()) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Product Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + loanAccount.getProductCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Branch Code:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + loanAccount.getBranchCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"green\">Branch Name:</font></td>" +
                "       <td align=\"right\"><font size=\"4\"> " + loanAccount.getUser().getBranch().getName() + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"4\" color=\"green\">Customer Shortname:</font></td>" +
                "       <td align=\"right\">" + loanAccount.getUser().getLastName() + "</td></tr>" +
                "       </table><br/><br/><br/>" +
                "    <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th>Date</th>\n" +
                "                <th style=\"font:12px\">Branch/MOP</th>\n" +
                "                <th style=\"font:12px\">Agent</th>\n" +
                "                <th style=\"font:12px\">Reference</th>\n" +
                "                <th style=\"font:12px\">Notes</th>\n" +
                "                <th style=\"font:12px\">VAT</th>\n" +
                "                <th style=\"font:12px\">Interest</th>\n" +
                "                <th style=\"font:12px\">Debit</th>\n" +
                "                <th style=\"font:12px\">Credit</th>\n" +
                "                <th style=\"font:12px\">Balance</th>\n" +
                "            </tr>\n" + getTableList(loanBilanzList, rt.getCurrentAccount()) +
                "        </table><br/>" +
//                "    <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"center\">\n" +
//                "       <tr><th id=\"transactions\">Opening Balance</th><th>1000</th></tr>" +
//                "       <tr><td>Credit Sum</td> <td></td></tr>" +
//                "       <tr><td>Debit Sum</td> <td></td></tr></table>" +
                "       <table><tr><th>Closing Balance: </th><th> " + loanBilanzList.getCurrentLoanBalance() + " " + rt.getCurrency() + "</th></tr>" +
//              "       <tr><td>Bamenda Branch, N W Region</td><td>"+ BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) +"</td></tr>" +
                "       </table></body></html>";
        return savingBilanzNoInterest;
    }

    public String generatePDFCurrentBilanzListInterval(CurrentBilanzList currentBilanzList, CurrentAccount currentAccount,
                                                       String logoPath, RuntimeSetting rt, GLSearchDTO glSearchDTO) throws IOException {
        Integer startYear = Integer.parseInt(glSearchDTO.getStartDate().substring(0, 4));
        Integer endYear = Integer.parseInt(glSearchDTO.getEndDate().substring(0, 4));
        Integer startMonth = Integer.parseInt(glSearchDTO.getStartDate().substring(5, 7));
        String startDayofMonth = glSearchDTO.getStartDate().substring(8, 10);
        Integer endMonth = Integer.parseInt(glSearchDTO.getEndDate().substring(5, 7));
        Integer endDayofMonth = Integer.parseInt(glSearchDTO.getEndDate().substring(8, 10));
        LocalDateTime startDate = LocalDateTime.of(startYear, startMonth, 5, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(endYear, endMonth, endDayofMonth, 1, 1);
        String accountDue = "";
        double dueAmount = currentAccount.getAccountMinBalance() - currentAccount.getAccountBalance();
        if (dueAmount > 0) {
            accountDue = "Account Due: " + BVMicroUtils.formatCurrency(dueAmount, rt.getCountryCode()) + " " + rt.getCurrency() + "<br/>";
        }
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String currentBilanzNoInterest = "<html><head><style>\n" +
                "#transactions {\n" +
                "  border-collapse: collapse;\n" +
                "  width: 100%;\n" +
                "}\n" +
                "\n" +
                "#transactions td, #customers th {\n" +
                "  border: 1px solid #ddd;\n" +
                "  padding: 4px;\n" +
                "}\n" +
                "\n" +
                "#transactions tr:nth-child(even){background-color: \"" + rt.getThemeColor2() + "\";}\n" +
                "\n" +
                "#transactions tr:hover {background-color: #ddd;}\n" +
                "\n" +
                "#transactions th {\n" +
                "  padding-top: 6px;\n" +
                "  padding-bottom: 6px;\n" +
                "  text-align: left;\n" +
                "  background-color: #cda893;\n" +
                "  color: white;\n" +
                "}\n" +
                "</style>" +
                "</head><body>" +
                "    <table border=\"0\" width=\"100%\">" +
                "        <tr><td align=\"center\"> <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/><br/>" + rt.getBusinessName() + " <br/>" + rt.getSlogan() + "</td>" +
                "       <td colspan=\"2\"><b><font size=\"4\" color=\"" + rt.getThemeColor() + "\">"+rt.getCurrentAccount()+" STATEMENT</font></b></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period From:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + startDayofMonth + " " + startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + startYear + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Period To:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + endDayofMonth + " " + endDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + endYear + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Account Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + BVMicroUtils.getFormatAccountNumber(currentAccount.getAccountNumber()) + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Product Number:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + currentAccount.getProductCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Code:</font></td>" +
                "       <td align=\"right\"><font size=\"4\">" + currentAccount.getBranchCode() + "</font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"6\" color=\"" + rt.getThemeColor() + "\">Branch Name:</font></td>" +
                "       <td align=\"right\"><font size=\"4\"> " + currentAccount.getUser().getBranch().getName() + " </font></td></tr>" +
                "        <tr><td> </td><td> </td>" +
                "       <td align=\"right\"><font size=\"4\" color=\"" + rt.getThemeColor() + "\">Customer:</font></td>" +
                "       <td align=\"right\">" + currentAccount.getUser().getGender() + ". " + currentAccount.getUser().getFirstName() + " " + currentAccount.getUser().getLastName() + "</td></tr>" +
                "       </table><br/>" + "<p>Beginning Balance: <b>" + currentBilanzList.getCurrentBilanzList().get(0).getCurrentBalance() + "</b> " + rt.getCurrency() + "</p>" + "<br/>" +
                "    <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"center\">\n" +
                "            <tr>\n" +
                "                <th style=\"font:12px\">Date</th>\n" +
                "                <th style=\"font:12px\">Branch/MOP </th>\n" +
                "                <th style=\"font:12px\">Agent</th>\n" +
                "                <th style=\"font:12px\">Notes</th>\n" +
                "                <th style=\"font:12px\">Debit</th>\n" +
                "                <th style=\"font:12px\">Credit</th>\n" +
                "                <th></th>\n" +
                "            </tr>\n" + getTableList(currentBilanzList, rt) +
                "            <tr>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td></td>\n" +
                "                <td colspan=\"3\">Total: <font size=\"10px\"><b>" + currentBilanzList.getTotalCurrent() + "</b> " + rt.getCurrency() + "</font></td>\n" +
                "                \n" +
                "            </tr>" +
                "        </table><br/>" +
                "       <table><tr><th>Closing Balance:</th><th> " + currentBilanzList.getTotalCurrent() + " " + rt.getCurrency() + "<br/>" + accountDue + "</th></tr>" +
                "       </table></body></html>";
        return currentBilanzNoInterest;
    }

    private String getShortOfMinTable(List<SavingAccount> savingAccounts, RuntimeSetting rt) {
        String tableHtml = "";
        for (SavingAccount savingAccount : savingAccounts) {
            tableHtml = tableHtml +
                    "<tr><td>" + BVMicroUtils.getFullName(savingAccount.getUser()) + "<br/><i>Tel:" + savingAccount.getUser().getTelephone1() + "</i></td>" +
                    "<td align=\"right\">" + savingAccount.getAccountType().getDisplayName() + "</td>" +
                    "<td align=\"right\">" + BVMicroUtils.formatCurrency(savingAccount.getAccountMinBalance(), rt.getCountryCode()) + "</td>" +
                    "<td align=\"right\">" + BVMicroUtils.formatCurrency(savingAccount.getAccountBalance(), rt.getCountryCode()) + "</td>" +
                    "<td align=\"right\">" + BVMicroUtils.formatCurrency(savingAccount.getAccountMinBalance() - savingAccount.getAccountBalance(), rt.getCountryCode()) + "</td>" +
                    "</tr>";
        }
        return tableHtml;
    }

    private String getTableList(Invoice invoice, RuntimeSetting rt) {
        String tableHtml = "";
        for (InvoiceLineItemDetail invoiceLineItem : invoice.getInvoiceLineItemDetail()) {
            tableHtml = tableHtml +
                    "<tr><td>" + invoiceLineItem.getDescription() + "</td>" +
                    "<td align=\"right\">" + invoiceLineItem.getQuantity() + "</td>" +
                    "<td align=\"right\">" + BVMicroUtils.formatCurrency(invoiceLineItem.getUnitPrice(), rt.getCountryCode()) + "</td>" +
                    "<td align=\"right\">" + BVMicroUtils.formatCurrency(invoiceLineItem.getTotal(), rt.getCountryCode()) + "</td>" +
                    "</tr>";
        }
        return tableHtml;
    }

    private String getTableList(LoanBilanzList loanBilanzList, String currentAccount) {
        String tableHtml = "";
        for (LoanBilanz bilanz : loanBilanzList.getLoanBilanzList()) {
            tableHtml = tableHtml + "<tr><td style=\"font:10px\">" + bilanz.getCreatedDate() + "</td>" +
                    "<td>" + bilanz.getBranch() + "/" + bilanz.getModeOfPayment() + "</td>" +
                    "<td>" + bilanz.getAgent() + "</td>" +
                    "<td style=\"font:10px\">" + bilanz.getReference() + "</td>" +
                    "<td style=\"font:10px\">" + bilanz.getNotes() + "</td>" +
                    "<td>" + bilanz.getVatPercent() + "</td>" +
                    "<td>" + bilanz.getInterestAccrued() + "</td>" +
                    "<td>" + getLoanDebitBalance(bilanz) + "</td>" +
                    "<td>" + getLoanCreditBalance(bilanz) + "</td>" +
                    "<td>" + bilanz.getCurrentBalance() + "</td>" +
                    "</tr>";
        }
        return tableHtml;
    }

    private String getLoanDebitBalance(LoanBilanz loanBilanz) {
        if (loanBilanz.getModeOfPayment().equals("RECEIPT")) {
            return loanBilanz.getLoanAmount();
        }
        return "";
    }

    private String getLoanCreditBalance(LoanBilanz loanBilanz) {
        if (!loanBilanz.getModeOfPayment().equals("RECEIPT")) {
            return loanBilanz.getAmountReceived();
        }
        return "";
    }

    private String getSavingDebitBalance(SavingBilanz savingBilanz, String countryCode) {
        if (savingBilanz.getSavingAmount() < 0) {
            return BVMicroUtils.formatCurrency(savingBilanz.getSavingAmount() * -1, countryCode);
        }
        return "";
    }

    private String getSavingCreditBalance(SavingBilanz savingBilanz, String countryCode) {
        if (savingBilanz.getSavingAmount() > 0) {
            return BVMicroUtils.formatCurrency(savingBilanz.getSavingAmount(), countryCode);
        }
        return "";
    }

    private String getTableList(CurrentBilanzList currentBilanzList, RuntimeSetting rt) {
        String tableHtml = "";
        for (CurrentBilanz bilanz : currentBilanzList.getCurrentBilanzList()) {
            String debitValue = "";
            String creditValue = "";
            if (new Double(bilanz.getCurrentAmount()) > 0) {
                creditValue = BVMicroUtils.formatCurrency(bilanz.getCurrentAmount(), rt.getCountryCode());
            } else if (bilanz.getCurrentAmount() < 0) {
                debitValue = BVMicroUtils.formatCurrency(bilanz.getCurrentAmount() * -1, rt.getCountryCode());
            }
            tableHtml = tableHtml + "<tr><td style=\"font:10px\">" + bilanz.getCreatedDate() + "</td>" +
                    "<td style=\"font:10px\">" + bilanz.getBranch() + "/" + BVMicroUtils.parseNotesCurrent(bilanz.getModeOfPayment(),rt.getCurrentAccount()) + "<br/>" + bilanz.getNotes() + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + debitValue + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + creditValue + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + bilanz.getCurrentBalance() + "</td>" +
                    "</tr>";
        }
        return tableHtml;
    }

    private String getTableList(SavingBilanzList savingBilanzList, RuntimeSetting rt) {
        String tableHtml = "";
        for (SavingBilanz bilanz : savingBilanzList.getSavingBilanzList()) {
            tableHtml = tableHtml + "<tr><td style=\"font:10px\">" + bilanz.getCreatedDate() + "</td>" +
                    "<td style=\"font:10px\">" + bilanz.getBranch() + "/" + BVMicroUtils.parseNotesCurrent( bilanz.getModeOfPayment(), rt.getCurrentAccount()) + "<br/>" + bilanz.getNotes() + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + getSavingDebitBalance(bilanz, rt.getCountryCode()) + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + getSavingCreditBalance(bilanz, rt.getCountryCode()) + "</td>" +
                    "<td style=\"font:10px\" align=\"right\">" + bilanz.getCurrentBalance() + "</td>" +
                    "</tr>";
        }
        return tableHtml;
    }

    public ByteArrayOutputStream generatePDF(String completeHtml, HttpServletResponse response) {
        ByteArrayOutputStream os = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            completeHtml = completeHtml.replaceAll("&", "&amp;");
            org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(completeHtml.getBytes(StandardCharsets.UTF_8)));
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(doc, null);
            renderer.layout();
            os = new ByteArrayOutputStream();
            renderer.createPDF(os, true);
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return os;
    }


    public String generateAmortizationPDF(Amortization amortizationHT, Amortization amortization, RuntimeSetting rt, User aUser) {
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        return "<html><body>" +
                "<table border=\"0\" width=\"100%\"><tr><td><img width=\"80px\" src=\"" + prefix + rt.getUnionLogo() + "\"/></td><td><h3>LOAN PAYMENT DETAILS - AMORTIZATION REPORT</h3></td></tr><tr><td colspan=\"2\">Customer Name: <b>" + amortization.getCustomer() + "</b><br/>Telephone: <b>" + amortization.getTelephone() + "</b></td></tr></table>" +
                "<table border=\"0\" width=\"100%\"><tr>" +

                "<td colspan=\"2\">Number: <br/><b> " + amortization.getLoanMonths() + "</b><br/>" +
                "Start Date:<br/><b> " + amortization.getStartDate() + "</b></td>" +
                "<td>Annual Rate HT: <br/><b> " + amortizationHT.getInterestRate() +
                "</b><br/>Annual Rate TTC: <br/><b> " + amortization.getInterestRate() + "</b></td>" +
                "<td>VAT Interest: <br/><b>" + rt.getVatPercent() + "</b><br/>" +
                "Total VAT Interest: <br/><b>" + BVMicroUtils.formatCurrency(amortization.getInterestHT(), rt.getCountryCode()) + "</b></td>" +
                "<td>Monthly Payment:<br/><b>" + amortization.getMonthlyPayment() + "</b><br/> Total Payments:<b><br/>" + amortization.getTotalInterestLoanAmount() + "</b></td>" +
                "<td>Total Interest:<br/>" +
                " <b>" + BVMicroUtils.formatCurrency(amortization.getTotalInterest(), rt.getCountryCode()) + "</b><br/>Loan Amount: <br/><b>" + BVMicroUtils.formatCurrency(amortization.getLoanAmount(), rt.getCountryCode()) + "</b></td>" +
                "</tr></table>" +
                "<table style= \"{tr:nth-child(even) = background-color: #c2ddf2;}\" width=\"100%\" border=\"1\"><tr><td><br/><b>Number</b></td><td><br/><b>Balance</b></td><td><br/><b>Principal</b></td><td><br/>" +
                "<b>Interest On TTC</b></td><td><br/><b>VAT On Interest</b></td><td><br/><b>Interest On HT</b></td>" +
                "<td><br/><b>Payment</b></td><td><br/><b>Due Date</b></td></tr>" +
                getAmortizationRow(amortization.getAmortizationRowEntryList(), rt) +
                "<tr><td colspan=\"8\" align=\"center\"><br/> Prepared by " + BVMicroUtils.getFullName(aUser) + " <br/> Date: " + BVMicroUtils.formatDate(new Date()) + "</td></tr>" +
                "<tr><td colspan=\"8\" align=\"center\"><br/> This loan offer is valid till " + BVMicroUtils.formatDateTime(LocalDateTime.now().plusDays(14)) + "</td></tr>" +
                "</table></body></html>";
    }

    private String getAmortizationRow(List<AmortizationRowEntry> amortizationRowEntryList, RuntimeSetting rt) {
        String row = "";
        for (AmortizationRowEntry amortizationRowEntry : amortizationRowEntryList) {
            row = row +
                    "<tr>" +
                    "<td>" + amortizationRowEntry.getMonthNumber() + "</td>" +
                    "<td>" + amortizationRowEntry.getLoanBalance() + "</td>" +
                    "<td>" + amortizationRowEntry.getPrincipal() + "</td>" +
                    "<td>" + BVMicroUtils.formatCurrency(amortizationRowEntry.getMonthlyInterest(), rt.getCountryCode()) + "</td>" +
                    "<td>" + BVMicroUtils.formatCurrency(amortizationRowEntry.getVATOnInterest(), rt.getCountryCode()) + "</td>" +
                    "<td>" + BVMicroUtils.formatCurrency(amortizationRowEntry.getInterestOnHT(), rt.getCountryCode()) + "</td>" +
                    "<td>" + amortizationRowEntry.getPayment() + "</td>" +
                    "<td>" + amortizationRowEntry.getDate() + "</td>" +
                    "</tr>";
        }
        return row;
    }

    public String generateShortOfMinimum(List<SavingAccount> savingAccounts, RuntimeSetting rt, User user) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        Double total = 0.0;
        for (SavingAccount savingAccount : savingAccounts) {
            total += savingAccount.getAccountMinBalance() - savingAccount.getAccountBalance();
        }
        String shortOfMinHtml = "<html><head><style>" +
                "* { font-family: Arial, Helvetica, sans-serif; }" +
                "table { border:none; }" +
                ".address-section { padding-top:20px; padding-bottom: 20px; }" +
                ".address-section h2 { font-size: 20px; font-weight: bold; margin: 0px; padding:0px; color:#000000; line-height: 30px; }" +
                ".address-section p { font-size: 16px; font-weight: normal; margin: 0px; padding:0px; color: #000000; line-height: 22px; }" +
                ".invoice-title h2 { font-size: 30px; font-weight: bold; margin: 0px; padding:0px; color: #000000; line-height: 24px; }" +
                ".invoice-details { background: " + rt.getThemeColor2() + "; border-bottom:5px solid #333333; padding-top:5px; padding-bottom:5px; padding-left: 10px; padding-right:10px; border-radius: 10px 10px 0 0; -moz-border-radius: 10px 10px 0 0; -webkit-border-radius: 10px 10px 0 0; }" +
                ".created-by { padding-bottom: 10px; }" +
                ".created-by span { font-size: 16px; color:#000000; }" +
                ".invoice-snapshot h4 { line-height:22px; font-size: 18px; font-weight: bold; color: #ffffff; margin: 0px; padding: 0px; }" +
                ".invoice-snapshot h4 label { font-size: 14px; font-weight: normal; color: #ffffff; margin: 0px; padding: 0px; }" +
                ".invoice-item-table { border-collapse: collapse; }" +
                ".invoice-item-table th { border-bottom: 2px solid #333333; padding-top: 10px; padding-bottom:10px; } " +
                ".invoice-item-table td { border-bottom: 2px solid #cdcdcd; padding-top: 10px; padding-bottom: 10px; } " +
                ".invoice-item-table td.last-row { border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom:15px; }" +
                ".invoice-item-table td.invoice-total { color: " + rt.getThemeColor() + "; border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom: 15px; }" +
                ".invoice-total h4 { font-size: 22px; font-weight: bold; margin:0px; padding:0px; }" +
                "</style>" +
                "</head><body>" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\">" +
                "           <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <h2>" + rt.getBusinessName() + "</h2>" +
                "           <p>" + rt.getSlogan() + "</p>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\" class=\"invoice-title\">" +
                "           <br/>" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <h2>" + user.getGender() + ". " + user.getFirstName() + " " + user.getLastName() + "</h2>" +
                "           <p>" + user.getBranch().getEmail() + "<br/>" + user.getBranch().getStreet() + "<br/>" + user.getBranch().getCity() + ", " + user.getBranch().getCountry() + "</p>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"created-by\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "           <span>Users who do not meet the minimum balance requirements.</span>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"invoice-details\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label>Date:</label><br/>" + dateFormat.format(date) + "</h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label></label><br/></h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label></label><br/></h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"right\">" +
                "           <h4><label>Branch Name:</label><br/>" + user.getBranch().getName() + "</h4>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "           <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"invoice-item-table\">" +
                "            <tr>" +
                "                <th>Name</th>" +
                "                <th align=\"right\">COBAC Code</th>" +
                "                <th align=\"right\">Balance Required</th>" +
                "                <th align=\"right\">Account Balance</th>" +
                "                <th align=\"right\">Amount Due </th>" +
                "            </tr>" + getShortOfMinTable(savingAccounts, rt) +
                "            <tr>" +
                "                <td colspan=\"3\" class=\"last-row\"><i>www.balaanz.com - user: " + user.getUserName() + " pwd: " + user.getPassword().substring(0, 2) + "**** </i><br/><br/><br/><br/>" + rt.getInvoiceFooter() + "</td>" +
                "                <td class=\"invoice-total\" colspan=\"3\" align=\"right\">" +
                "                   <h3>Total: " + BVMicroUtils.formatCurrency(total, rt.getCountryCode()) + " " + rt.getCurrency() + "</h3>" +
                "                </td>" +
                "            </tr>" +
                "           </table>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "</body></html>";
        return shortOfMinHtml;
    }

    public String generateTrialBalance(TrialBalanceBilanz trialBalanceBilanz, RuntimeSetting rt) {

        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        return "<html><head><style>" +
                "* { font-family: Arial, Helvetica, sans-serif; }" +
                "table { border:none; }" +
                ".address-section { padding-top:20px; padding-bottom: 20px; }" +
                ".address-section h2 { font-size: 20px; font-weight: bold; margin: 0px; padding:0px; color:#000000; line-height: 30px; }" +
                ".address-section p { font-size: 16px; font-weight: normal; margin: 0px; padding:0px; color: #000000; line-height: 22px; }" +
                ".address-section span { color:#ffffff }" +
                ".invoice-title h2 { font-size: 24px; font-weight: bold; margin: 0px; padding:0px; color: #ffffff; line-height: 24px; }" +
                ".invoice-details { background: " + rt.getThemeColor2() + "; border-bottom:5px solid #333333; padding-top:5px; padding-bottom:5px; padding-left: 10px; padding-right:10px; border-radius: 10px 10px 0 0; -moz-border-radius: 10px 10px 0 0; -webkit-border-radius: 10px 10px 0 0; }" +
                ".created-by { padding-bottom: 10px; }" +
                ".created-by span { font-size: 16px; color:#000000; }" +
                ".invoice-snapshot h4 { line-height:22px; font-size: 18px; font-weight: bold; color: #000000; margin: 0px; padding: 0px; }" +
                ".invoice-snapshot h4 label { font-size: 14px; font-weight: normal; color: #333333; margin: 0px; padding: 0px; }" +
                ".invoice-item-table { border-collapse: collapse; }" +
                ".invoice-item-table th { border-bottom: 2px solid #333333; padding-top: 10px; padding-bottom:10px; } " +
                ".invoice-item-table td { border-bottom: 2px solid #cdcdcd; padding-top: 10px; padding-bottom: 10px; } " +
                ".invoice-item-table td.last-row { border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom:15px; }" +
                ".invoice-item-table tr:last-child td.debit-col { border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom: 15px; }" +
                ".invoice-item-table tr:last-child td.credit-col { border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom: 15px; }" +
                ".debit-col { padding-right: 10px; min-width: 100px; padding-left: 10px;} " +
                ".credit-col { padding-left: 10px; min-width: 100px; } " +
                ".debit-col h4 { font-size: 22px; color: " + rt.getThemeColor() + "; font-weight: bold; margin:0px; padding:0px; }" +
                ".credit-col h4 { font-size: 22px; color: " + rt.getThemeColor() + "; font-weight: bold; margin:0px; padding:0px; }" +
                "</style>" +
                "</head><body>" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\">" +
                "           <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <h2>" + rt.getBusinessName() + "</h2>" +
                "           <p>" + rt.getSlogan() + "</p>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"invoice-details\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\" class=\"invoice-title\">" +
                "           <h2>INCOME EXPENSE Report</h2><br/>Trial Balance" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <span>" + trialBalanceBilanz.getStartDate() + " - " + trialBalanceBilanz.getEndDate() + "</span>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"transaction-container\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "           <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"invoice-item-table\">" +
                "            <tr>" +
                "                <th valign=\"middle\" align=\"left\">Description</th>" +
                "                <th valign=\"middle\" class=\"debit-col\" align=\"right\">Debit<br/>" +
                "                  <h4>" + BVMicroUtils.formatCurrency(trialBalanceBilanz.getCreditTotal(), rt.getCountryCode()) + "</h4> " +
                "                </th>" +
                "                <th valign=\"middle\" class=\"credit-col\" align=\"right\">Credit<br/>" +
                "                  <h4>" + BVMicroUtils.formatCurrency(trialBalanceBilanz.getDebitTotal(), rt.getCountryCode()) + "</h4> " +
                "                </th>" +
                "            </tr>" + getTrialBalanceRow(trialBalanceBilanz, rt) +
                "            <tr>" +
                "                <td class=\"last-row\"></td>" +
                "                <td class=\"debit-col\" align=\"right\">" +
                "                   <h4>" + BVMicroUtils.formatCurrency(trialBalanceBilanz.getCreditTotal(), rt.getCountryCode()) + "</h4>" +
                "                </td>" +
                "                <td class=\"credit-col\" align=\"right\">" +
                "                   <h4>" + BVMicroUtils.formatCurrency(trialBalanceBilanz.getDebitTotal(), rt.getCountryCode()) + "</h4>" +
                "                </td>" +
                "            </tr>" +
                "           </table>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "</body></html>";

    }


    private String getTrialBalanceRow(TrialBalanceBilanz trialBalanceBilanz, RuntimeSetting rt) {
        String row = "";

        for (TrialBalanceWeb aTrialBalanceWeb : trialBalanceBilanz.getTrialBalanceWeb()) {
            double debitTotal = 0;
            double creditTotal = 0;

            if (aTrialBalanceWeb.getCreditBalance().equalsIgnoreCase("false")) {
                if (aTrialBalanceWeb.getTotalDifference() > 0) {
                    debitTotal = aTrialBalanceWeb.getTotalDifference();
                } else if (aTrialBalanceWeb.getTotalDifference() < 0) {
                    debitTotal = aTrialBalanceWeb.getTotalDifference() * -1;
                }
            }
            if (aTrialBalanceWeb.getCreditBalance().equalsIgnoreCase("true")) {
                if (aTrialBalanceWeb.getTotalDifference() < 0) {
                    creditTotal = aTrialBalanceWeb.getTotalDifference() * -1;
                } else if (aTrialBalanceWeb.getTotalDifference() > 0) {
                    creditTotal = aTrialBalanceWeb.getTotalDifference();
                }
            }
            String debit = debitTotal == 0.0 ? "" : BVMicroUtils.formatCurrency(debitTotal, rt.getCountryCode());
            String credit = creditTotal == 0.0 ? "" : BVMicroUtils.formatCurrency(creditTotal, rt.getCountryCode());
            row = row +
                    "<tr>" +
                    "<td width=\"70%\">" + aTrialBalanceWeb.getName() + "</td>" +
                    "<td class=\"debit-col\" width=\"20%\" align=\"right\">" + debit + "</td>" +
                    "<td class=\"credit-col\" width=\"20%\" align=\"right\">" + credit + "</td>" +
                    "</tr>";
        }
        return row;
    }


    public String generatePDFInvoice(Invoice invoice, RuntimeSetting rt, User customer) throws IOException {
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        User byUserNameAndOrgId = userService.findByUserNameAndOrgId(invoice.getUsername(), invoice.getOrgId());
        String invoiceHtml = "<html><head><style>" +
                "* { font-family: Arial, Helvetica, sans-serif; }" +
                "table { border:none; }" +
                ".address-section { padding-top:20px; padding-bottom: 20px; }" +
                ".address-section h2 { font-size: 20px; font-weight: bold; margin: 0px; padding:0px; color:#000000; line-height: 30px; }" +
                ".address-section p { font-size: 16px; font-weight: normal; margin: 0px; padding:0px; color: #000000; line-height: 22px; }" +
                ".invoice-title h2 { font-size: 30px; font-weight: bold; margin: 0px; padding:0px; color: #000000; line-height: 24px; }" +
                ".invoice-details { background: " + rt.getThemeColor2() + "; border-bottom:5px solid #333333; padding-top:5px; padding-bottom:5px; padding-left: 10px; padding-right:10px; border-radius: 10px 10px 0 0; -moz-border-radius: 10px 10px 0 0; -webkit-border-radius: 10px 10px 0 0; }" +
                ".created-by { padding-bottom: 10px; }" +
                ".created-by span { font-size: 16px; color:#000000; }" +
                ".invoice-snapshot h4 { line-height:22px; font-size: 18px; font-weight: bold; color: #ffffff; margin: 0px; padding: 0px; }" +
                ".invoice-snapshot h4 label { font-size: 14px; font-weight: normal; color: #ffffff; margin: 0px; padding: 0px; }" +
                ".invoice-item-table { border-collapse: collapse; }" +
                ".invoice-item-table th { border-bottom: 2px solid #333333; padding-top: 10px; padding-bottom:10px; } " +
                ".invoice-item-table td { border-bottom: 2px solid #cdcdcd; padding-top: 10px; padding-bottom: 10px; } " +
                ".invoice-item-table td.last-row { border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom:15px; }" +
                ".invoice-item-table td.invoice-total { color: " + rt.getThemeColor() + "; border-bottom: none; border-top:2px solid #333333; padding-top:15px; padding-bottom: 15px; }" +
                ".invoice-total h4 { font-size: 22px; font-weight: bold; margin:0px; padding:0px; }" +
                "</style>" +
                "</head><body>" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\">" +
                "           <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <h2>" + rt.getBusinessName() + "</h2>" +
                "           <p>" + rt.getSlogan() + "</p>" +
                "           <p>Tel: " + rt.getTelephone() + "</p>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td valign=\"middle\" align=\"left\" class=\"invoice-title\">" +
                "           <h2>" + invoice.getInvoiceStatus().name() + "</h2>" +
                "       </td>" +
                "       <td valign=\"middle\" align=\"right\" class=\"address-section\">" +
                "           <h2>" + byUserNameAndOrgId.getGender() + ". " + byUserNameAndOrgId.getFirstName() + " " + byUserNameAndOrgId.getLastName() + "</h2>" +
                "           <p>" + byUserNameAndOrgId.getEmail() + "<br/> Tel: " + byUserNameAndOrgId.getTelephone1() + "<br/>" + byUserNameAndOrgId.getAddress() + "<br/>Date: " + BVMicroUtils.formatDateTime(LocalDateTime.now()) + "</p>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"created-by\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "           <span>Created By: " + BVMicroUtils.getFullName(customer) + "</span>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td class=\"invoice-details\" colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "<table border=\"0\" width=\"100%\">" +
                "   <tr>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label>" + invoice.getInvoiceStatus().name() + ":</label><br/>#" + invoice.getInvoiceNumber() + "</h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label>Invoice Created:</label><br/>" + BVMicroUtils.formatDate(invoice.getCreatedDate()) + "</h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"left\">" +
                "           <h4><label>Due Date:</label><br/>" + invoice.getDueDate().substring(0, 10) + "</h4>" +
                "       </td>" +
                "       <td class=\"invoice-snapshot\" align=\"right\">" +
                "           <h4><label>Branch Name:</label><br/>" + invoice.getUser().getBranch().getName() + "</h4>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "       </td>" +
                "   </tr>" +
                "   <tr>" +
                "       <td colspan=\"2\" valign=\"middle\" align=\"left\">" +
                "           <table id=\"transactions\" border=\"0\" width=\"100%\" class=\"invoice-item-table\">" +
                "            <tr>" +
                "                <th>Description</th>" +
                "                <th align=\"right\">Quantity</th>" +
                "                <th align=\"right\">Unit Price</th>" +
                "                <th align=\"right\">Amount</th>" +
                "            </tr>" + getTableList(invoice, rt) +
                "            <tr>" +
                "                <td class=\"invoice-total\" colspan=\"4\" align=\"right\">" +
                "                   <h4>Total: " + BVMicroUtils.formatCurrency(invoice.getTotalSum(), rt.getCountryCode()) + " " + rt.getCurrency() + "</h4><br/><br/>" + rt.getInvoiceFooter() +
                "                <br/></td></tr>" +
                "            <tr>" +
                "                <td class=\"last-row\" colspan=\"4\">Make Credit Card Payments here <a href=\"www.balaanz.com/pay/" + rt.getBid() + "\">www.balaanz.com/pay/" + rt.getBid() + "</a></td>" +
                "            </tr>" +
                "           </table>" +
                "       </td>" +
                "   </tr>" +
                "</table>" +
                "</body></html>";
        return invoiceHtml;
    }

    public String generatePOSReceiptPDF(PosReceiptPdf posReceiptPdf) throws IOException {
        return constructPOSReceiptHtml(posReceiptPdf);
    }

    private String constructPOSReceiptHtml(PosReceiptPdf posReceiptPdf) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer
                .append(prepareLogoSlogan(posReceiptPdf.getLogoPath(), posReceiptPdf.getSlogan()))
                .append(prepareReceiptUserDetails(posReceiptPdf.getCartUserDetails()))
                .append(prepareReceiptTable(posReceiptPdf));
        String head = format(HEAD, STYLES);
        String body = format(BODY, format(FINAL_TABLE, buffer));
        StringBuilder finalBuffer = new StringBuilder();
        finalBuffer.append(head).append(body);
        return format(HTML, finalBuffer);
    }

    private String prepareLogoSlogan(String logoPath, String slogan) throws IOException {
        byte[] imageBytes = getLogoFilePath(Paths.get(logoPath));
        String baseImage = encodeImageToBase64(imageBytes);
        String image = format(LOGO_IMG, logoPath);
        String td = format(LOGO_TABLE_SLOGAN, image);
        String tr = format(TABLE_TR, td);
        String table = format(LOGO_TABLE, tr.concat(format(LOGO_SLOGAN, slogan)));
        String dataOne = format(TBALE_LOGO_TD, table);
        return format(TABLE_TR, dataOne);
    }

    private byte[] getLogoFilePath(Path path) throws IOException {
        if (StringUtils.isNotEmpty(path.toString())) {
            return Files.readAllBytes(path);
        }
        return null;
    }

    private static String encodeImageToBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private String prepareReceiptUserDetails(CartUserDetails cartUserDetails) {
        StringBuilder buffer = new StringBuilder();
        buffer
                .append(prepareReceiptUserDetailsFirst(cartUserDetails))
                .append("<td valign=\"top\"></td>")
                .append(prepareReceiptUserDetailsSecond(cartUserDetails));
        return format(TABLE_TR, buffer);
    }

    private String prepareReceiptUserDetailsFirst(CartUserDetails cartUserDetails) {
        String rowName = format(ROW, "Name:", cartUserDetails.getName());
        String rowAddress = format(ROW, "Address:", cartUserDetails.getAddress());
        String rowContact = format(ROW, "Contact:", cartUserDetails.getContactNo());
        StringBuilder buffer = new StringBuilder();
        buffer.append(rowName).append(rowAddress).append(rowContact);
        String receiptDataList = format(TABLE_RECEIPT_DATA_LIST, buffer);
        return format(TD_VALIGN, receiptDataList);
    }

    private String prepareReceiptUserDetailsSecond(CartUserDetails cartUserDetails) {
        String rowInvoice = format(ROW, "Invoice no:", cartUserDetails.getInvoiceNo());
        String rowDateOfInvoice = format(ROW, "Date:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        String rowCompany = format(ROW, "Company:", cartUserDetails.getCompany());
        StringBuilder buffer = new StringBuilder();
        buffer.append(rowInvoice).append(rowDateOfInvoice).append(rowCompany);
        String receiptDataList = format(TABLE_RECEIPT_DATA_LIST, buffer);
        return format(TD_VALIGN, receiptDataList);
    }

    private String prepareReceiptTable(PosReceiptPdf posReceiptPdf) {
        String buffer = prepareReceiptTableData(posReceiptPdf);
        String tableAllCartDetails = format(LOGO_TABLE, buffer);
        String tableReceiptAllCartDetails = format(RECEIPT_TABLE_TD, tableAllCartDetails);
        return format(TABLE_TR, tableReceiptAllCartDetails);
    }

    private String prepareReceiptTableData(PosReceiptPdf posReceiptPdf) {
        String cartItemsTrs = posReceiptPdf.getCartDetails()
                .stream()
                .map(item -> format(RECEIPT_TABLE_DATA,
                        item.getId(),
                        item.getProductName(),
                        BVMicroUtils.formatCurrency(item.getPrice()),
                        item.getQuantity(),
                        item.getTax(),
                        BVMicroUtils.formatCurrency(item.getTotal())))
                .map(td -> format(TABLE_TR, td))
                .reduce("", (str1, str2) -> str1.concat("").concat(str2));
        String rowSubTotal = format(RECEIPT_TABLE_TOTALS, "Sub Total:", BVMicroUtils.formatCurrency(posReceiptPdf.getTotalAmount()) + " " + CURRENCY);
        String rowTotalTax = format(RECEIPT_TABLE_TOTALS, "Tax:", BVMicroUtils.formatCurrency(posReceiptPdf.getTotalTax()));
        double total = posReceiptPdf.getTotalTax() + posReceiptPdf.getTotalAmount();
        String rowTotal = format(RECEIPT_TABLE_TOTALS, "Total", BVMicroUtils.formatCurrency(total) + " " + CURRENCY);
        StringBuilder buffer = new StringBuilder();
        buffer
                .append(RECEIPT_TABLE_HEADERS)
                .append(cartItemsTrs)
                .append(rowSubTotal)
                .append(rowTotalTax)
                .append(rowTotal);
        return buffer.toString();
    }

    public String cumulativeAccountsSum(List<ReportService.Cumulatives> cumulatives, RuntimeSetting rt){
        String prefix = rt.getImagePrefix() == null ? "" : rt.getImagePrefix();
        String country = null == rt.getCountryCode() ? "cm" : rt.getCountryCode();
        String businessName = null == rt.getBusinessName() ? "" : rt.getBusinessName();
        String slogan = null == rt.getSlogan() ? "" : rt.getSlogan();
        return "<html>\n" +
          "\n" +
          "<head>\n" +
          "    <style>\n" +
          "        * {\n" +
          "            font-family: Arial, Helvetica, sans-serif;\n" +
          "        }\n" +
          "\n" +
          "        body {\n" +
          "            margin: 0px;\n" +
          "            padding: 0px;\n" +
          "        }\n" +
          "\n" +
          "        table {\n" +
          "            width: 100%;\n" +
          "            border: none;\n" +
          "        }\n" +
          "\n" +
          "        h6 {\n" +
          "            font-size: 18px;\n" +
          "            font-weight: bold;\n" +
          "            margin: 0px;\n" +
          "            width: 100%;\n" +
          "        }\n" +
          "\n" +
          "        h2 {\n" +
          "            font-size: 22px;\n" +
          "            font-weight: bold;\n" +
          "            margin: 0px;\n" +
          "            width: 100%;\n" +
          "        }\n" +
          "\n" +
          "        h4 {\n" +
          "            font-size: 18px;\n" +
          "            font-weight: bold;\n" +
          "            margin: 0px;\n" +
          "            width: 100%;\n" +
          "        }\n" +
          "\n" +
          "        p {\n" +
          "            font-size: 14px;\n" +
          "            font-weight: normal;\n" +
          "            margin: 0px;\n" +
          "            width: 100%;\n" +
          "        }\n" +
          "\n" +
          "        .logo-section {\n" +
          "            padding-bottom: 15px;\n" +
          "        }\n" +
          "\n" +
          "        .address-section h2 {\n" +
          "            margin-bottom: 7px;\n" +
          "        }\n" +
          "\n" +
          "        .statement-blocks {\n" +
          "            border-top: 2px solid #000000;\n" +
          "            border-bottom: 2px solid #000000;\n" +
          "            padding: 10px 0px;\n" +
          "        }\n" +
          "\n" +
          "        .statement-blocks table tr td {\n" +
          "            padding-top: 5px;\n" +
          "            padding-bottom: 5px;\n" +
          "        }\n" +
          "\n" +
          "        .statement-blocks label {\n" +
          "            font-size: 12px;\n" +
          "            color: #999999;\n" +
          "            margin: 0px;\n" +
          "        }\n" +
          "\n" +
          "        .statement-blocks p {\n" +
          "            font-size: 14px;\n" +
          "            color: #000000;\n" +
          "            margin: 0px;\n" +
          "            padding-top: 3px;\n" +
          "        }\n" +
          "\n" +
          "        .statement-blocks h6 {\n" +
          "            padding-top: 3px;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list {\n" +
          "            padding-top: 20px;\n" +
          ".overflow-text { font-size: 0.9em;}"+
          "                     }\n" +
          "\n" +
          "        .pdf-list table,\n" +
          "        .pdf-list {\n" +
          "            border: none;\n" +
          "            border-collapse: collapse;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list td,\n" +
          "        .pdf-list th {\n" +
          "            padding: 10px 10px;\n" +
          "            font-size: 12px;\n" +
          "            font-weight: normal;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list th {\n" +
          "            background: green;\n" +
          "            color: #ffffff;\n" +
          "             width: 20%;"+
          "        }\n" +
          "\n" +
          "        .pdf-list th:first-child {\n" +
          "            border-radius: 5px 0 0 0;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list th:last-child {\n" +
          "            border-radius: 0 5px 0 0;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list td {\n" +
          "            color: #000000;\n" +
          "            border-bottom: 1px solid #cdcdcd;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list td.total {\n" +
          "            text-align: right;\n" +
          "            font-size: 18px;\n" +
          "            font-weight: bold;\n" +
          "        }\n" +
          "\n" +
          "        .pdf-list td.total label {\n" +
          "            font-weight: normal;\n" +
          "            overflow-wrap: break-word;"+
          "        }\n" +
          "\n" +
          "        .pdf-list td.wordWrap {\n" +
          "            word-wrap: break-word;\n" +
          "        }\n" +
          "        .statement-blocks { border-top: 2px solid #000000; border-bottom:  0px solid #000000; padding: 10px 0px; }" +
          "    </style>\n" +
          "</head>\n" +
          "\n" +
          "<body>\n" +
          "    <table>\n" +
          "            <tr>" +
          "                <td>" +
          "                    <table>" +
          "                        <tr>" +
          "                            <td class=\"logo-section\" width=\"50%\" align=\"left\" valign=\"top\">" +
          "                                <img width=\"125px\" src=\"" + prefix + rt.getUnionLogo() + "\"/>" +
          "                            </td>" +
          "                            <td class=\"address-section\" width=\"50%\" align=\"right\" valign=\"top\">" +
          "                                <h2>Financial Statement</h2>" +
          "                                <p>" + BVMicroUtils.formatDate(new Date(System.currentTimeMillis())) + "</p>" +
          "                            </td>" +
          "                        </tr>" +
          "                        <tr>" +
          "                            <td class=\"logo-section\" width=\"50%\" align=\"left\" valign=\"top\">" +
          "                                <h4>"+businessName+"</h4>"+
          "                            </td>" +
          "                            <td class=\"logo-section\" width=\"50%\" align=\"right\" valign=\"top\">" +
          "                                <h4>"+slogan+"</h4>"+
          "                            </td>" +
          "                        </tr>" +
          "                    </table>" +
          "                </td>" +
          "            </tr>" +
          "        <tr>" +
          "           <td class=\"statement-blocks\">" +
          "           </td>" +
          "        </tr>\n" +
          "        <tr>\n" +
          "            <td class=\"pdf-list\">\n" +
          "                <table>\n" +
          "                    <tr>\n" +
          "                        <th align=\"center\" width=\"9%\">S.no</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Member Id</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Daily Account</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Savings Account</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Current Account</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Share Account</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Loan</th>\n" +
          "                        <th align=\"right\" width=\"9%\">Total</th>\n" +
          "                    </tr>"+ getTableList(cumulatives, country) +"\n" +
          "                </table>\n" +
          "            </td>\n" +
          "        </tr>\n" +
          "    </table>\n" +
          "</body>\n" +
          "\n" +
          "</html>";
    }

    private String getTableList(List<ReportService.Cumulatives> cumulatives, String country) {
        String tableHtml = "";
        double totalDaily = 0d;
        double totalSavings = 0d;
        double totalCurrent = 0d;
        double totalShare = 0d;
        double totalLoan = 0d;
        int count = 1;
        for (ReportService.Cumulatives bilanz : cumulatives) {
            tableHtml =
              tableHtml +
                "<tr>" +
                  "<td style=\"font:10px\" align=\"center\">" + count + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + (bilanz.getId()) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getDaily_balance(), country) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getSaving_balance(), country) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getCurrent_balance(), country) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getShare_balance(), country) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getLoan_amount(), country) + "</td>" +
                  "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(bilanz.getTotal(), country) + "</td>" +
                "</tr>";
            totalDaily += bilanz.getDaily_balance();
            totalSavings += bilanz.getSaving_balance();
            totalCurrent += bilanz.getCurrent_balance();
            totalShare += bilanz.getShare_balance();
            totalLoan += bilanz.getLoan_amount();
            count++;
        }

        tableHtml =
          tableHtml +
            "<tr>" +
            "<td style=\"font:10px\">" + "<b>Total :</b>" + "</td>" +
            "<td style=\"font:10px\">" + " " + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency((totalDaily), country) + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(totalSavings, country) + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(totalCurrent, country) + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(totalShare, country) + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + BVMicroUtils.formatCurrency(totalLoan, country) + "</td>" +
            "<td style=\"font:10px\" align=\"right\">" + "" + "</td>" +
            "</tr>";
        return tableHtml;
    }

}


