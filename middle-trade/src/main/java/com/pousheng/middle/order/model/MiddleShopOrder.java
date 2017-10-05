package com.pousheng.middle.order.model;

import io.terminus.parana.order.model.ShopOrder;
import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/9/30
 * pousheng-middle
 */
@Data
public class MiddleShopOrder extends ShopOrder implements java.io.Serializable{
    private static final long serialVersionUID = 5248019685105628893L;
    private String mobile;


}
