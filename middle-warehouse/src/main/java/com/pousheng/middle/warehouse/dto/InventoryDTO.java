package com.pousheng.middle.warehouse.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: 库存表Model类
 * Date: 2018-05-14
 */
@Data
public class InventoryDTO implements Serializable {


    private static final long serialVersionUID = 1223331500850644764L;

    protected static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private Long warehouseId;

    private Long id;
    private String entityId;
    private Integer entityType;
    private String warehouseCode;
    private Integer warehouseType;
    private Long realQuantity;
    private Long safeQuantity;
    private Long preorderQuantity;
    private Long withholdQuantity;
    private Long occupyQuantity;
    private Integer status;
    private String skuCode;
    private Integer version;
    private Date createdAt;
    private Date updatedAt;
    protected String extraJson;
    protected Map<String, String> extra;

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            try {
                this.extra = (Map)objectMapper.readValue(extraJson, InventoryDTO.JacksonType.MAP_OF_STRING);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra != null && !extra.isEmpty()) {
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        } else {
            this.extraJson = null;
        }

    }

    public static final class JacksonType {
        public static final TypeReference<Map<String, String>> MAP_OF_STRING = new TypeReference<Map<String, String>>() {
        };
        public JacksonType() {
        }
    }

    public Long getAvailStock() {
        return getRealQuantity()-getOccupyQuantity()-getSafeQuantity();
    }

    public Long getRealQuantity() {
        return null==realQuantity?0:realQuantity;
    }

    public Long getSafeQuantity() {
        return null==safeQuantity?0:safeQuantity;
    }

    public Long getPreorderQuantity() {
        return null==preorderQuantity?0:preorderQuantity;
    }

    public Long getWithholdQuantity() {
        return null==withholdQuantity?0:withholdQuantity;
    }

    public Long getOccupyQuantity() {
        return null==occupyQuantity?0:occupyQuantity;
    }
}
