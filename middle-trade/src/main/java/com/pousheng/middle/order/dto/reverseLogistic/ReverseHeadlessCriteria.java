package com.pousheng.middle.order.dto.reverseLogistic;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Date: 2019/06/06
 *
 * @author bernie
 */
@Data
public class ReverseHeadlessCriteria extends PagingCriteria implements java.io.Serializable {
    private static final long serialVersionUID = 4282263665769465196L;

    private List<String> statuses;

    /**
     * 状态
     */
    private String status;

    /**
     * 无头件单号
     */
    private String headlessNo;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 店铺
     */
    private String shop;

    /**
     * 快递单号
     */
    private String expressNo;

    /**
     * 平台销售单号
     */
    private String platformNo;

    /**
     * 客户名称
     */
    private String customer;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 唯一码
     */
    private Integer uniqueNo;

    /**
     * 关联ASN:
     */
    private String relateAsn;

    /**
     * 盘盈入库单
     */
    private String inventoryProfitNo;

    /**
     * 查询创建开始时间
     */
    private Date startAt;
    /**
     * 查询创建结束时间
     */
    private Date endAt;

    /**
     * 关闭开始时间
     */
    private Date closeStartAt;

    /**
     * 关闭结束时间
     */
    private Date closeEndAt;

    /**
     * 处理方式
     */
    private String processType;


}
