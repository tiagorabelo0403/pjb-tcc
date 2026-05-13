package com.tcc.pjb.backend.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    public String channelId() {
        return "EMAIL";
    }

    @Override
    public boolean supports(UserNotificationPreference preference) {
        return preference == null || preference.isAllowEmail();
    }

    @Override
    public void send(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Canal de email indisponível (JavaMailSender não configurado). Destinatário={} | Título={}",
                    message.destinatario().getEmail(), message.titulo());
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(message.destinatario().getEmail());
            helper.setSubject(message.titulo());
            helper.setText(buildHtmlBody(context), true);
            mailSender.send(mimeMessage);

            log.info("Email enviado para: {}", message.destinatario().getEmail());

        } catch (MessagingException e) {
            log.error("Falha ao enviar email para {}: {}", message.destinatario().getEmail(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String buildHtmlBody(NotificationDispatchContext context) {
        NotificationMessage message = context.message();
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family:Arial,sans-serif;font-size:14px;line-height:1.5'>")
                .append("<h3>").append(escape(message.titulo())).append("</h3>")
                .append("<p>").append(escape(message.mensagem())).append("</p>");
        if (message.urlAcesso() != null && !message.urlAcesso().isBlank()) {
            html.append("<p><a href='").append(message.urlAcesso()).append("'>Abrir no PJB</a></p>");
        }
        html.append("<p><a href='").append(context.cienciaUrl()).append("'>Confirmar ciência</a></p>")
                .append("<img src='").append(context.pixelUrl()).append("' width='1' height='1' alt='' style='display:none' />")
                .append("</div>");
        return html.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
