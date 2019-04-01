package com.pousheng.middle.web.utils.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/17
 * Time: 下午2:10
 */
public class MyAuthenticator extends Authenticator
{
    String userName = null;
    String password = null;

    public MyAuthenticator() {
    }

    public MyAuthenticator(String username, String password)
    {
        this.userName = username;
        this.password = password;
    }

    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
    }
}
