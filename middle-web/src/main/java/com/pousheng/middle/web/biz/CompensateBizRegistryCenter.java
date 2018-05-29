package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 中台业务处理模块bean工厂
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 * @author tony
 */
@Component
public class PoushengMiddleCompensateBizRegistryCenter {

    private Map<PoushengCompensateBizType, MiddleCompensateBizService> registry = new HashMap<>();

    /**
     * 注册相应的bean
     *
     * @param bizType
     * @param middleCompensateBizService
     */
    public void register(PoushengCompensateBizType bizType, MiddleCompensateBizService middleCompensateBizService) {
        if (bizType == null) {
            throw new IllegalArgumentException("middleCompensateBizService bizType not provided");
        }
        if (middleCompensateBizService == null) {
            throw new IllegalArgumentException("middleCompensateBizService bizType can't be null");
        }
        registry.put(bizType, middleCompensateBizService);
    }

    /**
     * 获取相应的bean
     * @param poushengCompensateBiz
     * @return
     */
    public MiddleCompensateBizService getBizProcessor(PoushengCompensateBiz poushengCompensateBiz) {
        MiddleCompensateBizService service = registry.get(poushengCompensateBiz.getBizType());
        if (service == null) {
            throw new IllegalStateException("poushengMiddleCompensateBizService not registered of bizType:" + poushengCompensateBiz.getBizType());
        }
        return service;
    }

}
