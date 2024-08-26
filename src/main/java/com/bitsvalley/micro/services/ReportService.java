package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.model.*;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.LedgerCategory;
import com.bitsvalley.micro.utils.TicketStatus;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import com.bitsvalley.micro.webdomain.SavingReportDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.bitsvalley.micro.utils.BVMicroUtils.objectMapper;

@Service
@Slf4j
public class ReportService {

    public static final String YYYY_MM_DD_T_HH_MM = "yyyy-MM-dd'T'HH:mm";
    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountTypeRepository accountTypeRepository;

    @Autowired
    UserRoleRepository userRoleRepository;

    @Autowired
    SavingAccountRepository savingAccountRepository;
    @Autowired
    LedgerAccountRepository ledgerAccountRepository;
    @Autowired
    IssueRepository issueRepository;
    @Autowired
    SavingAccountTransactionRepository savingAccountTransactionRepository;
    @Autowired
    PdfService pdfService;

    public SavingReportDTO savingReports( String loggedInUserName , String countryCode) {

        ArrayList<UserRole> customers = new ArrayList<UserRole>();
        UserRole roleByName = userRoleRepository.findByName(BVMicroUtils.ROLE_CUSTOMER);
        customers.add(roleByName);
        User user = userRepository.findByUserName(loggedInUserName);
        StringBuilder descriptionValues = new StringBuilder("[\"");
        int index = 0;
        StringBuilder dueValues = new StringBuilder("[");
        StringBuilder paidValues = new StringBuilder("[");

        double sumTotalPaid = 0;
        double sumTotalDue = 0;

        for (AccountType accountType: accountTypeRepository.findByOrgIdAndCategoryAndActiveTrue(user.getOrgId(),BVMicroUtils.SAVINGS)){
            double totalDue = 0.0;
            double totalPaid = 0.0;

            List<SavingAccount> savingAccounts = savingAccountRepository.findByOrgIdAndAccountType(user.getOrgId(), accountType);

            if (index > 0){
                descriptionValues.append("\",\"");
            }
            index++;
            descriptionValues.append(accountType.getDisplayName());
            for ( SavingAccount aSavingAccount: savingAccounts ) {

                double accountMinBalance = aSavingAccount.getAccountMinBalance();
                double accountBalance = aSavingAccount.getAccountBalance();

                if(accountBalance < accountMinBalance){
                    totalDue = totalDue + (accountMinBalance - accountBalance);
                    sumTotalDue = sumTotalDue +  (accountMinBalance - accountBalance);
                }
                totalPaid = totalPaid + accountBalance;
                sumTotalPaid = sumTotalPaid + accountBalance;

            }
            dueValues.append(totalDue).append(",");
            paidValues.append(totalPaid).append(",");
        }

        dueValues.deleteCharAt(dueValues.lastIndexOf(","));
        paidValues.deleteCharAt(paidValues.lastIndexOf(","));
        descriptionValues.append("\"],");
        dueValues.append("]");
        paidValues.append("]");

        SavingReportDTO savingReportDTO = new SavingReportDTO();
        savingReportDTO.setDescriptionValues(descriptionValues.toString());
        savingReportDTO.setPaidValues(paidValues.toString());
        savingReportDTO.setDueValues(dueValues.toString());
        savingReportDTO.setSumTotalPaid("\"Paid - " + BVMicroUtils.formatCurrency(sumTotalPaid, countryCode)+"\",");
        savingReportDTO.setSumTotalDue("\"Due - "+ BVMicroUtils.formatCurrency(sumTotalDue, countryCode)+"\",");

//        model.put("descriptionValues","[\"Web1\",\"App1\",\"App2\",\"CSe\"],");
//        model.put("paidValues","[13000,20000,17000,25000]");
//        model.put("dueValues","[33000,40000,35000,50000]");

        return savingReportDTO;
    }

    public ReportsResponseModel getActiveUsersReport(HttpServletRequest request){
        ReportsRequest reportsRequest = buildRequest(request);
        String ii = String.valueOf(request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG));
        reportsRequest.setOrgId(Integer.valueOf(ii));
        UserReportsModel activeUsers = fetchActiveUsers(reportsRequest);
        //UserReportsModel unlockedUsers = fetchUnlockedUsers(reportsRequest.getOrgId());
        Map<String, Object> userReportsModelMap = new HashMap<>();
        userReportsModelMap.put("Managers",activeUsers);
        //userReportsModelMap.put("Unlocked Users",unlockedUsers);
        userReportsModelMap.put("Customers", UserReportsModel.builder().attribute("customers")
          .value(fetchCustomers(reportsRequest)).build());
        //List<PrioritizedIssues> prioritizedIssues = fetchIssues(1, reportsRequest.getOrgId());
        //IssuesByDateStatusMap issuesByDateStatusMap = fetchIssuesByDateAndStatus(reportsRequest.getOrgId());
        Map<String, Integer> transactions = new HashMap<>();
        transactions.put("Savings", fetchSavingAccTransactions(reportsRequest));
        transactions.put("Loan", fetchLoanAccTransactions(reportsRequest));
        transactions.put("Current", fetchCurrentAccTransactions(reportsRequest));
        return ReportsResponseModel.builder().data(userReportsModelMap)
          .categorizedLedgerAccount(fetchLedgerAccounts(reportsRequest.getOrgId()))
          .reportsRequest(reportsRequest)
          .transactions(transactions)
          .build();
    }
    private ReportsRequest buildRequest(HttpServletRequest request){
        return validateDates(request);
    }
    private ReportsRequest validateDates(HttpServletRequest request){
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");
        LocalDateTime fromLocalDate;
        LocalDateTime toLocalDate;
        if(!StringUtils.isNotEmpty(toDate) && !StringUtils.isNotEmpty(fromDate)){
            toLocalDate = LocalDateTime.now().withNano(0);
            fromLocalDate = toLocalDate.withDayOfMonth(1).withHour(0).withMinute(0).withNano(0);
        }else{
            fromLocalDate = LocalDateTime.parse(fromDate).withNano(0);
            toLocalDate = LocalDateTime.parse(toDate).withNano(0);
        }
        return ReportsRequest.builder().fromDate(fromLocalDate).toDate(toLocalDate).build();
    }
    private void fetchUserReports(){
        userRepository.findUsers();
    }

    private UserReportsModel fetchActiveUsers(ReportsRequest reportsRequest) {
        Integer activeUsers = userRepository.findActiveUsers(reportsRequest.getFromDate(),
          reportsRequest.getToDate(), reportsRequest.getOrgId());
        return UserReportsModel.builder().attribute("ACTIVE_USERS").value(activeUsers).build();
    }

    private UserReportsModel fetchUnlockedUsers(Integer orgId){
        Integer unlockedUsers = userRepository.findUnlockedUsers(orgId);
        return UserReportsModel.builder().attribute("UNLOCKED_USERS").value(unlockedUsers).build();
    }
    private List<CategorizedLedgerAccount> fetchLedgerAccounts(Integer orgId){
        List<Object[]> ledgerAccountsByCategory = ledgerAccountRepository.findLedgerAccountsByCategory(orgId);
        return convertToCategorizedLedgerAccount(ledgerAccountsByCategory);
    }
    private Integer fetchCustomers(ReportsRequest reportsRequest){
        return userRepository.findCustomers(reportsRequest.getFromDate(),reportsRequest.getToDate(),
          reportsRequest.getOrgId());
    }

    private List<CategorizedLedgerAccount> convertToCategorizedLedgerAccount(List<Object[]> result){
        Map<String, String> ledgerCategories = LedgerCategory.fetchLedgerCategory();
        return result.stream()
          .filter(Objects::nonNull)
          .map(item -> CategorizedLedgerAccount.builder()
            .category(ledgerCategories.get(String.valueOf(item[0])))
            .totalCount(Integer.valueOf(String.valueOf(item[1])))
            .active(Integer.valueOf(String.valueOf(item[2])))
            .inactive(Integer.valueOf(String.valueOf(item[3]))).build())
          .collect(Collectors.toList());

    }
    private List<PrioritizedIssues> fetchIssues(Integer status, Integer orgId){
        List<Object[]> issuesByFilterting = issueRepository.findIssuesByFilterting(status, orgId);
        return convertToPrioritizedIssues(issuesByFilterting);
    }
    private List<PrioritizedIssues> convertToPrioritizedIssues(List<Object[]> result){
        return result.stream()
          .filter(Objects::nonNull)
          .map(item -> PrioritizedIssues.builder()
            .priority(Integer.valueOf(String.valueOf(item[0])))
            .totalCount(Integer.valueOf(String.valueOf(item[1])))
            .assigned(Integer.valueOf(String.valueOf(item[2])))
            .unassigned(Integer.valueOf(String.valueOf(item[3]))).build())
          .collect(Collectors.toList());

    }
    private IssuesByDateStatusMap fetchIssuesByDateAndStatus(Integer orgId){
        List<Object[]> issuesByDateAndStatus = issueRepository.findIssuesByDateAndStatus(orgId);
        List<StatusAndDateIssues> statusAndDateIssues = convertToStatusAndDateIssues(issuesByDateAndStatus);
        Map<LocalDate, Map<String, Integer>> cc = new LinkedHashMap<>();
        for(StatusAndDateIssues item : statusAndDateIssues){
            if(cc.containsKey(item.getDate())){
                Map<String, Integer> vv = cc.get(item.getDate());
                if(vv.containsKey(String.valueOf(TicketStatus.values()[item.getStatus()]))){
                    vv.put(String.valueOf(TicketStatus.values()[item.getStatus()]), vv.get(item.getStatus()) + item.getCount());
                }
                else {
                    vv.put(String.valueOf(TicketStatus.values()[item.getStatus()]), item.getCount());
                }
            }else {
                Map<String, Integer> mm = new LinkedHashMap<>();
                mm.put(String.valueOf(TicketStatus.values()[item.getStatus()]), item.getCount());
                cc.put(item.getDate(), mm);
            }
        }
        log.info("map : {}", cc);
        return IssuesByDateStatusMap.builder().issuesByDateAndStatus(cc).build();
    }
    private List<StatusAndDateIssues> convertToStatusAndDateIssues(List<Object[]> result){
        return result.stream()
          .filter(Objects::nonNull)
          .map(item -> StatusAndDateIssues.builder()
            .date(LocalDate.parse(String.valueOf(item[0])))
            .status(Integer.valueOf(String.valueOf(item[1])))
            .count(Integer.valueOf(String.valueOf(item[2])))
            .build())
          .collect(Collectors.toList());
    }

    private Integer fetchSavingAccTransactions(ReportsRequest reportsRequest){
        return savingAccountTransactionRepository.findSavingAccountTransactions(
          reportsRequest.getFromDate(), reportsRequest.getToDate(), reportsRequest.getOrgId());
    }
    private Integer fetchLoanAccTransactions(ReportsRequest reportsRequest){
        return savingAccountTransactionRepository.findLoanAccountTransactions(
          reportsRequest.getFromDate(), reportsRequest.getToDate(), reportsRequest.getOrgId());
    }
    private Integer fetchCurrentAccTransactions(ReportsRequest reportsRequest){
        return savingAccountTransactionRepository.findCurrentAccountTransactions(
          reportsRequest.getFromDate(), reportsRequest.getToDate(), reportsRequest.getOrgId());
    }

    public String allAccountsSum(long orgId, RuntimeSetting runtimeSetting){
        List<Object[]> allAccountsBalanceSum = accountTypeRepository.getAllAccountsBalanceSum(orgId);
      try {
          SimpleModule module = new SimpleModule();
          module.addDeserializer(Cumulatives.class, new DataRowDeserializer());
          objectMapper.registerModule(module);

          String allAccountsSum = objectMapper.writeValueAsString(allAccountsBalanceSum);

          List<Cumulatives> cumulatives = objectMapper.readValue(allAccountsSum, new TypeReference<List<Cumulatives>>() {
          });
          return pdfService.cumulativeAccountsSum(cumulatives, runtimeSetting);
      } catch (JsonProcessingException e) {
        log.error("failed to deserialize : ",e);
        throw new RuntimeException(e);
      }
    }
    @Getter
    @Setter
    @ToString
    class Cumulatives{
        private int id;
        private double daily_balance;
        private double saving_balance;
        private double current_balance;
        private double share_balance;
        private double loan_amount;
        private double total;
        @JsonCreator
        public Cumulatives(@JsonProperty("array") List<Object> array) {
            this.id = (int) array.get(0);
            this.daily_balance = (double) array.get(1);
            this.saving_balance = (double) array.get(2);
            this.current_balance = (double) array.get(3);
            this.share_balance = (double) array.get(4);
            this.loan_amount = (double) array.get(5);
            this.total = (double) array.get(6);
        }
    }
    class DataRowDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Cumulatives> {
        @Override
        public Cumulatives deserialize(com.fasterxml.jackson.core.JsonParser p,
                                       com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
            List<Object> values = p.readValueAs(new TypeReference<List<Object>>() {});
            return new Cumulatives(values);
        }
    }
}
