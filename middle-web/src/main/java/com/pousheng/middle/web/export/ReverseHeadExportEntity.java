package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import com.pousheng.middle.web.utils.export.ExportDateFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author bernie
 * @date 2019/6/12
 */
@Data
public class ReverseHeadExportEntity {

    /**
     * 无头件单号
     */
    @ExportTitle("无头件单号")
    private String headlessNo;
    /**
     * 店铺
     */
    @ExportTitle("店铺")
    private String shop;

    /**
     * 快递单号
     */
    @ExportTitle("快递单")
    private String expressNo;

    /**
     * 平台销售单号
     */
    @ExportTitle("平台销售单号")
    private String platformNo;

    /**
     * 渠道
     */
    @ExportTitle("渠道")
    private String channel;

    /**
     * 单据状态
     */
    @ExportTitle("单据状态")
    private String status;

    /**
     * 手机号码
     */
    @ExportTitle("手机号码")
    private String customer;

    /**
     * 手机号码
     */
    @ExportTitle("手机号码")
    private String phone;

    /**
     * 唯一码
     */
    @ExportTitle("唯一码")
    private String uniqueNo;

    /**
     * 原因
     */
    @ExportTitle("原因")
    private String reason;

    /**
     * 关联ASN:
     */
    @ExportTitle("关联ASN")
    private String relateAsn;

    /**
     * 盘盈入库单
     */
    @ExportTitle("盘盈入库单")
    private String inventoryProfitNo;

    /**
     * 出货方式
     */
    @ExportTitle("出货方式")
    private String shipMode;

    /**
     * 出货快递公司
     */
    @ExportTitle("出货快递公司")
    private String shipCompany;

    /**
     * 出货快递单号
     */
    @ExportTitle("出货快递单号")
    private String shipExpressNo;


    /**
     * 货号：通过goods货号
     */
    @ExportTitle("货号")
    private String goodsNo;

    /**
     * sku信息
     */
    @ExportTitle("sku信息")
    private String skuNo;
    /**
     * 尺码
     */
    @ExportTitle("尺码")
    private String size;


    /**
     * 数量
     */
    @ExportTitle("数量")
    private Integer quantity;
    /**
     * 左脚尺寸
     */
    @ExportTitle("左脚尺寸")
    private String leftSize;
    /**
     * 鞋盒信息
     */
    @ExportTitle("鞋盒信息")
    private String shoeboxInfo;
    /**
     * 实物信息
     */
    @ExportTitle("实物信息")
    private String materialInfo;
    /**
     * 重量
     */
    @ExportTitle("重量")
    private String weight;
    /**
     * 右脚尺寸
     */
    @ExportTitle("右脚尺寸")
    private String rightSize;

    /**
     * 创建时间
     */
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    @ExportTitle("创建时间")
    private Date createdAt;


    /**
     * 无头件关闭时间
     */
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    @ExportTitle("关闭时间")
    private Date closeAt;

    /**
     * 无头件ASN创建时间
     */
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    @ExportTitle("ASN创建时间")
    private Date AsnCreateAt;

}
