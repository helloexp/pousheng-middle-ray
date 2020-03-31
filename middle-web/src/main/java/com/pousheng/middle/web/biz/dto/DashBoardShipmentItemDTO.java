package com.pousheng.middle.web.biz.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * 门店派单同步bashborad 明细
 *
 */
@Data
public class DashBoardShipmentItemDTO implements Serializable {

    private static final long serialVersionUID = -1665035038504532599L;

    /**
     *   货号
     */
    private String materialCode;

    /**
     *   尺码
     */
    private String sizeCode;
    /**
     *   数量
     */
    private Integer qty;

}
