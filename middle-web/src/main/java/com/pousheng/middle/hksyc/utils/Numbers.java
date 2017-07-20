package com.pousheng.middle.hksyc.utils;

import java.util.Random;

/**
 * Description: add something here
 * User: xiao
 * Date: 05/07/2017
 */
public class Numbers {

    public static String randomZeroPaddingNumber(int length, int max) {
        String str = max + "";
        if (str.length() > length) {
            throw new IllegalArgumentException("max's character length must not greater than 1st arg");
        }

        return String.format("%0" + length + "d", new Random().nextInt(max));
    }

    public static String zeroPaddingNum(int length, Long num) {
        return String.format("%0" + length + "d", num);
    }
    public static String zeroPaddingNum(int length, Integer num) {
        return String.format("%0" + length + "d", num);
    }

}
