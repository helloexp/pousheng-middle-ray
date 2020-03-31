package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author tanlongjun
 */
@Data
public class MiddleShipmentCriteria extends PagingCriteria implements Serializable{


    private static final long serialVersionUID = 8648172902284584642L;
    /**
     * 状态
     */
    private List<Integer> statusList;

    /**
     * 店铺编号
     */
    private Long shopId;




}
