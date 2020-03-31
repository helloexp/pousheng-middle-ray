package com.pousheng.middle.web.order.component;

/**
 * @author bernie
 * @date 2019/6/17
 */
public interface RefundPostProcessor {

    /**
     * 接收逆向单之前业务逻辑处理
     */
    default void postProcessorBeforeCreated(){};

    /**
     * 接收逆向单生成之后业务逻辑处理
     */
    default void postProcessorAfterCreated(Long refundId){}
}


