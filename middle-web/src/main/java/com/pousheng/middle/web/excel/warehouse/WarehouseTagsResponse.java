package com.pousheng.middle.web.excel.warehouse;

import lombok.Data;

import java.util.List;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/6/6
 */
@Data
public class WarehouseTagsResponse {

    private List<WarehouseTagsFaildBean> faildBeans;

    private boolean success = true;

    private String errorMsg;
}
