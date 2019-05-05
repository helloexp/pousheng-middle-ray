package com.pousheng.middle.web.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Xiongmin
 * 2019/4/28
 */
@Data
public class OrderFetchDTO implements Serializable {
    private static final long serialVersionUID = 4525097561304448225L;

    private Integer pageNo;

    private Integer pageSize;

    private Long openShopId;

    private Date startTime;

    private Date endTime;

    private String orderFetchType;
}
