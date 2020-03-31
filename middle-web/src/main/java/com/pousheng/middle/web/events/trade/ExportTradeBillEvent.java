package com.pousheng.middle.web.events.trade;

import io.terminus.parana.common.model.Criteria;
import lombok.Data;

/**
 * Created by penghui on 2018/2/6
 * 导出交易单据事件，包括订单，售后单以及发货单
 */
@Data
public class ExportTradeBillEvent {

    private String type;

    private Long userId;

    private Criteria criteria;

}
