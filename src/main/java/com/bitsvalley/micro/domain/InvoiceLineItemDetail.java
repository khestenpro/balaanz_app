package com.bitsvalley.micro.domain;

import javax.persistence.*;

/**
 * @author Fru Chifen
 * 09.26.2021
 */
@Entity
@Table(name = "invoiceLineItemDetail")
public class InvoiceLineItemDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    private Invoice invoice;

    private int quantity;
    private double unitPrice;
    private double OriginalUnitPrice;
    private double total;
    private String description;

    public long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getOriginalUnitPrice() {
        return OriginalUnitPrice;
    }

    public void setOriginalUnitPrice(double originalUnitPrice) {
        OriginalUnitPrice = originalUnitPrice;
    }
}
