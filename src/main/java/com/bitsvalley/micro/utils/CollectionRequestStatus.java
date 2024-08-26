package com.bitsvalley.micro.utils;

import java.math.BigDecimal;

public class CollectionRequestStatus {

    private String requestId;
    private Long accountNumber;
    private BigDecimal amount;
    private BigDecimal transactionFee = BigDecimal.ZERO;
    private String currency;
    private Long senderPhoneNumber;
    private String senderMsisdnProvider;
    private String note;
    private String status;


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

    public Long getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(Long senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getSenderMsisdnProvider() {
        return senderMsisdnProvider;
    }

    public void setSenderMsisdnProvider(String senderMsisdnProvider) {
        this.senderMsisdnProvider = senderMsisdnProvider;
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