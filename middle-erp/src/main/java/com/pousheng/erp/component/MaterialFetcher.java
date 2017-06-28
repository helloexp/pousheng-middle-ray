package com.pousheng.erp.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.erp.model.PoushengMaterial;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 同步货品
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@Component
@Slf4j
public class MaterialFetcher {

    private static final TypeReference<List<PoushengMaterial>> LIST_OF_MATERIAL = new TypeReference<List<PoushengMaterial>>() {
    };
    private final ErpClient erpClient;

    @Autowired
    public MaterialFetcher(ErpClient erpClient) {
        this.erpClient = erpClient;
    }

    /**
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数量
     * @param start 开始时间
     * @param end 结束时间(可空)
     * @return 对象
     */
    List<PoushengMaterial>  fetch(int pageNo, int pageSize, Date start, Date end){
        try {
            String result = this.erpClient.get("/e-commerce-api/v1/hk-materials",
                    start, end, pageNo, pageSize, Maps.newHashMap());

            return JsonMapper.nonEmptyMapper().getMapper().readValue(result,
                    LIST_OF_MATERIAL);
        } catch (IOException e) {
            log.error("failed to deserialize json to PoushengMaterial list, cause:{}",
                    Throwables.getStackTraceAsString(e));
            throw new ServiceException("material.request.fail", e);
        } catch (ServiceException e){
            throw new ServiceException("material.request.fail", e);
        }
    }
}
