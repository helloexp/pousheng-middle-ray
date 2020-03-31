package com.pousheng.middle.web.excel.acvitity;

import io.terminus.parana.spu.model.SkuTemplate;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/6/12
 */
@Data
public class AcvitityItemResponseBean {

    /**
     * 货号结果集
     */
    private List<SkuTemplate> resultData;

    /**
     * 失败文件下载地址
     */
    private String faildFileUrl;

}
