package com.pousheng.middle.web.utils.mail;


import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/17
 * Time: 下午2:12
 */
public class SimpleMailSender
{
    /**
     * @MethodName sendTextMail
     * @Description 文本格式发送邮件
     *
     * @author laosan
     * @date 2014-12-8 下午2:32:22
     * @param mailInfo
     * @return
     */
    public static boolean sendTextMail(MailSenderInfo mailInfo)
    {
        if (null == mailInfo)
        {
            return false;
        }

        try
        {
            Message mailMessage = setMailBasicInfo(mailInfo);

            // 设置邮件的主要内容
            String mailContent = mailInfo.getContent();
            mailMessage.setText(mailContent);

            // 发送邮件
            Transport.send(mailMessage);
            return true;
        }
        catch (MessagingException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @MethodName sendHtmlMail
     * @Description 以HTML格式发送邮件
     *
     * @author laosan
     * @date 2014-12-8 下午2:35:53
     * @param mailInfo
     * @return
     */
    public static boolean sendHtmlMail(MailSenderInfo mailInfo)
    {
        if (null == mailInfo)
        {
            return false;
        }

        try
        {
            Message mailMessage = setMailBasicInfo(mailInfo);

            // MiniMultipart类是一个容器类，包含MimeBodyPart类型的对象
            Multipart mainPart = new MimeMultipart();

            // 创建一个包含HTML内容的MimeBodyPart
            BodyPart html = new MimeBodyPart();

            // 设置HTML内容
            html.setContent(mailInfo.getContent(), "text/html; charset=utf-8");
            mainPart.addBodyPart(html);

            // 将MiniMultipart对象设置为邮件内容
            mailMessage.setContent(mainPart);

            // 发送邮件
            Transport.send(mailMessage);
            return true;
        }
        catch (MessagingException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @MethodName sendMailForAttachFile
     * @Description 发送带有附件的邮件，以HTML形式发送
     *
     * @author laosan
     * @date 2014-12-8 下午03:43:10
     * @param mailInfo
     * @return
     */
    public static boolean sendHtmlMail4AttachFile(MailSenderInfo mailInfo)
    {
        // 如果传入的信息为空，直接返回发送失败
        if (null == mailInfo)
        {
            return false;
        }

        try
        {
            Message mailMessage = setMailBasicInfo(mailInfo);

            // MiniMultipart类是一个容器类，包含MimeBodyPart类型的对象
            Multipart mainPart = new MimeMultipart();

            // 创建一个包含HTML内容的MimeBodyPart
            BodyPart bodyPart = new MimeBodyPart();

            // 设置HTML内容
            bodyPart.setContent(mailInfo.getContent(), "text/html; charset=utf-8");
            mainPart.addBodyPart(bodyPart);

            // 添加所有的附件
            FileDataSource fileDataSource = null;
            for (int i = 0; i < mailInfo.getAttachFileNames().length; i++)
            {
                String attachFileName = mailInfo.getAttachFileNames()[i];
                bodyPart = new MimeBodyPart();
                fileDataSource = new FileDataSource(attachFileName);
                bodyPart.setDataHandler(new DataHandler(fileDataSource));
                bodyPart.setFileName(MimeUtility.encodeText(fileDataSource.getName(), "utf-8", null));
                mainPart.addBodyPart(bodyPart);
            }

            // 将MiniMultipart对象设置为邮件内容
            mailMessage.setContent(mainPart);

            // 发送邮件
            Transport.send(mailMessage);

            return true;
        }
        catch (MessagingException ex)
        {
            ex.printStackTrace();
        }
        catch (UnsupportedEncodingException ue)
        {
            ue.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @MethodName setMailBasicInfo
     * @Description 设置邮件发送的基本信息
     *
     * @author laosan
     * @date 2014-12-8 下午03:00:10
     * @param mailInfo
     * @return
     */
    private static Message setMailBasicInfo(MailSenderInfo mailInfo)
    {
        // 如果传入的邮件信息为空，返回空
        if (null == mailInfo)
        {
            return null;
        }

        // 判断是否需要身份认证, 如果需要身份认证，则创建一个密码验证器
        MyAuthenticator authenticator = null;
        if (mailInfo.isValidate())
        {
            authenticator = new MyAuthenticator(mailInfo.getUserName(), mailInfo.getPassword());
        }

        // 配置信息
        Properties pro = mailInfo.getProperties();

        // 根据邮件会话属性和密码验证器构造一个发送邮件的session
        Session sendMailSession = Session.getDefaultInstance(pro, authenticator);

        // 根据session创建一个邮件消息
        Message mailMessage = new MimeMessage(sendMailSession);

        try
        {
            // 发件人地址
            Address from = new InternetAddress(mailInfo.getFromAddress());
            mailMessage.setFrom(from);

            // 收件人地址
            InternetAddress[] to = new InternetAddress().parse(mailInfo.getToAddress());
            mailMessage.setRecipients(Message.RecipientType.TO, to);

            // 抄送地址
            if (null != mailInfo.getCcAddress() && mailInfo.getCcAddress().trim().length() > 0)
            {
                InternetAddress[] cc = new InternetAddress().parse(mailInfo.getCcAddress());
                mailMessage.setRecipients(Message.RecipientType.CC, cc);
            }

            // 密送地址
            if (null != mailInfo.getBccAddress() && mailInfo.getBccAddress().trim().length() > 0)
            {
                InternetAddress[] bcc = new InternetAddress().parse(mailInfo.getBccAddress());
                mailMessage.setRecipients(Message.RecipientType.BCC, bcc);
            }

            // 设置邮件的主题
            mailMessage.setSubject(mailInfo.getSubject());

            // 设置邮件发送的时间
            mailMessage.setSentDate(new Date());

            return mailMessage;
        }
        catch (AddressException e)
        {
            e.printStackTrace();
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
