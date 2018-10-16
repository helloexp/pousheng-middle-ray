package com.pousheng.middle.web.biz.dto;

import io.terminus.parana.common.model.Criteria;
import lombok.Data;

/**
 * 导出交易单据事件，包括订单，售后单以及发货单
 * @author tanlongjun
 * @param <T>
 *
 */
@Data
public class ExportTradeBillDTO<T extends Criteria> {

    private String type;

    private Long userId;

    private T criteria;

}
