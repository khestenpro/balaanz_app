package com.bitsvalley.micro.services;


//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
//import org.springframework.mail.javamail.JavaMailSender;
//import javax.mail.MessagingException;
//import javax.mail.internet.MimeMessage;
//import java.io.File;


//@Service
public class EmailSenderService extends SuperService {

//    @Autowired
//    public CurrentAccountService currentAccountService;
//
//    @Autowired
//    public InitSystemService initSystemService;
//
//    @Autowired
//    private JavaMailSender javaMailSender;
//
//    public void emailMe(String toEmail, String body, String subject, File attachment, String attachmentName) throws MessagingException {
//        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
//        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage,true);
//        mimeMessageHelper.setFrom("noreplybalaanz@gmail.com");
//        mimeMessageHelper.setTo(toEmail);
//        mimeMessageHelper.setText(body);
//        mimeMessageHelper.setSubject(subject);
//        FileSystemResource fileSystemResource = new FileSystemResource(attachment);
//        mimeMessageHelper.addAttachment(attachmentName, fileSystemResource);
//        javaMailSender.send(mimeMessage);
//        System.out.println("MESSAGE SENT ");
//    }

}