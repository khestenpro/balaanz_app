package com.bitsvalley.micro.utils;

import java.math.BigDecimal;

public class DisbursementRequestStatus {

    private String requestId;
    private Long accountNumber;
    private BigDecimal amount;
    private BigDecimal cashOutFee = BigDecimal.ZERO;
    private BigDecimal transactionFee = BigDecimal.ZERO;
    private String currency;
    private Long phoneNumber;
    private String msisdnProvider;
    private String note;
    private String status;
    private String transactionId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCashOutFee() {
        return cashOutFee;
    }

    public void setCashOutFee(BigDecimal cashOutFee) {
        this.cashOutFee = cashOutFee;
    }

    public BigDecimal getTransactionFee() {
        return transactionFee;
    }

    public void setTransactionFee(BigDecimal transactionFee) {
        this.transactionFee = transactionFee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMsisdnProvider() {
        return msisdnProvider;
    }

    public void setMsisdnProvider(String msisdnProvider) {
        this.msisdnProvider = msisdnProvider;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}