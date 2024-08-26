package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.CurrentAccountTransaction;
import com.bitsvalley.micro.domain.PaymentTransaction;
import com.bitsvalley.micro.repositories.FolePayTransactionRepository;
import com.bitsvalley.micro.repositories.PaymentTransactionRepository;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.ChargeRequest;
import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    @Autowired
    PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    FolePayTransactionRepository folePayTransactionRepository;

    @Autowired
    CallCenterService callCenterService;

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
    public Charge charge(ChargeRequest chargeRequest)
            throws StripeException {
        Map<String, Object> chargeParams = new HashMap<>();

        BigDecimal bd = new BigDecimal(chargeRequest.getAmount());
//        BigDecimal multiply = bd.multiply(new BigDecimal(0.030));
//        bd = bd.add(multiply);
//        bd = bd.setScale(0, RoundingMode.HALF_EVEN);
        BigDecimal bdAmount = new BigDecimal(chargeRequest.getAmount());
        BigDecimal interest = new BigDecimal(.035);
        interest = bdAmount.multiply(interest);
        final BigDecimal totalAmount = bdAmount.add(interest);

        chargeParams.put("amount", totalAmount.multiply(new BigDecimal(.01)).intValue() );
        chargeParams.put("currency", chargeRequest.getCurrency());
        chargeParams.put("description", chargeRequest.getDescription());
        chargeParams.put("source", chargeRequest.getStripeToken());

        return Charge.create(chargeParams);
    }

    public void updateStripePaymentTransaction(ChargeRequest chargeRequest, Charge charge, String username) {
            PaymentTransaction paymentTransaction = new PaymentTransaction();
            paymentTransaction.setModeOfPayment(BVMicroUtils.CREDIT_CARD);
            paymentTransaction.setReference(charge.getId());
            paymentTransaction.setUsername(username);
            paymentTransaction.setStatus(charge.getStatus());
            paymentTransaction.setAmount(charge.getAmount());
            paymentTransaction.setDescription(chargeRequest.getDescription());
            paymentTransaction.setCreatedDate(new Date());
            paymentTransactionRepository.save(paymentTransaction);

            callCenterService.saveCallCenterLog(
                    paymentTransaction.getReference(),username,"",
                    "Stripe Payment made. Status: "+ charge.getStatus()+": "  + paymentTransaction.getDescription()+  " Amount: " + charge.getAmount() );

    }

    public int getMonthlyLimit(double mobileMoneyMonthlyLimit, Date day) {
        String dayString    =    day.toString();

        return 0;
    }

    public Double getDailyLimit(LocalDate day, long orgId, long userId) {
        LocalDateTime endDay = day.atTime(LocalTime.MAX);
        LocalDateTime startDay = day.atTime(LocalTime.MIN);

        Double list = folePayTransactionRepository.sumDailyLimit(startDay.toString(), endDay.toString(), orgId, userId);

//        List<PaymentTransaction> list = paymentTransactionRepository.a( orgId, username);

        return list;
    }
}
