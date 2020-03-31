package com.pousheng.middle.web.utils.mail;

import lombok.Data;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/17
 * Time: 下午2:11
 */
@Data
public class MailSenderInfo
{
    /**
     * @FieldName mailServerHost 发送邮件的服务器的IP
     */
    private String mailServerHost;

    /**
     * @FieldName mailServerPort 发送邮件的服务器的端口
     */
    private String mailServerPort = "994";

    /**
     * @FieldName fromAddress 邮件发送者的地址
     */
    private String fromAddress;

    /**
     * @FieldName toAddress 邮件接收者的地址
     */
    private String toAddress;

    /**
     * @FieldName ccAddress 抄送地址
     */
    private String ccAddress;

    /**
     * @FieldName scAddress 密送地址
     */
    private String bccAddress;

    /**
     * @FieldName userName 登陆邮件发送服务器的用户名
     */
    private String userName;

    /**
     * @FieldName password 登陆邮件发送服务器的密码
     */
    private String password;

    /**
     * @FieldName validate 是否需要身份验证
     */
    private boolean validate = false;

    /**
     * @FieldName subject 邮件主题
     */
    private String subject;

    /**
     * @FieldName content 邮件的文本内容
     */
    private String content;

    /**
     * @FieldName attachFileNames 邮件附件的文件名
     */
    private String[] attachFileNames;

    /**
     * @FieldName SSL_FACTORY 替换默认的socketFactory为SSLSocketFactory
     */
    private String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    
    /**
     * 获得邮件会话属性
     */
    public Properties getProperties()
    {
        Properties p = new Properties();
        p.put("mail.smtp.host", this.mailServerHost);
        p.put("mail.smtp.port", this.mailServerPort);
        p.put("mail.smtp.auth", validate ? "true" : "false");
        p.put("mail.smtp.socketFactory.fallback", "false");
	    p.put("mail.smtp.socketFactory.class", SSL_FACTORY);
	    p.put("mail.smtp.socketFactory.port", this.mailServerPort);
        return p;
    }

}

