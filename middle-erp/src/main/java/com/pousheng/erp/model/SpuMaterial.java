package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author: jlchen
 * Desc: spu与material_id的关联Model类
 * Date: 2017-06-23
 */
@Data
public class SpuMaterial implements Serializable {

    private static final long serialVersionUID = 2831862307175992170L;
    private Long id;
    
    /**
     * spu id
     */
    private Long spuId;
    
    /**
     * 货品id
     */
    private Long materialId;
    
    /**
     * 货品编码
     */
    private String materialCode;
    
    /**
     * 创建时间
     */
    private Date createdAt;
}
