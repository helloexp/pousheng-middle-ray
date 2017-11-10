package com.pousheng.middle.warehouse.service;

import com.pousheng.middle.warehouse.model.StockPushLog;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/10
 * pousheng-middle
 */
public interface MiddleStockPushLogReadSerive {

    /**
     * 根据id查询库存推送日志
     * @param Id 主键id
     * @return 仓库
     */
    Response<StockPushLog> findById(Long Id);

    /**
     *   库存推送日志列表
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数目
     * @param params 其他查询参数
     * @return 库存推送日志列表
     */
    Response<Paging<StockPushLog>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params);
}
