package com.bitsvalley.micro.model.response;

import com.bitsvalley.micro.utils.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserVO {

  private long id;
  private long orgId;
  private String userName;
  private String firstName;
  private String lastName;
  private String gender;
  private String referral;
  private String email;
  private Date created;
  private String createdBy;
  private AccountStatus accountStatus;
  private double unsignedAmount;
  private double collectionLimit;

}
