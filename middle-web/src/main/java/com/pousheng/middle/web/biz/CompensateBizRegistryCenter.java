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
public class CompensateBizRegistryCenter {

    private Map<String, CompensateBizService> registry = new HashMap<>();

    /**
     * 注册相应的bean
     *
     * @param bizType
     * @param compensateBizService
     */
    public void register(String bizType, CompensateBizService compensateBizService) {
        if (bizType == null) {
            throw new IllegalArgumentException("compensateBizService bizType not provided");
        }
        if (compensateBizService == null) {
            throw new IllegalArgumentException("compensateBizService bizType can't be null");
        }
        registry.put(bizType, compensateBizService);
    }

    /**
     * 获取相应的bean
     * @param poushengCompensateBiz
     * @return
     */
    public CompensateBizService getBizProcessor(PoushengCompensateBiz poushengCompensateBiz) {
        CompensateBizService service = registry.get(poushengCompensateBiz.getBizType());
        if (service == null) {
            throw new IllegalStateException("poushengMiddleCompensateBizService not registered of bizType:" + poushengCompensateBiz.getBizType());
        }
        return service;
    }

}
