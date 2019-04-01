package com.pousheng.middle.web.shop;

import com.google.common.base.Optional;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopBusinessInfo;
import com.pousheng.middle.shop.dto.ShopBusinessTime;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.web.shop.component.ShopBusinessLogic;
import com.pousheng.middle.web.shop.dto.OrderExpireInfo;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class AdminShopsTest {

    private AdminShops api = Mockito.spy(AdminShops.class);
    private ShopReadService shopReadService = Mockito.mock(ShopReadService.class);
    private ShopWriteService shopWriteService = Mockito.mock(ShopWriteService.class);
    private PsShopReadService psShopReadService = Mockito.mock(PsShopReadService.class);
    private ShopCacher shopCacher  = Mockito.mock(ShopCacher.class);
    private MiddleShopCacher middleShopCacher  = Mockito.mock(MiddleShopCacher.class);
    private OrderShipmentReadService orderShipmentReadService  = Mockito.mock(OrderShipmentReadService.class);
    private Shop shop;
    private ShopBusinessInfo shopBusinessInfo;
    private ShopBusinessTime shopBusinessTime;
    private ShopBusinessLogic shopBusinessLogic = new ShopBusinessLogic();

    @Before
    public void init(){
        api.setShopReadService(shopReadService);
        api.setShopWriteService(shopWriteService);
        api.setPsShopReadService(psShopReadService);
        api.setShopBusinessLogic(shopBusinessLogic);
        api.setShopCacher(shopCacher);
        api.setMiddleShopCacher(middleShopCacher);
        api.setOrderShipmentReadService(orderShipmentReadService);

        shopBusinessTime = new ShopBusinessTime();
        shopBusinessTime.setOpeningStatus(1);

        shopBusinessTime.setOpeningStatusMon(1);
        shopBusinessTime.setOpeningStartTimeMon("10:00");
        shopBusinessTime.setOpeningEndTimeMon("20:00");
        shopBusinessTime.setOrderTimeout(120);
        shopBusinessTime.setOrderEmailTimeout(90);


        shopBusinessTime.setOpeningStatusTue(1);
        shopBusinessTime.setOpeningStartTimeTue("10:00");
        shopBusinessTime.setOpeningEndTimeTue("20:00");

        shopBusinessTime.setOpeningStatusWed(1);
        shopBusinessTime.setOpeningStartTimeWed("10:00");
        shopBusinessTime.setOpeningEndTimeWed("20:00");
        shopBusinessTime.setOrderAcceptQtyMax(1);

        shopBusinessInfo = new ShopBusinessInfo();
        shopBusinessInfo.setId(109l);
        shopBusinessInfo.setType(1);
        shopBusinessInfo.setBusinessId(200l);
        shopBusinessInfo.setOutId("SC110006");
        shopBusinessInfo.setShopBusinessTime(shopBusinessTime);

        shop = new Shop();
        shop.setId(109l);
        shop.setType(1);
        shop.setOuterId("SC110006");
        shop.setBusinessId(200l);
        ShopExtraInfo shopExtraInfo = new ShopExtraInfo();
        shopExtraInfo.setShopBusinessTime(shopBusinessTime);
        shopExtraInfo.setOpenShopId(200L);
        String jsonStr = JsonMapper.nonEmptyMapper().toJson(shopExtraInfo);
        Map<String,String> map = new HashMap<String,String>();
        map.put("shopExtraInfo",jsonStr);
        shop.setExtra(map);
    }


    @Test
    public void getShopBusinessInfo() {
        long shopId = 109l;
        Response<Shop> response = new Response<Shop>();
        response.setResult(shop);
        response.setSuccess(true);
        when(shopReadService.findById(any())).thenReturn(response);
        ShopBusinessInfo info = api.getShopBusinessInfo(shopId);
        assertNotNull(info);
        Assert.assertThat(info,equalTo(shopBusinessInfo));
    }

    @Test
    public void getShopBusinessInfoByOuterIdAndBusinessId() {
        String outerId = "SC110006";
        Long businessId = 200l;

        Response<Optional<Shop>> response = new Response<Optional<Shop>>();
        response.setResult(Optional.fromNullable(shop));
        response.setSuccess(true);

        when(psShopReadService.findByOuterIdAndBusinessId(any(),any())).thenReturn(response);
        ShopBusinessInfo info = api.getShopBusinessInfoByOuterIdAndBusinessId(outerId,businessId);
        assertNotNull(info);
        Assert.assertThat(info,equalTo(shopBusinessInfo));
    }

    @Test
    public void getShopOrderExpireTime() {
        String outerId = "SC110006";
        Long businessId = 200l;

        Response<Optional<Shop>> response = new Response<Optional<Shop>>();
        response.setResult(Optional.fromNullable(shop));
        response.setSuccess(true);

        LocalDateTime orderDateTime = LocalDateTime.of(LocalDate.of(2018,5,14),LocalTime.of(10,10));
        when(psShopReadService.findByOuterIdAndBusinessId(any(),any())).thenReturn(response);
        OrderExpireInfo orderExpireInfo = api.getShopOrderExpireTime(outerId,businessId,orderDateTime);
        Assert.assertThat(orderExpireInfo,equalTo(LocalDateTime.of(LocalDate.of(2018,5,14),LocalTime.of(12,10))));
    }

    @Test
    public void updateShopBusinessInfo() {

        long shopId = 107l;

        Response<Shop> findResp = new Response<Shop>();
        findResp.setResult(shop);
        findResp.setSuccess(true);

        Response<Boolean> updateResp = new Response();
        updateResp.setSuccess(true);
        updateResp.setResult(true);

        when(shopReadService.findById(any())).thenReturn(findResp);
        when(shopWriteService.update(any())).thenReturn(updateResp);

        doNothing().when(shopCacher).refreshShopById(any());
        doNothing().when(middleShopCacher).refreshByOuterIdAndBusinessId(any(),any());

        Assert.assertTrue(api.updateShopBusinessInfo(shopId,shopBusinessInfo).getResult());
    }

    @Test
    public void getShopCurrentOpeningStatusTest(){
        String outerId = "SC110006";
        Long businessId = 200l;

        Response<Optional<Shop>> response = new Response<Optional<Shop>>();
        response.setResult(Optional.fromNullable(shop));
        response.setSuccess(true);

        when(psShopReadService.findByOuterIdAndBusinessId(any(),any())).thenReturn(response);
        when(orderShipmentReadService.countByShopId(any())).thenReturn(Response.ok(0));
        boolean result = api.getShopCurrentStatus(outerId,businessId);
        Assert.assertTrue(result);
    }
}
