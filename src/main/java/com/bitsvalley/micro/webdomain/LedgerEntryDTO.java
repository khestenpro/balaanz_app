package com.bitsvalley.micro.webdomain;

import com.bitsvalley.micro.domain.SavingAccount;

import java.util.ArrayList;
import java.util.List;

public class LedgerEntryDTO {

    private long orgId;
    private String accountNumber;
    private long originLedgerAccount;
    private long destinationLedgerAccount;
    private double ledgerAmount;
    private String creditOrDebit;
    private String recordDate;
    private String notes;
    private boolean fromAccountToLedger;
    private List<String> paramValueString = new ArrayList<String>();
    private List<SavingAccount> savingAccounts;

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
    private int two;
    private int one;

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

    public int getTwo() {
        return two;
    }

    public void setTwo(int two) {
        this.two = two;
    }

    public int getOne() {
        return one;
    }

    public void setOne(int one) {
        this.one = one;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getOriginLedgerAccount() {
        return originLedgerAccount;
    }

    public void setOriginLedgerAccount(long originLedgerAccount) {
        this.originLedgerAccount = originLedgerAccount;
    }

    public Long getDestinationLedgerAccount() {
        return destinationLedgerAccount;
    }

    public void setDestinationLedgerAccount(Long destinationLedgerAccount) {
        this.destinationLedgerAccount = destinationLedgerAccount;
    }

    public double getLedgerAmount() {
        return ledgerAmount;
    }

    public void setLedgerAmount(double ledgerAmount) {
        this.ledgerAmount = ledgerAmount;
    }

    public String getCreditOrDebit() {
        return creditOrDebit;
    }

    public void setCreditOrDebit(String creditOrDebit) {
        this.creditOrDebit = creditOrDebit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SavingAccount> getSavingAccounts() {
        return savingAccounts;
    }

    public void setSavingAccounts(List<SavingAccount> savingAccounts) {
        this.savingAccounts = savingAccounts;
    }

    public List<String> getParamValueString() {
        return paramValueString;
    }

    public void setParamValueString(List<String> paramValueString) {
        this.paramValueString = paramValueString;
    }

    public String getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
    }

    public boolean isFromAccountToLedger() {
        return fromAccountToLedger;
    }

    public void setFromAccountToLedger(boolean fromAccountToLedger) {
        this.fromAccountToLedger = fromAccountToLedger;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
