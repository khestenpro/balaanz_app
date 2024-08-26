package com.bitsvalley.micro.utils;

public enum InvoiceStatus {

    INVOICE("INVOICE"),OFFER("OFFER"), OPEN("OPEN"), PAID("PAID"), SUBMITTED("SUBMITTED"), PAST_DUE("PAST_DUE"), DUE("DUE"), OBSOLETE("OBSOLETE"), REJECTED("REJECTED");

    private InvoiceStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    private final String displayValue;

}
