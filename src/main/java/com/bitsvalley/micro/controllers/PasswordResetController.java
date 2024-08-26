package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {
  public static final int PAGES = 50;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  @Value("${private.secret}")
  private String secret;

  @PostMapping("passwordReset")
  public String doPasswordRest(@RequestHeader("privateKey") String _header){
    if (!_header.equals(secret)) {
      return "Invalid Key";
    }
    else {
      log.info("==== PROCESS STARTED FOR PASSWORD ENCODE ====");
      doProcessPasswords();
      return "Success";
    }
  }

  public void doProcessPasswords() {
    int totalRecords = userRepository.usersCount();
    log.info("==== TOTAL RECORDS TO BE UPDATE : {} ====",totalRecords);
    int totalPages = (int) Math.ceil((double) totalRecords / PAGES);
    for (int page = 0; page < totalPages; page++) {
      Pageable pageable = PageRequest.of(page, PAGES);
      Page<User> entitiesPage = userRepository.findAll(pageable);
      List<User> entitiesToUpdate = entitiesPage.getContent();
      for (User entity : entitiesToUpdate) {
        String encodedPassword = encodePassword(entity.getPassword());
        entity.setPassword(encodedPassword);
      }
      userRepository.saveAll(entitiesToUpdate);
    }
    log.info("==== PASSWORD ENCODING COMPLETED ====");
  }

  private String encodePassword(String password){
    if(password != null && !password.startsWith("$2a$10$")){
      return passwordEncoder.encode(password);
    }
    return password;
  }
}
