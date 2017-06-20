package com.pousheng.middle.web.shop;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by songrenfei on 2017/6/19
 */
@Slf4j
@RestController
@RequestMapping("/api/shops")
public class MiddleShops {


    @RpcConsumer
    private ShopWriteService shopWriteService;

    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createShop(@RequestParam String shopJson) {

        Shop shop = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shopJson,Shop.class);

        Response<Long> response = shopWriteService.create(shop);
        if(!response.isSuccess()){
            log.error("create shop:{} fail,error:{}",shop,response.getError());
        }

        return response.getResult();

    }
}
