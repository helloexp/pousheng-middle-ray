package com.pousheng.erp.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.erp.model.PoushengWarehouse;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 同步仓库接口
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@Component
@Slf4j
public class WarehouseFetcher {
    public static final TypeReference<List<PoushengWarehouse>> LIST_OF_WAREHOUSE = new TypeReference<List<PoushengWarehouse>>() {
    };
    private final ErpClient erpClient;

    @Autowired
    public WarehouseFetcher(ErpClient erpClient) {
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
    public List<PoushengWarehouse>  fetch(int pageNo, int pageSize, Date start, Date end){
        try {
            String result = this.erpClient.get("e-commerce-api/v1/hk-get-stocks",
                    start, end, pageNo, pageSize, Maps.newHashMap());

            return JsonMapper.nonEmptyMapper().getMapper().readValue(result,
                    LIST_OF_WAREHOUSE);
        } catch (IOException e) {
            log.error("failed to deserialize json to PoushengWarehouse list, cause:{}",
                    Throwables.getStackTraceAsString(e));
            throw new ServiceException("warehouse.request.fail", e);
        } catch (ServiceException e){
            throw new ServiceException("warehouse.request.fail", e);
        }
    }
}
