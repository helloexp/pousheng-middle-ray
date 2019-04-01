package com.pousheng.middle.warehouse.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 仓库模型转换成DTO
 *
 * @auther feisheng.ch
 * @time 2018/5/17
 */
@Data
public class WarehouseDTO {

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private Integer warehouseSubType;
    private String companyId;
    private String companyName;
    private String outCode;
    private Integer isMpos;
    private Integer status;

    private String warehouseCode;
    private String warehouseName;
    private Integer warehouseType;
    private Integer priority;
    private String address;
    private Integer isDefault;
    private String divisionId;
    private String regionId;
    private Date createdAt;
    private Date updatedAt;

    private Long id;
    private Integer tenantId;
    private String extraJson;
    private Map<String, String> extra;

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            try {
                this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
            } catch (IOException e) {
                // ignore
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取公司编码
     *
     * @return 公司编码
     */
    public String getCompanyCode(){
        if(StringUtils.hasText(getWarehouseCode())) {
            return Splitter.on('-').omitEmptyStrings().trimResults().limit(2).splitToList(getWarehouseCode()).get(0);
        }
        return null;
    }

    /**
     * 获取仓库内码
     *
     * @return 仓库内码
     */
    public String getInnerCode() {
        if (StringUtils.hasText(getWarehouseCode())) {
            return Splitter.on('-').omitEmptyStrings().trimResults().limit(2).splitToList(getWarehouseCode()).get(1);
        }
        return null;
    }

    /**
     * 获取获取安全库存
     *
     * @return
     */
    public String getSafeQuantity() {
        Map<String, String>  extra = this.getExtra();
        if(CollectionUtils.isEmpty(extra)
                ||!extra.containsKey("safeQuantity")
                ){
            return "";
        }
        try {
            return extra.get("safeQuantity");
        } catch (Exception e) {
            return "";
        }
    }

    public static final class JacksonType {

        public static final TypeReference<Map<String, String>> MAP_OF_STRING = new TypeReference<Map<String,String>>(){};

    }

}
