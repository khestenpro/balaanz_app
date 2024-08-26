package com.bitsvalley.micro.controllers.rest;


import com.bitsvalley.micro.model.response.UserDetails;
import com.bitsvalley.micro.services.UserDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/")
@Slf4j
@RequiredArgsConstructor
public class UserDetailsController {

  private final UserDetailService userDetailService;
  /**
   * Give user details with daily savings balance
  * */
  @GetMapping("user/{username}")
  public ResponseEntity<UserDetails> getUser(@PathVariable String username){
    log.info("Request received : {}",username);
    UserDetails userDetails = userDetailService.getUserDetails(username);
    return new ResponseEntity<>(userDetails, HttpStatus.OK);
  }
}
