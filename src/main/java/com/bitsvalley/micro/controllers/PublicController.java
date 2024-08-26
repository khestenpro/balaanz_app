package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.CurrentAccountTransaction;
import com.bitsvalley.micro.domain.PaymentTransaction;
import com.bitsvalley.micro.repositories.PaymentTransactionRepository;
import com.bitsvalley.micro.services.InitSystemService;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class PublicController extends SuperController{

    @Autowired
    InitSystemService initSystemService;

    @Autowired
    PaymentTransactionRepository paymentTransactionRepository;

//    @Autowired
//    PasswordEncoder passwordEncoder;

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

        @GetMapping(value = "/pay/{bid}")
        public String landing( @PathVariable String bid, ModelMap model ){

//            System.out.println("------- -----------                 -------------- ------------");
//            System.out.println(passwordEncoder.encode("bitsv@ll3y"));
//            System.out.println("------- -----------                 -------------- ------------");

            RuntimeSetting byBid = initSystemService.findByBid(bid);
            model.addAttribute("businessInfo",byBid);
            model.put("currentAccountTransaction",new CurrentAccountTransaction());
            return "landing";
    }

    @PostMapping("/registerPublicTransactionCCForm")
    public String checkoutCC(ModelMap model, @ModelAttribute("currentAccountTransaction") CurrentAccountTransaction currentAccountTransaction,
                             HttpServletRequest request) {
        String bid = request.getParameter("bid");
        RuntimeSetting byBid = initSystemService.findByBid(bid);
        model.addAttribute("businessInfo",byBid);

        BigDecimal bdAmount = new BigDecimal(currentAccountTransaction.getCurrentAmount());
        BigDecimal interest = new BigDecimal(.035);
        interest = bdAmount.multiply(interest);
        BigDecimal totalAmount = bdAmount.add(interest);
        String amountFormat = String.format("%.2f", totalAmount);

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_EVEN);
        model.addAttribute("stripeAmount", totalAmount.multiply(new BigDecimal(100)).doubleValue()); // in cents

        PaymentTransaction paymentTransaction = new PaymentTransaction();
        model.addAttribute("totalAmount", amountFormat); // in cents

        model.addAttribute("netAmount", new Double(currentAccountTransaction.getCurrentAmount()).intValue()); // in cents
        Integer amt = Double.valueOf(currentAccountTransaction.getCurrentAmount()*100).intValue();
        model.addAttribute("amount", amt); // in cents

        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("currency", "USD");
        model.addAttribute("description", "Making a payment into the "+ bid + " "+currentAccountTransaction.getNotes());
        model.put("paymentTransaction",paymentTransaction);
        return "ccPublicCheckout";
    }

}
