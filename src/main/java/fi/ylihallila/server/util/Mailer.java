package fi.ylihallila.server.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

import static fi.ylihallila.server.util.Config.Config;

public class Mailer {

    private final Session session;

    public Mailer() {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", Config.getBoolean("smtp.tls"));
        prop.put("mail.smtp.host", Config.getString("smtp.host"));
        prop.put("mail.smtp.port", Config.getInt("smtp.port"));
        prop.put("mail.smtp.ssl.trust", Config.getString("smtp.ssl.trust"));

        this.session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Config.getString("smtp.username"), Config.getString("smtp.password"));
            }
        });
    }

    public void sendMail(String recipient, String subject, String body) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Config.getString("smtp.email")));
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
