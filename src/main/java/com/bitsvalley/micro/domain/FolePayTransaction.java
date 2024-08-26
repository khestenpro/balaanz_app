package com.bitsvalley.micro.domain;

import javax.persistence.*;
import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Fru Chifen
 * 13.05.2024
 */
@Entity
@Table(name = "folepayTransaction")
public class FolePayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String requestId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal cashOutFee = BigDecimal.ZERO;
    private BigDecimal transactionFee = BigDecimal.ZERO;
    private String currency;
    private Long phoneNumber;
    private String msisdnProvider;
    private String note;
    private String status;
    private String transactionId;
    private Date date;
    private long userId;
    private long orgId;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

}