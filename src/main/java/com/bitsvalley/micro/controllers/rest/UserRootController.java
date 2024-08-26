package com.bitsvalley.micro.controllers.rest;

import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.model.requests.CreateUserPayload;
import com.bitsvalley.micro.services.UserRootService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/userRoot")
@Slf4j
@RequiredArgsConstructor
public class UserRootController {

  private final UserRootService userRootService;

  @PostMapping("")
  public ResponseEntity<List<String>> createUser(@RequestBody CreateUserPayload createUserPayload){
    log.info("Create usr payload : {}",createUserPayload);
    userRootService.create(createUserPayload);
    return new ResponseEntity<>(Collections.singletonList("success"), HttpStatus.CREATED);
  }
  @GetMapping("/users")
  public ResponseEntity<?> getAllAgents(@RequestParam(name = "orgId") long orgId,
  @RequestParam(name = "username") String username){
    List<User> agents = userRootService.getAgents(orgId, username);

    return new ResponseEntity<>(agents,HttpStatus.OK);
  }
}
