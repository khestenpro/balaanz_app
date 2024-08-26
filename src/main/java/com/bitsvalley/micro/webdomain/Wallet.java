package com.bitsvalley.micro.webdomain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
public class Wallet {
    OrangeWallet orangeWallet;
    MtnWallet mtnWallet;
}
