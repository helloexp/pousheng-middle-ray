package com.pousheng.middle.warehouse.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Description: 店铺商品供货规则
 * User: support 9
 * Date: 2018/9/13
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
public class ShopSkuSupplyRule implements Serializable {

    private static final long serialVersionUID = 7335099172438973976L;

    /**
     * id
     */
    private Long id;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 货品条码
     */
    private String skuCode;

    /**
     * 货品名称
     */
    private String skuName;

    /**
     * 货号
     */
    private String materialCode;

    /**
     * 限制类型
     */
    private String type;

    /**
     * 规则状态
     */
    private String status;

    private Date createdAt;

    private Date updatedAt;

}
