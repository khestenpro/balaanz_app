package com.bitsvalley.micro.webdomain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MomoCustomerInfo {

    boolean validSubscriber;
    String phoneNumber;
    String phoneServiceProvider;
    String familyName;
    String givenNames;


}
