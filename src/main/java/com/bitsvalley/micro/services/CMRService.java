package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.CurrentAccount;
import com.bitsvalley.micro.domain.FolePayTransaction;
import com.bitsvalley.micro.repositories.FolePayTransactionRepository;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.CollectionRequestStatus;
import com.bitsvalley.micro.utils.DisbursementRequestStatus;
import com.bitsvalley.micro.utils.SignatureGenerator;
import com.bitsvalley.micro.webdomain.*;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class CMRService {

    @Autowired
    CurrentAccountService currentAccountService;

    @Autowired
    CallCenterService callCenterService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    FolePayTransactionRepository folePayTransactionRepository;

    @Autowired
    SavingAccountService savingAccountService;

    @Value("${MOMO_USERNAME}")
    private String USER_NAME;

    @Value("${MOMO_PASSWORD}")
    private String PASSWORD;

    @Value("${MOMO_COLLECT_FOLELOGIX_API}")
    private String MOMO_COLLECT_FOLELOGIX_API;

    @Value("${MOMO_DISBURSE_FOLELOGIX_API}")
    private String MOMO_DISBURSE_FOLELOGIX_API;

    @Value("${MOMO_WALLET_BALANCE_FOLELOGIX_API}")
    private String MOMO_WALLET_BALANCE_FOLELOGIX_API;

    @Value("${MOMO_TOKEN_FOLELOGIX_API}")
    private String MOMO_TOKEN_FOLELOGIX_API;

    @Value("${MOMO_CURRENCY}")
    private String MOMO_CURRENCY;

    @Value("${MOMO_SUBSCRIBER_INFO_API}")
    private String MOMO_SUBSCRIBER_INFO_API;

    @Value("${MOMO_BEARER_TOKEN_API}")
    private String MOMO_BEARER_TOKEN_API;

    @Value("${MOMO_FOLELOGIX_HMAC_SIGNATURE}")
    private String MOMO_FOLELOGIX_HMAC_SIGNATURE;

    @Value("${MOMO_TRANSACTIONS_FOLELOGIX_API}")
    private String MOMO_TRANSACTIONS_FOLELOGIX_API;

    RestTemplate restTemplate = new RestTemplate();


    public CollectionRequestStatus sendMomoCollectionRequest(CollectionRequest collectionRequest) {

        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.add("Client", "bitsvalley");
        String token = getAuthToken();
        System.out.println("Log ---------------- Calling FoleLogix " + collectionRequest.getAmount() + " Number" + collectionRequest.getSenderPhoneNumber());

        try {
            requestHeaders.add("Captcha-Token", SignatureGenerator.generateSignature(MOMO_FOLELOGIX_HMAC_SIGNATURE, token));
        } catch (Exception e) {
            e.printStackTrace();
        }
        requestHeaders.add("Authorization", "Bearer " + token);
        HttpEntity<CollectionRequest> requestEntity = new HttpEntity<CollectionRequest>(getCollectionRequest(collectionRequest.getAmount(), collectionRequest.getSenderPhoneNumber()), requestHeaders);
        ResponseEntity<CollectionRequestStatus> responseEntity = restTemplate.exchange(MOMO_COLLECT_FOLELOGIX_API, HttpMethod.POST, requestEntity, CollectionRequestStatus.class);
        return responseEntity.getBody();
    }

    @Transactional
    public DisbursementRequestStatus sendMomoDisbursementRequest(CurrentAccount currentAccount, DisbursementRequest disbursementRequest, RuntimeSetting rt) {

        HttpHeaders requestHeaders = new HttpHeaders();
        String token = getAuthToken();

        try {
            requestHeaders.add("Captcha-Token", SignatureGenerator.generateSignature(MOMO_FOLELOGIX_HMAC_SIGNATURE, token));
            log.info(" -------    --------- Got the captcha ----   ---- ");

        } catch (Exception e) {
            e.printStackTrace();
        }
        requestHeaders.add("Authorization", "Bearer " + token);
        HttpEntity<DisbursementRequest> requestEntity = new HttpEntity<DisbursementRequest>(getDisbursementRequest(disbursementRequest.getAmount(), disbursementRequest.getRecipientPhoneNumber()), requestHeaders);

        log.info(" --------       -------- disburse now ------           ------------");
        ResponseEntity<DisbursementRequestStatus> responseEntity = restTemplate.exchange(MOMO_DISBURSE_FOLELOGIX_API, HttpMethod.POST, requestEntity, DisbursementRequestStatus.class);
        DisbursementRequestStatus status = responseEntity.getBody();

        log.info(" --------       -------- calculatePlatformFee now ------           ------------");
        BigDecimal fee = savingAccountService.calculatePlatformFee(String.valueOf(disbursementRequest.getAmount().doubleValue()), rt.getPlatformFee());
        BigDecimal totalAmountWithFee = fee.add(BigDecimal.valueOf(disbursementRequest.getAmount().doubleValue()));

        log.info(" --------       -------- transfering FromCurrentToCurrent now ------           ------------");
        savingAccountService.transferFromCurrentToCurrent(currentAccount.getAccountNumber(),
                rt.getMomoOrgAccount(),
                totalAmountWithFee.doubleValue(), "Momo transfer to " + BVMicroUtils.maskPhoneNumber(disbursementRequest.getRecipientPhoneNumber().toString()), currentAccount.getOrgId(), rt);
        log.info(" --------  -------- transfered happened to org momo   ----------   --------");
        StringBuilder sb = new StringBuilder();
        sb.append("Request Id: ");
        sb.append(status.getRequestId());
        sb.append(" Transaction Id: ");
        sb.append(status.getTransactionId());
        sb.append(status.getNote());
        callCenterService.saveCallCenterLog(sb.toString(), currentAccount.getUser().getUserName(), BVMicroUtils.maskPhoneNumber(disbursementRequest.getRecipientPhoneNumber().toString()), "1.Manual MOMO withdrawal 2.Reduce Min. "+ rt.getCurrentAccount()+" Balance by: " + BVMicroUtils.formatCurrency(disbursementRequest.getAmount().doubleValue()));

        log.info(" --------  -------- record outgoing folePayTransaction  ----------   --------");
        // Record outgoing FoleLogic transaction
        FolePayTransaction folePayTransaction = new FolePayTransaction();
        folePayTransaction.setTransactionId(status.getTransactionId());
        folePayTransaction.setRequestId(status.getRequestId());
        folePayTransaction.setTransactionFee(status.getTransactionFee());
//        folePayTransaction.setCashOutFee(status.getCashOutFee());
        folePayTransaction.setStatus(status.getStatus());
        folePayTransaction.setAccountNumber(currentAccount.getUser().getUserName());
        folePayTransaction.setMsisdnProvider(status.getMsisdnProvider());
        folePayTransaction.setPhoneNumber(disbursementRequest.getRecipientPhoneNumber());
        folePayTransaction.setNote(status.getNote());
        folePayTransaction.setAmount(status.getAmount());
        folePayTransaction.setOrgId(currentAccount.getOrgId());
        folePayTransaction.setUserId(currentAccount.getUser().getId());
        folePayTransaction.setDate(new Date());
//        folePayTransaction.setCurrency(status.getCurrency());
        folePayTransactionRepository.save(folePayTransaction);

        log.info(" --------  -------- sampleDisbursement EMail   ----------   --------");
        // if sender subscribed for notifications
        if (currentAccount.getUser().isReceiveEmailNotifications() && currentAccount.getUser().getEmail() != null) { // if receiver subscribed for notifications
            notificationService.notifySender(disbursementRequest.getAmount().doubleValue(), rt, BVMicroUtils.maskPhoneNumber(disbursementRequest.getRecipientPhoneNumber().toString()), currentAccount.getUser(), status.getRequestId(), "Mobile Money Transfer ");
        }

        return responseEntity.getBody();
    }

    public String getAuthToken() {
        String encodedCredentials = Base64.getEncoder().encodeToString((USER_NAME + ":" + PASSWORD).getBytes());
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", "Basic " + encodedCredentials);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<String> responseEntity = restTemplate.exchange(MOMO_TOKEN_FOLELOGIX_API, HttpMethod.POST, requestEntity, String.class);
        String tokenResponse = responseEntity.getBody();
        String authTokenExpression = "$.token";
        String authToken = JsonPath.parse(tokenResponse).read(authTokenExpression, String.class);
        return authToken;
    }


    public Wallet getBalance() throws NoSuchAlgorithmException, InvalidKeyException {
        String bearerToken = getBearerToken();
        String captchaToken = getCaptchaToken(bearerToken);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/json");
        requestHeaders.add("Authorization", "Bearer " + bearerToken);
        requestHeaders.add("Captcha-Token", captchaToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);

        log.info("--------            ------     url        ----------            ------" + MOMO_WALLET_BALANCE_FOLELOGIX_API);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Wallet> responseEntity = restTemplate.exchange(MOMO_WALLET_BALANCE_FOLELOGIX_API, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<Wallet>() {
        });
        return responseEntity.getBody();
    }


    public MomoCustomerInfo getSubscriberInfo(String disbursementPhoneNumber) throws NoSuchAlgorithmException, InvalidKeyException {
        String bearerToken = getBearerToken();
        String captchaToken = getCaptchaToken(bearerToken);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/json");
        requestHeaders.add("Authorization", "Bearer " + bearerToken);
        requestHeaders.add("Captcha-Token", captchaToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);

        String url = MOMO_SUBSCRIBER_INFO_API + disbursementPhoneNumber;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<MomoCustomerInfo> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, MomoCustomerInfo.class);
        return responseEntity.getBody();
    }


    public List<FolePayTransactionWeb> getTransactionSummary() throws NoSuchAlgorithmException, InvalidKeyException {
        String bearerToken = getBearerToken();
        String captchaToken = getCaptchaToken(bearerToken);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/json");
        requestHeaders.add("Authorization", "Bearer " + bearerToken);
        requestHeaders.add("Captcha-Token", captchaToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);

        log.info("--------         ------     url        ----------          ------" + MOMO_TRANSACTIONS_FOLELOGIX_API);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<FolePayTransactionWeb>> responseEntity = restTemplate.exchange(MOMO_TRANSACTIONS_FOLELOGIX_API, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<List<FolePayTransactionWeb>>() {
        });
        return responseEntity.getBody();
    }


    private String getCaptchaToken(String jwt) throws InvalidKeyException, NoSuchAlgorithmException {
        String captchaToken = SignatureGenerator.generateSignature(MOMO_FOLELOGIX_HMAC_SIGNATURE, jwt);
        return captchaToken;
    }


    private String getBearerToken() {

        String encodedCredentials = Base64.getEncoder().encodeToString((USER_NAME + ":" + PASSWORD).getBytes());
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Content-Type", "application/json");
        requestHeaders.add("Authorization", "Basic " + encodedCredentials);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(MOMO_BEARER_TOKEN_API, HttpMethod.POST, requestEntity, String.class);
        String tokenResponse = responseEntity.getBody();
        String authTokenExpression = "$.token";
        String authToken = JsonPath.parse(tokenResponse).read(authTokenExpression, String.class);
        return authToken;
    }


    public DisbursementRequest getDisbursementRequest(BigDecimal disburseAmount, Long disbursePhoneNumber) {
        DisbursementRequest disbursementRequest = new DisbursementRequest();
        disbursementRequest.setAmount(disburseAmount);
        if (StringUtils.equals(MOMO_CURRENCY, "EUR")) {
            disbursementRequest.setCurrency(ChargeRequest.Currency.EUR);
        } else {
            disbursementRequest.setCurrency(ChargeRequest.Currency.XAF);
        }
        disbursementRequest.setRecipientPhoneNumber(disbursePhoneNumber); // 67734333L
        disbursementRequest.setNote("Client disbursement");
//        disbursementRequest.setRecipientMsisdnProvider("MTN");

        //TODO: Transaction fee. Call API to determine transaction to add to final amount
//        disbursementRequest.setTransactionFee();
        disbursementRequest.setTransactionFee(new BigDecimal(0));
        return disbursementRequest;
    }

    public CollectionRequest getCollectionRequest(BigDecimal collectionAmount, Long collectionPhoneNumber) {
        CollectionRequest debit = new CollectionRequest();
//        debit.setTransactionId(UUID.randomUUID().toString());
        debit.setAmount(collectionAmount);

        //TODO: Transaction fee. Call API to determine cashapp fee to add to final amount
//        debit.setCashOutFee(BigDecimal.valueOf(0));
        debit.setTransactionFee(BigDecimal.valueOf(0));

        if (StringUtils.equals(MOMO_CURRENCY, "EUR")) {
            debit.setCurrency(ChargeRequest.Currency.EUR);
        } else {
            debit.setCurrency(ChargeRequest.Currency.XAF);
        }
        debit.setSenderPhoneNumber(collectionPhoneNumber);
        debit.setNote("Client collection");
//        debit.setMsisdnProvider("MTN");
        return debit;
    }


}
