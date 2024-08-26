package com.bitsvalley.micro.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Entity
@Table(name = "currentAccountTransaction")
public class CurrentAccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private double accountBalance;
    private String createdBy;
    private LocalDateTime createdDate;
    private double currentAmount;
    private String notes;
    private String modeOfPayment;
    private int withdrawalDeposit;
    private int tenThousand;
    private int fiveThousand;
    private int twoThousand;
    private int oneThousand;
    private int fiveHundred;
    private int oneHundred;
    private int fifty;
    private int twentyFive;
    private int ten;
    private int five;
    private int one;
    private String accountOwner;
    private String representative;
    private long branch;
    private String branchCode;
    private String branchCountry;
    private String currentAmountInLetters;

    @Column(unique = true)
    private String reference;

    @ManyToOne
    private CurrentAccount currentAccount;

    private long orgId;

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(String accountOwner) {
        this.accountOwner = accountOwner;
    }

    public int getTenThousand() {
        return tenThousand;
    }

    public void setTenThousand(int tenThousand) {
        this.tenThousand = tenThousand;
    }

    public int getFiveThousand() {
        return fiveThousand;
    }

    public void setFiveThousand(int fiveThousand) {
        this.fiveThousand = fiveThousand;
    }

    public int getTwoThousand() {
        return twoThousand;
    }

    public void setTwoThousand(int twoThousand) {
        this.twoThousand = twoThousand;
    }

    public int getOneThousand() {
        return oneThousand;
    }

    public void setOneThousand(int oneThousand) {
        this.oneThousand = oneThousand;
    }

    public int getFiveHundred() {
        return fiveHundred;
    }

    public void setFiveHundred(int fiveHundred) {
        this.fiveHundred = fiveHundred;
    }

    public int getOneHundred() {
        return oneHundred;
    }

    public void setOneHundred(int oneHundred) {
        this.oneHundred = oneHundred;
    }

    public int getFifty() {
        return fifty;
    }

    public void setFifty(int fifty) {
        this.fifty = fifty;
    }

    public int getTwentyFive() {
        return twentyFive;
    }

    public void setTwentyFive(int twentyFive) {
        this.twentyFive = twentyFive;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public long getId() {
        return id;
    }

    public long getBranch() {
        return branch;
    }

    public void setBranch(long branch) {
        this.branch = branch;
    }




    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getModeOfPayment() {
        return modeOfPayment;
    }

    public void setModeOfPayment(String modeOfPayment) {
        this.modeOfPayment = modeOfPayment;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchCountry() {
        return branchCountry;
    }

    public void setBranchCountry(String branchCountry) {
        this.branchCountry = branchCountry;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getCurrentAmountInLetters() {
        return currentAmountInLetters;
    }

    public void setCurrentAmountInLetters(String currentAmountInLetters) {
        this.currentAmountInLetters = currentAmountInLetters;
    }

    public CurrentAccount getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(CurrentAccount currentAccount) {
        this.currentAccount = currentAccount;
    }

    public int getWithdrawalDeposit() {
        return withdrawalDeposit;
    }

    public void setWithdrawalDeposit(int withdrawalDeposit) {
        this.withdrawalDeposit = withdrawalDeposit;
    }

    public int getTen() {
        return ten;
    }

    public void setTen(int ten) {
        this.ten = ten;
    }

    public int getFive() {
        return five;
    }

    public void setFive(int five) {
        this.five = five;
    }

    public int getOne() {
        return one;
    }

    public void setOne(int one) {
        this.one = one;
    }
}
