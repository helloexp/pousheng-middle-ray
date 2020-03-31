package com.pousheng.middle.web.excel.supplyRule.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-11 13:57<br/>
 */
@Data
@ApiModel("上传发货限制任务明细")
public class SupplyRuleImportTaskDTO implements Serializable {
    private static final long serialVersionUID = 790562838654063508L;

    /**
     * 主键
     */
    @ApiModelProperty("主键")
    private Long id;

    /**
     * 明细内容
     */
    @ApiModelProperty("明细内容")
    private String message;

    /**
     * 状态
     */
    @ApiModelProperty("状态")
    private String status;

    /**
     * 处理文件路径
     */
    @ApiModelProperty("处理文件路径")
    private String filePath;

    /**
     * 文件名
     */
    @ApiModelProperty("文件名")
    private String fileName;

    /**
     * 增量处理
     */
    @ApiModelProperty("增量处理")
    private Boolean delta;

    /**
     * 处理明细文件路径
     */
    @ApiModelProperty("处理明细文件路径")
    private List<ProcessDetailDTO> processDetails;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    private Date updatedAt;
}
