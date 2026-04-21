package com.github.konstantinevashalomidze.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.application.customConfigs.constants.applicationEmail}")
    private String APPLICATION_EMAIL;

    private final JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(APPLICATION_EMAIL);
        message.setTo(toEmail);
        message.setSubject("Your App Verification Code");
        message.setText(
                """
                Hello!
                
                Your OTP for verification is: 
                """ + otpCode +
                """
                
                Please do not share this code with anyone.
                
                Contact: 
                """ + APPLICATION_EMAIL
        );

        mailSender.send(message);
    }

}
