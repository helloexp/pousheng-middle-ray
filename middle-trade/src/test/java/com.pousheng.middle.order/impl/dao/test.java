package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.dto.SubmitRefundInfo;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/30
 * Time: 上午11:26
 */
public class test {
    public static void main(String[] args) {
        SubmitRefundInfo submitRefundInfo = new SubmitRefundInfo();
        submitRefundInfo.setFee(300L);

        System.out.print(submitRefundInfo.getFee());
    }
}
