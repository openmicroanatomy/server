package fi.ylihallila.server.util;

import fi.ylihallila.server.Application;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class Mailer {

    private final Session session;

    public Mailer() {
        Configuration.SMTPConfiguration config = Application.getConfiguration().smtp();

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", config.tls());
        prop.put("mail.smtp.host", config.host());
        prop.put("mail.smtp.port", config.port());
        prop.put("mail.smtp.ssl.trust", config.trusted());

        this.session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username(), config.password());
            }
        });
    }

    public void sendMail(String recipient, String subject, String body) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Application.getConfiguration().smtp().email()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(body, "text/html");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }
}
