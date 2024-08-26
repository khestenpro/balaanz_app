package com.bitsvalley.micro.domain;

import com.bitsvalley.micro.utils.OrderStatus;
import com.bitsvalley.micro.utils.OrderType;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "orderitem")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private Integer qty;

    private String itemNumber;

    private String name;

    @ManyToOne
    private User createdBy;

    @ManyToOne
    private Receipt receipt;

    @ManyToOne
    @JoinColumn(name = "shopping_cart_id")
    private ShoppingCart shoppingCart;

    @Basic(fetch=FetchType.LAZY)
    @ManyToOne
    private ShopProduct shopProduct;

    private double price;

    private double discountPrice;

    @Basic(fetch=FetchType.LAZY)

    @ManyToOne
    private User user;

    public ShopProduct getShopProduct() {
        return shopProduct;
    }

    public void setShopProduct(ShopProduct shopProduct) {
        this.shopProduct = shopProduct;
    }

    //    @Basic(fetch=FetchType.LAZY)
//    @ManyToOne
//    private SubPosUser subPosUser;

    private Date lastModified;

    private Date orderPlaced;

    private Date orderReady;

    private Date orderServed;

    @Enumerated
    private OrderType orderType;

    @Enumerated
    private OrderStatus orderStatus;

    private double discount;

    private String notes;

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getOrderPlaced() {
        return orderPlaced;
    }

    public void setOrderPlaced(Date orderPlaced) {
        this.orderPlaced = orderPlaced;
    }

    public Date getOrderReady() {
        return orderReady;
    }

    public void setOrderReady(Date orderReady) {
        this.orderReady = orderReady;
    }

    public Date getOrderServed() {
        return orderServed;
    }

    public void setOrderServed(Date orderServed) {
        this.orderServed = orderServed;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
    }
}
