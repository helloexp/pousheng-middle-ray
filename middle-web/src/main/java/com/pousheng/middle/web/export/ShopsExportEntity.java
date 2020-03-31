package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import lombok.Data;

@Data
public class ShopsExportEntity {

    /**
     * 区部名称
     */
    @ExportTitle("区部名称")
    private String zoneName;

    /**
     * 门店账套
     */
    @ExportTitle("门店账套")
    private String businessId;

    /**
     * 门店外码
     */
    @ExportTitle("门店外码")
    private String outId;

    /**
     * 门店名称
     */
    @ExportTitle("门店名称")
    private String shopName;

    /**
     * 门店账户
     */
    @ExportTitle("门店账户")
    private String shopAccount;

    /**
     * 门店手机号
     */
    @ExportTitle("门店手机号")
    private String shopTelephone;

    /**
     * 门店邮箱
     */
    @ExportTitle("门店邮箱")
    private String shopEmail;

    /**
     * 门店地址
     */
    @ExportTitle("门店地址")
    private String shopAddress;

    /**
     * 门店类型
     */
    @ExportTitle("门店类型")
    private String shopType;

    /**
     * 门店最大接单量
     */
    @ExportTitle("门店最大接单量")
    private String shopOrderAcceptMax;

    /**
     * 快递公司
     */
    @ExportTitle("快递公司")
    private String expressCompany;

    /**
     * 是否必发货
     */
    @ExportTitle("是否必发货")
    private String notReject;

    /**
     * 门店状态
     */
    @ExportTitle("门店状态")
    private String shopStatus;
    
    /**
     * 创建时间
     */
    @ExportTitle("创建时间")
    private String createdAt;
    
    /**
     * 更新时间
     */
    @ExportTitle("更新时间")
    private String updatedAt;
}
