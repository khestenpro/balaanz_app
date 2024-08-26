package com.bitsvalley.micro.webdomain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;


@Data
@NoArgsConstructor
public class MtnWallet {
    Long id;
    String organization;
    Long accountNumber;
    BigDecimal collectionBalance;
    BigDecimal disbursementBalance;
    String currency;
    ZonedDateTime lastModified;
}