package com.bitsvalley.micro.webdomain;

import java.math.BigDecimal;

public class DisbursementRequest {

    private BigDecimal amount;
    private BigDecimal transactionFee;
    private ChargeRequest.Currency currency;
    private Long recipientPhoneNumber;
    private String recipientMsisdnProvider;
    private String note;


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ChargeRequest.Currency getCurrency() {
        return currency;
    }

    public void setCurrency(ChargeRequest.Currency currency) {
        this.currency = currency;
    }

    public Long getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(Long recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getRecipientMsisdnProvider() {
        return recipientMsisdnProvider;
    }

    public void setRecipientMsisdnProvider(String recipientMsisdnProvider) {
        this.recipientMsisdnProvider = recipientMsisdnProvider;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getTransactionFee() {
        return transactionFee;
    }

    public void setTransactionFee(BigDecimal transactionFee) {
        this.transactionFee = transactionFee;
    }
}