package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.webdomain.DisbursementRequest;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class NotificationService {
    private static final String IMAGE_ASSETS_LINK = "https://test.balaanz.com/assets/images";
    public static final String PNG = ".png";
    public static final String SEPARATOR = "/";

    private JavaMailSender javaMailSender;
    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Autowired
    public NotificationService(FreeMarkerConfigurer freeMarkerConfigurer) {
        this.freeMarkerConfigurer = freeMarkerConfigurer;
    }

    public void sampleWelcomeEmail(RuntimeSetting runtimeSetting, User user) {
        try {
            Map<String, Object> templateData = getTemplateData(runtimeSetting, user);
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = configuration.getTemplate("newCustomerEmailTemplate.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateData);
            html = buildEmailWithDetails(html, runtimeSetting);
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            String[] emails = {"fchifen@gmail.com", user.getEmail()};

            helper.setTo(emails);
            helper.setSubject("Welcome to " + runtimeSetting.getBusinessName());
            helper.setFrom("info@balaanz.com");
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private Map<String, Object> getTemplateData(RuntimeSetting runtimeSetting, User user) {
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("businessName", runtimeSetting.getBusinessName());
        templateData.put("recipient", user.getFirstName() + ' ' + user.getLastName());
        templateData.put("phoneNumber", runtimeSetting.getTelephone());
        templateData.put("amount", user.getFirstName() + "created");
        templateData.put("referenceId", runtimeSetting.getBid());
        templateData.put("status", "New Account Created");
        templateData.put("payeeNote", runtimeSetting.getNotes());
        return templateData;
    }

    private String buildEmailWithDetails(String html, RuntimeSetting runtimeSetting) {
        Map<String, String> placeHolders = getPlaceHolders(runtimeSetting);
        StringBuilder htmBuilder = new StringBuilder(html);
        for (String placeholder : placeHolders.keySet()) {
            int indexOf = htmBuilder.indexOf(placeholder);
            while (indexOf != -1) {
                if (!StringUtils.isNotEmpty(placeHolders.get(placeholder))) {
                    htmBuilder.replace(indexOf, indexOf + placeholder.length(), StringUtils.EMPTY);
                    indexOf = htmBuilder.indexOf(placeholder);
                } else {
                    htmBuilder.replace(indexOf, indexOf + placeholder.length(), placeHolders.get(placeholder));
                    indexOf = htmBuilder.indexOf(placeholder, indexOf + placeHolders.get(placeholder).length());
                }
            }
        }
        return htmBuilder.toString();
    }

    private Map<String, String> getPlaceHolders(RuntimeSetting runtimeSetting) {
        Map<String, String> placeHolders = new HashMap<>();
        placeHolders.put("BG_LOGO_IMAGE", IMAGE_ASSETS_LINK.concat(SEPARATOR).concat(runtimeSetting.getBid().concat(PNG)));
        placeHolders.put("DESIGN_PORTFOLIO_1", IMAGE_ASSETS_LINK.concat(SEPARATOR).concat(runtimeSetting.getBid().concat("1").concat(PNG)));
        placeHolders.put("DESIGN_PORTFOLIO_2", IMAGE_ASSETS_LINK.concat(SEPARATOR).concat(runtimeSetting.getBid().concat("2").concat(PNG)));
        placeHolders.put("ORG_NAME", runtimeSetting.getBusinessName());
        placeHolders.put("ORG_MAIL", runtimeSetting.getEmail());
        placeHolders.put("ORG_WEBSITE", runtimeSetting.getWebsite());
        placeHolders.put("ORG_PHONE1", runtimeSetting.getTelephone());
        placeHolders.put("ORG_PHONE2", runtimeSetting.getTelephone2());
        placeHolders.put("ORG_SLOGAN", runtimeSetting.getSlogan());
        placeHolders.put("ORG_SUMMARY_1", runtimeSetting.getEmailDescription1());
        placeHolders.put("ORG_SUMMARY_2", runtimeSetting.getEmailDescription2());
        mapServicesOffered(runtimeSetting, placeHolders);
        return placeHolders;
    }

    private static void mapServicesOffered(RuntimeSetting runtimeSetting, Map<String, String> placeHolders) {
        String organizationProvidedServices = runtimeSetting.getOrganizationProvidedServices();
        if (StringUtils.isNotEmpty(organizationProvidedServices)) {
            String trim = StringUtils.trim(organizationProvidedServices);
            String[] split = trim.split(",");
            String li = "<li>%s</li>";

            int median = split.length % 2;
            int firstItr, secondItr;
            if (median == 0) {
                firstItr = secondItr = split.length / 2;
            } else {
                firstItr = split.length / 2;
                secondItr = split.length - firstItr;
            }
            StringBuilder result1 = new StringBuilder();
            for (int i = 0; i < firstItr; i++) {
                result1.append(String.format(li, split[i]));
            }
            StringBuilder result2 = new StringBuilder();
            for (int i = 0; i < secondItr; i++) {
                result2.append(String.format(li, split[i]));
            }
            placeHolders.put("SERVICE_OFFERED_LIST_1", result1.toString());
            placeHolders.put("SERVICE_OFFERED_LIST_2", result2.toString());
        }
    }

//    public void sampleTransferEmail(double amount, String orgId, String phoneNumber, String emailSender,String payeeNote) {
//        try {
//            Map<String, Object> templateData = new HashMap<>();
//            templateData.put("businessName", orgId);
//            templateData.put("recipient", "xxxxxx");
//            templateData.put("phoneNumber", phoneNumber);
//            templateData.put("amount", amount);
//            templateData.put("currency", "frs CFA");
//            templateData.put("referenceId", "xxxxxxxx");
//            templateData.put("status", "Momo/ OM disbursed");
//            templateData.put("payeeNote", payeeNote);
//
//            Configuration configuration = freeMarkerConfigurer.getConfiguration();
//            configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
//            Template template = configuration.getTemplate("email_depo_template.ftl");
//            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateData);
//
//            MimeMessage message = javaMailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
//
//            String[] emails = {"fchifen@gmail.com", emailSender};
//
//            helper.setTo(emails);
//            helper.setSubject("Deposit transfer made " + amount + "frs - " + orgId );
//            helper.setFrom("info@balaanz.com");
//            helper.setText(html, true);
//
//            javaMailSender.send(message);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void notifySender(double amount, RuntimeSetting rt, String recipient, User fromUser, String requestId, String transferType) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("businessName", rt.getBusinessName());
            templateData.put("imageLogo", IMAGE_ASSETS_LINK.concat(SEPARATOR).concat(rt.getBid().concat(PNG)));

            templateData.put("phoneNumber", recipient);
            templateData.put("amount", BVMicroUtils.formatCurrency(amount));
            templateData.put("currency", " " + rt.getCurrency());
            templateData.put("dateTime", BVMicroUtils.formatDate(new Date()));
            templateData.put("transferType", transferType);
//            templateData.put("payeeNote", disbursementRequest.getNote());
            templateData.put("requestId", requestId);
            templateData.put("firstname", fromUser.getFirstName());
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = configuration.getTemplate("transfer_template.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateData);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(fromUser.getEmail());
            helper.setBcc("info@balaanz.com");
            helper.setSubject(rt.getBusinessName() + " Online Transaction Made ");
            helper.setFrom("info@balaanz.com");
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void notifyReceiver(double amount, RuntimeSetting rt, User toUser, String accountNumber, String requestId, String transferType) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("businessName", rt.getBusinessName());
            templateData.put("imageLogo", IMAGE_ASSETS_LINK.concat(SEPARATOR).concat(rt.getBid().concat(PNG)));
            templateData.put("accountNumber", accountNumber);

            templateData.put("amount", BVMicroUtils.formatCurrency(amount));
            templateData.put("currency", " " + rt.getCurrency());
            templateData.put("dateTime", BVMicroUtils.formatDate(new Date()));
            templateData.put("transferType", transferType);
//            templateData.put("payeeNote", disbursementRequest.getNote());
            templateData.put("requestId", requestId);
            templateData.put("firstname", toUser.getFirstName());
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            configuration.setClassForTemplateLoading(this.getClass(), "/templates/");
            Template template = configuration.getTemplate("transfer_receiver_template.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, templateData);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(toUser.getEmail());
            helper.setSubject(rt.getBusinessName() + " Online Transaction Received ");
            helper.setFrom("info@balaanz.com");
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}