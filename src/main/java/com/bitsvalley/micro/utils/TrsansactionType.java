package com.bitsvalley.micro.utils;

public enum TrsansactionType {

    AVAILABLE("AVAILABLE"),
    COLLECTED("COLLECTED"),
    ON_HOLD("ON_HOLD"),
    PROCESSING("PROCESSING"),
    IN_TRANSIT("IN_TRANSIT");

    private final String transactionType;

    TrsansactionType(String type) {
        transactionType = type;
    }

}
