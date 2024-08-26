package com.bitsvalley.micro.webdomain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionRequest {

    private BigDecimal amount;
    private BigDecimal transactionFee;
    private ChargeRequest.Currency currency;
    private Long senderPhoneNumber;
    private String senderMsisdnProvider;
    private String note;

}