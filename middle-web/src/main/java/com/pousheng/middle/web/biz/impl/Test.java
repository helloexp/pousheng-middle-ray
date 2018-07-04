package com.pousheng.middle.web.biz.impl;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/7/4
 * pousheng-middle
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(new DateTime(new Date()).plusDays(15));
    }
}
