package com.pousheng.middle.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 格式校验工具
 *
 * @author xieqinghe .
 * @date 2018/4/11 下午4:50
 * @email xieqinghe@terminus.io
 */
public class VerifyUtil {

    private static final String EMAIL_REGULAR = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";

    private static final Pattern MOBLE_REGULAR = Pattern.compile("^[1][3,4,5,7,8][0-9]{9}$");

    private static final Pattern PHONE_REGULAR1 = Pattern.compile("^[0][0-9]{2,3}-[0-9]{5,10}$");

    private static final Pattern PHONE_REGULAR2 = Pattern.compile("^[0-9]{1}[0-9]{5,8}$");

    /**
     * 校验邮箱格式
     *
     * @param email
     * @return
     */
    public static Boolean verifyEmail(String email) {
        boolean flag;
        try {
            Pattern regex = Pattern.compile(EMAIL_REGULAR);
            Matcher matcher = regex.matcher(email);
            flag = matcher.matches();
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }

    /**
     * 手机号验证
     *
     * @param str
     * @return
     */
    public static Boolean verifyMobile(final String str) {
        try {
            return MOBLE_REGULAR.matcher(str).matches();
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    /**
     * 电话号码验证
     *
     * @param str
     * @return
     */
    public static Boolean verifyPhone(final String str) {
        try {
            if (str.length() > 9) {
                return PHONE_REGULAR1.matcher(str).matches();
            } else {
                return PHONE_REGULAR2.matcher(str).matches();
            }
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    /**
     * 校验是否是手机号或者电话
     *
     * @param
     * @return
     */
    public static Boolean verifyPhoneOrMobile(String str) {
        return verifyMobile(str) || verifyPhone(str);
    }
}
