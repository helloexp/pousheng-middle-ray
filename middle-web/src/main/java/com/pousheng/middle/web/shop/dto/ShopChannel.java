package com.pousheng.middle.web.shop.dto;

import io.terminus.open.client.common.shop.dto.OpenClientShop;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2018/3/27
 */
@Data
public class ShopChannel implements Serializable{

    private static final long serialVersionUID = 1681180986855125363L;

    private OpenClientShop openClientShop;

    private Long zoneId;

    //区部名称
    private String zoneName;
    //区部下的店铺
    private List<OpenClientShop> zoneOpenClientShops;


}
