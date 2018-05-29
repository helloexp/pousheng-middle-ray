package com.pousheng.middle.warehouse.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.StockDto;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


@Data
public class SkuStockUpdated implements Serializable {

    private static final long serialVersionUID = 4291293525147003215L;

    private Long id;

    private Long taskId;

    private String skuCode;

    private Date createdAt;

    private Date updatedAt;

}
