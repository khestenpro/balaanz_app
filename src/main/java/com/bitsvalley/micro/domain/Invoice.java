package com.bitsvalley.micro.domain;

import com.bitsvalley.micro.utils.InvoiceStatus;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Fru Chifen
 * 09.26.2021
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @ManyToOne
    private User user;

    @OneToMany(cascade = CascadeType.ALL)
    private List<InvoiceLineItemDetail> invoiceLineItemDetail = new ArrayList<InvoiceLineItemDetail>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String username;
    private String invoiceNumber;
    private String dueDate;
    private Date date;
    private InvoiceStatus invoiceStatus;
    private String notes;
    private double discount;
    private double totalSum;
    private String branchCode;
    private String createdBy;
    private Date createdDate;
    private long orgId;

    public List<InvoiceLineItemDetail> getInvoiceLineItemDetail() {
        return invoiceLineItemDetail;
    }

    public void setInvoiceLineItemDetail(List<InvoiceLineItemDetail> invoiceLineItemDetail) {
        this.invoiceLineItemDetail = invoiceLineItemDetail;
    }

    public long getId() {
        return id;
    }

    public InvoiceStatus getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(InvoiceStatus invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(double totalSum) {
        this.totalSum = totalSum;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getOrgId() {
        return orgId;
    }
}
