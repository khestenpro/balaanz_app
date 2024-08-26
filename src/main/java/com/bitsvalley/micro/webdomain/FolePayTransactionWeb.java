package com.bitsvalley.micro.webdomain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
public class FolePayTransactionWeb {

    private Long accountNumber;
    private String familyName;
    private String givenNames;
    private Long vendorAccount;
    private String vendorName;
    private Long phoneNumber;
    private String msisdnProvider;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String transactionId;
    private ZonedDateTime lastModified;

}
