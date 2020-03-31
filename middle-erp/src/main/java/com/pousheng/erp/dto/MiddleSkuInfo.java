package com.pousheng.erp.dto;

import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/15
 * Time: 下午1:41
 */
@Data
public class MiddleSkuInfo implements Serializable {
    private static final long serialVersionUID = 60812006693829969L;

    private SkuTemplate skuTemplate;

    private Spu spu;
}