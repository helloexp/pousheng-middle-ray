package com.pousheng.middle.order.dispatch.component;

import com.pousheng.middle.order.service.AddressGpsReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by songrenfei on 2017/12/25
 */
@Component
@Slf4j
public class ShopAddressComponent {


    @Autowired
    private AddressGpsReadService addressGpsReadService;







}
