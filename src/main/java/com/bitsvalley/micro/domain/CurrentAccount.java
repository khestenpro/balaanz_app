package com.bitsvalley.micro.domain;
import com.bitsvalley.micro.utils.AccountStatus;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Entity
@Table(name = "currentaccount")
public class CurrentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String createdBy;
    private String lastUpdatedBy;
    private Date createdDate;
    private Date lastUpdatedDate;
    private boolean accountMinBalanceLocked;
    private boolean defaultedPayment;
    private String branchCode;
    private AccountStatus accountStatus;
    private int minimumPayment;
    private int noOfCurrentActivity;
    private float interestRate;
    private String country;
    private String productCode;
    @Column(unique = true)
    private String accountNumber;

    private double accountBalance;
    private long orgId;

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    @ManyToOne
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    private AccountType accountType; //Leave

    @OneToMany(cascade = CascadeType.ALL)
    private List<CurrentAccountTransaction> currentAccountTransaction = new ArrayList<CurrentAccountTransaction>();

    private String notes;
    private double accountMinBalance;

    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDefaultedPayment() {
        return defaultedPayment;
    }

    public void setDefaultedPayment(boolean defaultedPayment) {
        this.defaultedPayment = defaultedPayment;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }



    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public int getMinimumPayment() {
        return minimumPayment;
    }

    public void setMinimumPayment(int minimumPayment) {
        this.minimumPayment = minimumPayment;
    }


    public double getAccountMinBalance() {
        return accountMinBalance;
    }

    public void setAccountMinBalance(double accountMinBalance) {
        this.accountMinBalance = accountMinBalance;
    }

    public boolean isAccountMinBalanceLocked() {
        return accountMinBalanceLocked;
    }

    public void setAccountMinBalanceLocked(boolean accountMinBalanceLocked) {
        this.accountMinBalanceLocked = accountMinBalanceLocked;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public float getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(float interestRate) {
        this.interestRate = interestRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }


    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountMinBalanceLocked = accountLocked;
    }

    public boolean getAccountLocked() {
        return accountMinBalanceLocked;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public int getNoOfCurrentActivity() {
        return noOfCurrentActivity;
    }

    public void setNoOfCurrentActivity(int noOfCurrentActivity) {
        this.noOfCurrentActivity = noOfCurrentActivity;
    }

    public List<CurrentAccountTransaction> getCurrentAccountTransaction() {
        return currentAccountTransaction;
    }

    public void setCurrentAccountTransaction(List<CurrentAccountTransaction> currentAccountTransaction) {
        this.currentAccountTransaction = currentAccountTransaction;
    }

}
