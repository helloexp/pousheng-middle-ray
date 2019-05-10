package com.pousheng.middle.web.utils.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/17
 * Time: 下午2:28
 */
@Component
@Slf4j
public class MailLogic {


    // 邮箱服务地址
    @Value("${outlook.mail.server}")
    private String mailServer;
    // 用户名
    @Value("${outlook.mail.username}")
    private String username;
    // 密码
    @Value("${outlook.mail.password}")
    private String password;
    // 标题
    @Value("${outlook.mail.title}")
    private String title;
    // 抄送人
    @Value("${outlook.mail.copy}")
    private String emailCopy;


    public void sendMail (String receives, String content) {
        MailSenderInfo mailInfo = new MailSenderInfo();
        mailInfo.setMailServerHost(mailServer);
        mailInfo.setMailServerPort("994");
        mailInfo.setValidate(true);
        mailInfo.setUserName(username);
        mailInfo.setPassword(password);
        mailInfo.setFromAddress(username);
        mailInfo.setToAddress(receives);
        mailInfo.setBccAddress(emailCopy);
        mailInfo.setSubject(title);
        mailInfo.setContent(content);
        SimpleMailSender.sendTextMail(mailInfo);
        log.info("Send Mail Over!");
    }

    public void sendMail (String receives, String title,String content) {
        MailSenderInfo mailInfo = new MailSenderInfo();
        mailInfo.setMailServerHost(mailServer);
        mailInfo.setMailServerPort("994");
        mailInfo.setValidate(true);
        mailInfo.setUserName(username);
        mailInfo.setPassword(password);
        mailInfo.setFromAddress(username);
        mailInfo.setToAddress(receives);
//        mailInfo.setBccAddress(emailCopy);
        mailInfo.setSubject(title);
        mailInfo.setContent(content);
        SimpleMailSender.sendTextMail(mailInfo);
        log.info("Send Mail Over!");
    }
}
