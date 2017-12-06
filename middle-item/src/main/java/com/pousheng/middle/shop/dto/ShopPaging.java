package com.pousheng.middle.shop.dto;

import io.terminus.parana.shop.model.Shop;
import lombok.Data;

import java.io.Serializable;

/**
 * 门店分页信息
 * Created by songrenfei on 2017/12/6
 */
@Data
public class ShopPaging implements Serializable{


    private static final long serialVersionUID = 4844750041205171228L;

    private Shop shop;

    private ShopExtraInfo shopExtraInfo;

}
