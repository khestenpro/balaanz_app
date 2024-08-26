package com.bitsvalley.micro.utils;

public enum PriorityStatus {

    LOW("LOW"), MEDIUM("MEDIUM"), HIGH("HIGH"), CRITICAL("CRITICAL");

    private PriorityStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    private final String displayValue;

}
