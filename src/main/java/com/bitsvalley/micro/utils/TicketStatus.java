package com.bitsvalley.micro.utils;

public enum TicketStatus {

    CREATED("CREATED"), IN_PROGRESS("IN_PROGRESS"), DONE("DONE"), SUSPENDED("SUSPENDED"), BLOCKED("BLOCKED"), IN_ACTIVE("IN_ACTIVE"), UNASSIGNED("UNASSIGNED"), REJECTED("REJECTED");

    private TicketStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    private final String displayValue;

}
