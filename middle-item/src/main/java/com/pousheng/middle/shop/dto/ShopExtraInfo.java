package com.pousheng.middle.shop.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pousheng.middle.shop.constant.ShopConstants;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 门店额外信息
 * Created by songrenfei on 2017/12/5
 */
@Data
@Slf4j
public class ShopExtraInfo implements Serializable{

    private static final long serialVersionUID = -4964175963334984847L;
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final TypeReference<List<ShopExpresssCompany>> EXPRESS_COMPANY_LIST = new TypeReference<List<ShopExpresssCompany>>() {};


    //区部Id
    private Long zoneId;
    //区部名称
    private String zoneName;

    //安全库存
    private Integer safeStock;

    //快递信息
    private List<ShopExpresssCompany> expresssCompanyList;

    //服务信息
    private ShopServerInfo shopServerInfo;

    @Getter
    private String expresssCompanyJson;



    public void setExpresssCompanyJson(String expresssCompanyJson){
        this.expresssCompanyJson = expresssCompanyJson;
        if (Strings.isNullOrEmpty(expresssCompanyJson)) {
            this.expresssCompanyList = Lists.newArrayList();
        } else {
            try {
               this.expresssCompanyJson = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(expresssCompanyJson, EXPRESS_COMPANY_LIST);
            } catch (IOException e) {
                log.error("analysis shop express company json:{} fail",expresssCompanyJson);
            }

        }
    }


    public static ShopExtraInfo fromJson(Map<String,String> extraMap){

        if(CollectionUtils.isEmpty(extraMap)){
            log.error("shop extra info json is null");
            return null;
        }

        if(extraMap.containsKey(ShopConstants.SHOP_EXTRA_INFO)){
            log.error("shop extra map not contains key:{}",ShopConstants.SHOP_EXTRA_INFO);
            return null;
        }

        return mapper.fromJson(extraMap.get(ShopConstants.SHOP_EXTRA_INFO),ShopExtraInfo.class);
    }

    public static Map<String,String> putExtraInfo(Map<String,String> extraMap,ShopExtraInfo shopExtraInfo){
        extraMap.put(ShopConstants.SHOP_EXTRA_INFO,mapper.toJson(shopExtraInfo));
        return extraMap;
    }
    
}
