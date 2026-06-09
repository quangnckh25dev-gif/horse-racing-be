package com.horseracing.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetTokenEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Yeu cau Dat lai Mat khau - Horse Racing System");
        message.setText("Chào bạn,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng sử dụng mã xác nhận dưới đây để tiếp tục:\n\n" +
                "MÃ XÁC NHẬN (TOKEN): " + token + "\n\n" +
                "Lưu ý: Mã xác nhận này sẽ hết hạn trong vòng 15 phút.\n\n" +
                "Trân trọng,\n" +
                "Ban quản trị Horse Racing System");

        mailSender.send(message);
    }
}
