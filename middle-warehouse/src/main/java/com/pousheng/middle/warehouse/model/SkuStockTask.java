package com.pousheng.middle.warehouse.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.StockDto;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Author: songrenfei
 * Desc: sku库存同步任务Model类
 * Date: 2018-05-24
 */
@Data
public class SkuStockTask implements Serializable {

    private static final long serialVersionUID = 4291293525147003205L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private static final TypeReference<List<StockDto>> LIST_OF_STOCK = new TypeReference<List<StockDto>>() {
    };


    private Long id;

    /**
     * 状态, 0 待处理, 1处理中
     */
    private Integer status;

    /**
     * 商品数量
     */
    private Integer skuCount;

    /**
     * sku信息
     */
    private String skuJson;

    private List<StockDto> stockDtoList;

    /**
     * 任务超时时间，当状态为处理中时会记录下处理的超时时间，当到达超时时间时任务状态仍为处理中，则更正状态为待处理
     */
    private Date timeoutAt;

    private Date createdAt;

    private Date updatedAt;


    /**
     * json转对象
     * @param skuJson json
     * @throws Exception 异常
     */
    public void setSkuJson(String skuJson) throws Exception {
        this.skuJson = skuJson;
        if (Strings.isNullOrEmpty(skuJson)) {
            this.stockDtoList = Lists.newArrayList();
        } else {
            this.stockDtoList = objectMapper.readValue(skuJson, LIST_OF_STOCK);
        }
    }



    /**
     * 对象转json
     */
    public void setStockDtoList(List<StockDto> stockDtoList){
        this.stockDtoList = stockDtoList;
        if (CollectionUtils.isEmpty(stockDtoList)) {
            this.skuJson = "";
        } else {
            this.skuJson = JsonMapper.nonEmptyMapper().toJson(stockDtoList);
        }
    }
}
