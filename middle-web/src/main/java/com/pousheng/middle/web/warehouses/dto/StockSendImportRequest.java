package com.pousheng.middle.web.warehouses.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-19 17:46<br/>
 */
@Data
@ApiModel("发货仓导入请求")
public class StockSendImportRequest implements Serializable {
    private static final long serialVersionUID = 39639925112645258L;

    @ApiModelProperty("是否为删除，若为删除为 true，否则为新增")
    private Boolean delete;
    @ApiModelProperty("店铺 Id")
    private List<Long> shopIds;
    @ApiModelProperty("是否全部店铺")
    private Boolean isAll;
    @ApiModelProperty("文件路径")
    private String filePath;
}
