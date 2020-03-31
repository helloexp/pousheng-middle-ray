package com.pousheng.middle.order.enums;

/**
 * 宝胜业务处理模块处理状态
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public enum  PoushengCompensateBizStatus {
    /**
     * 需要预处理
     */
    WAIT_PRE_HANDLE,
    /**
     * 待处理
     */
    WAIT_HANDLE,
    /**
     * 处理中
     */
    PROCESSING,
    /**
     * 处理成功
     */
    SUCCESS,
    /**
     * 处理失败
     */
    FAILED
}
