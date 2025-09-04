package co.edu.uniquindio.oldbaker.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.util.Locale;
import java.util.Map;


@Service
public class MailService {

    private final JavaMailSender mailSender;
    private  final TemplateEngine templateEngine;


    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }


    public void sendEmail(String to, String subject, Map<String, Object> variables) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        String html = templateEngine.process("email-template", new Context(Locale.getDefault(), variables));
        helper.setText(html, true);
        mailSender.send(mimeMessage);
    }

}
