package com.pousheng.middle.open.stock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/9/13
 */
@Component
public class StockPusherClient {
    @Autowired
    private ShopStockPusher stockPusher;
    @Autowired
    private YjJitStockPusher yjJitStockPusher;

    public void submit(List<String> skuCodes){
        //按店铺推送前端店铺(官网、天猫、京东、苏宁、咕咚)
        stockPusher.push(skuCodes);
        //按仓库推送前端店铺（云聚Jit）
        yjJitStockPusher.push(skuCodes);
    }
}
