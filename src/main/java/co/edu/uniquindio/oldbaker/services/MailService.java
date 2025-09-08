package co.edu.uniquindio.oldbaker.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.util.Locale;
import java.util.Map;


/**
 * Servicio para el envío de correos electrónicos utilizando plantillas Thymeleaf.
 * Proporciona un método asíncrono para enviar correos electrónicos con contenido HTML generado a partir de una plantilla y variables dinámicas.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private  final TemplateEngine templateEngine;

    // Constructor para inyectar las dependencias necesarias
    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }


    /** Envía un correo electrónico de manera asíncrona.
     *
     * @param to        La dirección de correo electrónico del destinatario.
     * @param subject   El asunto del correo electrónico.
     * @param variables Un mapa de variables que se utilizarán para rellenar la plantilla del correo.
     * @throws MessagingException Si ocurre un error al crear o enviar el correo electrónico.
     */
    @Async
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
