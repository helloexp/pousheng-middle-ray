/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.order.dispatch.component;

import com.pousheng.middle.order.dispatch.link.DispatchOrderLink;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 拦截器链
 * Author:  songrenfei
 * Date: 2017-12-10
 */
public class DispatchOrderChain {


    private List<DispatchOrderLink> dispatchOrderLinks;

    /**
     * 注入拦截器列表
     * @param dispatchOrderLinks 拦截器列表
     */
    public void setDispatchOrderLinks(List<DispatchOrderLink> dispatchOrderLinks){
        this.dispatchOrderLinks = dispatchOrderLinks ==null? Collections.<DispatchOrderLink>emptyList(): dispatchOrderLinks;
    }

    /**
     * 获取拦截器列表
     * @return  拦截器列表
     */
    public List<DispatchOrderLink> getDispatchOrderLinks() {
        return dispatchOrderLinks == null?  Collections.<DispatchOrderLink>emptyList(): dispatchOrderLinks;
    }
}
