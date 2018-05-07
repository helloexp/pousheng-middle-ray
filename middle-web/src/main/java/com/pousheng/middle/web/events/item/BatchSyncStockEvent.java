package com.pousheng.middle.web.events.item;

import com.pousheng.middle.warehouse.dto.StockDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量同步品牌事件
 * Created by songrenfei on 2017/6/7
 */
@Data
public class BatchSyncStockEvent implements Serializable{
    private static final long serialVersionUID = -2577931179578376505L;
    private List<StockDto> stockDtos;
}
