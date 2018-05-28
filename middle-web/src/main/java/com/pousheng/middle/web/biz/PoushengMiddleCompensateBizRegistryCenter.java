package com.pousheng.middle.web.biz;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.open.client.order.service.OpenClientOrderService;
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

    private Map<PoushengCompensateBizType, PoushengMiddleCompensateBizService> registry = new HashMap<>();

    /**
     * 注册相应的bean
     *
     * @param bizType
     * @param poushengMiddleCompensateBizService
     */
    public void register(PoushengCompensateBizType bizType, PoushengMiddleCompensateBizService poushengMiddleCompensateBizService) {
        if (bizType == null) {
            throw new IllegalArgumentException("poushengMiddleCompensateBizService bizType not provided");
        }
        if (poushengMiddleCompensateBizService == null) {
            throw new IllegalArgumentException("poushengMiddleCompensateBizService bizType can't be null");
        }
        registry.put(bizType, poushengMiddleCompensateBizService);
    }

    /**
     * 获取相应的bean
     * @param bizType
     * @return
     */
    public PoushengMiddleCompensateBizService getBizProcessor(PoushengCompensateBizType bizType) {
        PoushengMiddleCompensateBizService service = registry.get(bizType);
        if (service == null) {
            throw new IllegalStateException("poushengMiddleCompensateBizService not registered of bizType:" + bizType);
        }
        return service;
    }

}
