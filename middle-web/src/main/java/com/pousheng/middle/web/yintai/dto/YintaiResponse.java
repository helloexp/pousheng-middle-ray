package com.pousheng.middle.web.yintai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/6/25
 */
@Data
public class YintaiResponse implements Serializable {
    private static final long serialVersionUID = 1673217862801433921L;

    private List<YintaiBrand> brandList;

    private List<YintaiShop> shopList;

}
