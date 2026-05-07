package com.student.server.email;

import com.alibaba.fastjson2.JSON;
import com.student.server.dataobject.OrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Component
public class EmailClient {

    @Value("${send.message.email}")
    private String messageSender;

    @Value("${send.message.code}")
    private String messageSenderCode;

    /**
     * 发送订单邮箱
     * @param messageAcceptor
     * @param content
     */
    public void sendOrderEmail(String messageAcceptor, String content) {
        try {
            final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

            //配置邮箱信息
            Properties props = System.getProperties();
            //邮件服务器
            props.setProperty("mail.smtp.host", "smtp.qq.com");
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            //邮件服务器端口
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", "465");
            //鉴权信息
            props.setProperty("mail.smtp.auth", "true");
            //建立邮件会话
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                //身份认证
                protected PasswordAuthentication getPasswordAuthentication() {
                    //1.账户 授权码
                    return new PasswordAuthentication(messageSender, messageSenderCode);
                }
            });
            //建立邮件对象
            MimeMessage message = new MimeMessage(session);
            //设置邮件的发件人
            message.setFrom(new InternetAddress(messageSender));
            //2.设置邮件的收件人
            message.setRecipients(Message.RecipientType.TO, messageAcceptor);
            //设置邮件的主题
            message.setSubject("抢购成功！您的订单详情");

            String html = buildOrderHtml(content);

            message.setContent(html, "text/html;charset=UTF-8");
            message.saveChanges();
            //发送邮件
            Transport.send(message);
           log.info("订单邮件发送成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送验证码邮箱
     * @param to
     * @param code
     */
    public void sendVerifyCodeEmail(String to, String code) {
        try {
            final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
            Properties props = new Properties();

            props.setProperty("mail.smtp.host", "smtp.qq.com");
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", "465");
            props.setProperty("mail.smtp.auth", "true");

            Session session = Session.getDefaultInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(messageSender, messageSenderCode);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(messageSender));
            message.setRecipients(Message.RecipientType.TO, to);
            message.setSubject("找回密码 - 邮箱验证码");
            // 👇 自动发送 HTML 格式验证码
            message.setContent(buildCodeHtml(code), "text/html;charset=UTF-8");
            message.saveChanges();
            Transport.send(message);
            log.info("验证码邮件发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("验证码邮件发送失败");
        }
    }

    /**
     * 构建订单页面
     * @param content
     * @return
     */
    private String buildOrderHtml(String content) {
        if (content == null || content.isEmpty()) {
            return "<h3>订单数据异常</h3>";
        }

        OrderDO order;
        try {
            order = JSON.parseObject(content, OrderDO.class);
        } catch (Exception e) {
            return "<h3>订单解析失败</h3>";
        }
        return "<div style='max-width:600px;margin:20px auto;padding:20px;border:1px solid #eee;border-radius:10px;font-family:微软雅黑;'>"
                + "<h2 style='color:#07C160;text-align:center;'>恭喜您，抢购成功</h2>"
                + "<div style='font-size:14px;color:#333;line-height:1.8;'>"
                + "<p><strong>订单编号：</strong>" + order.getOrderNumber() + "</p>"
                + "<p><strong>商品ID：</strong>" + order.getProductId() + "</p>"
                + "<p><strong>商品名称：</strong>" + order.getProduct().getName() + "<p/>"
                + "<p><strong>用户ID：</strong>" + order.getUserId() + "</p>"
                + "<p><strong>用户昵称：</strong>" + order.getUser().getNickName() + "<p/>"
                + "<p><strong>订单状态：</strong>" + order.getStatus().getDesc() + "</p>"
                + "<p><strong>支付金额：</strong><span style='color:red;font-size:16px;'>" + String.format("%.2f", order.getTotalPrice()) + " 元</span></p>"
                + "</div>"
                + "<div style='margin-top:20px;text-align:center;color:#999;font-size:12px;'>"
                + "感谢您使用一站式生活服务平台"
                + "</div>"
                + "</div>";
    }

    /**
     * 构建验证码页面
     * @param code
     * @return
     */
    private String buildCodeHtml(String code) {
        return "<div style='max-width:500px;margin:20px auto;padding:25px;border-radius:12px;border:1px solid #f0f0f0;font-family:微软雅黑;'>"
                + "<h3 style='color:#333;margin-top:0;'>淮北师范大学一站式生活服务平台</h3>"
                + "<p style='font-size:15px;'>你正在使用找回密码功能</p>"
                + "<div style='background:#f7f8fa;padding:15px;border-radius:8px;text-align:center;margin:20px 0;'>"
                + "<h1 style='color:#0066cc;margin:0;letter-spacing:3px;'>" + code + "</h1>"
                + "</div>"
                + "<p style='font-size:14px;color:#666;'>验证码 5 分钟内有效，请勿泄露给他人</p>"
                + "<p style='font-size:12px;color:#999;'>如非本人操作，请忽略本邮件</p>"
                + "</div>";
    }

}
