package com.bitsvalley.micro.webdomain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
public class OrangeWallet {
    Long id;
    String organization;
    Long accountNumber;
    BigDecimal balance;
    String currency;
    ZonedDateTime lastModified;
}
