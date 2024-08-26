package com.bitsvalley.micro.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "receipt")
public class Receipt {


	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

    private String receiptNo;

    private Date lastModified;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

//	public SubPosUser getSubPosUser() {
//		return subPosUser;
//	}
//
//	public void setSubPosUser(SubPosUser subPosUser) {
//		this.subPosUser = subPosUser;
//	}

	public User getSignedBy() {
		return signedBy;
	}

	public void setSignedBy(User signedBy) {
		this.signedBy = signedBy;
	}

	@Basic(fetch=FetchType.LAZY)

    @ManyToOne
    private User user;

//	@ManyToOne
//    private SubPosUser subPosUser;

    private BigDecimal discount;

    private Boolean signed;

    private String cash;

    @ManyToOne
    private User signedBy;

    @Basic(fetch=FetchType.LAZY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "receipt")
    private Set<OrderItem> orderItems = new HashSet<OrderItem>();

    private String total;
    private BigDecimal totalBD;
    private String netto;
    private BigDecimal nettoBD;
    private String tax;
    private String street;
    
	private String businessName;
	private String telephone;
	private String city;
	private String zipCode;
    private BigDecimal taxIncluded;
    private String currencySymbol;
    private String allSubUsers;
    private String paid;
    private String paid_change;

    private boolean paymentReceived;
    private String uid;
    private String taxNr;

	public String getReceiptNo() {
		return receiptNo;
	}
	public void setReceiptNo(String receiptNo) {
		this.receiptNo = receiptNo;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}


	public BigDecimal getDiscount() {
		return discount;
	}
	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}
	public Boolean getSigned() {
		return signed;
	}
	public void setSigned(Boolean signed) {
		this.signed = signed;
	}
	public String getCash() {
		return cash;
	}
	public void setCash(String cash) {
		this.cash = cash;
	}

	public Set<OrderItem> getOrderItems() {
		return orderItems;
	}
	public void setOrderItems(Set<OrderItem> orderItems) {
		this.orderItems = orderItems;
	}
	public String getTotal() {
		return total;
	}
	public void setTotal(String total) {
		this.total = total;
	}
	public BigDecimal getTotalBD() {
		return totalBD;
	}
	public void setTotalBD(BigDecimal totalBD) {
		this.totalBD = totalBD;
	}
	public String getNetto() {
		return netto;
	}
	public void setNetto(String netto) {
		this.netto = netto;
	}
	public BigDecimal getNettoBD() {
		return nettoBD;
	}
	public void setNettoBD(BigDecimal nettoBD) {
		this.nettoBD = nettoBD;
	}
	public String getTax() {
		return tax;
	}
	public void setTax(String tax) {
		this.tax = tax;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getBusinessName() {
		return businessName;
	}
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}
	public String getTelephone() {
		return telephone;
	}
	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
	public BigDecimal getTaxIncluded() {
		return taxIncluded;
	}
	public void setTaxIncluded(BigDecimal taxIncluded) {
		this.taxIncluded = taxIncluded;
	}
	public String getCurrencySymbol() {
		return currencySymbol;
	}
	public void setCurrencySymbol(String currencySymbol) {
		this.currencySymbol = currencySymbol;
	}
	public String getAllSubUsers() {
		return allSubUsers;
	}
	public void setAllSubUsers(String allSubUsers) {
		this.allSubUsers = allSubUsers;
	}
	public String getPaid() {
		return paid;
	}
	public void setPaid(String paid) {
		this.paid = paid;
	}
	public String getPaid_change() {
		return paid_change;
	}
	public void setPaid_change(String paid_change) {
		this.paid_change = paid_change;
	}
	public boolean isPaymentReceived() {
		return paymentReceived;
	}
	public void setPaymentReceived(boolean paymentReceived) {
		this.paymentReceived = paymentReceived;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getTaxNr() {
		return taxNr;
	}
	public void setTaxNr(String taxNr) {
		this.taxNr = taxNr;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
