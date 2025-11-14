package iuh.fit.se.sebook_backend.utils;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderUtil {
    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom("sebook4u@gmail.com");
            helper.setTo(toEmail);
            helper.setText(body,true);
            helper.setSubject(subject);

            mailSender.send(message);
            System.out.println("Mail Sent Successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error sending email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}