// src/main/java/com/example/examplefeature/mail/EmailServiceSergiy.java
package com.example.examplefeature.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceSergiy {

    private final JavaMailSender sender;

    // Usa 'app.mail.from' se existir; caso contrário usa 'spring.mail.username'
    @Value("${app.mail.from:${spring.mail.username}}")

    private String from = "liolikkotovich@gmail.com";

    public EmailServiceSergiy(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendPlainText(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        from = "liolikkotovich@gmail.com";
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);

        sender.send(msg);
    }
}
