package com.pousheng.middle.web.order.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.shop.constant.ShopConstants;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.shop.model.Shop;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ShopMaxOrderLogicTest {

    @InjectMocks
    private ShopMaxOrderLogic shopMaxOrderLogic;

    @Mock
    private ShopCacher shopCacher;
    @Mock
    private WarehouseCacher warehouseCacher;

    @Mock
    private JedisTemplate jedisTemplate;
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void checkMaxOrderAcceptQty() {
        Shipment shipment=new Shipment();
        shipment.setShipWay(1);

        Shop shop=new Shop();
        Mockito.when(shopCacher.findShopById(Mockito.anyLong())).thenReturn(shop);
        shopMaxOrderLogic.checkMaxOrderAcceptQty(shipment);

    }

    @Test
    public void getMaxOrderKey() {
        String key=shopMaxOrderLogic.getMaxOrderKey(100L);
        Assert.assertNotNull(key);
    }

    @Test
    public void queryMaxOrder() {
    }

    @Test
    public void asyncPushStock() {
    }

    @Test
    public void isOverMaxOrderAcceptQty() {
    }

    @Test
    public void filterWarehouse() {
    }

    @Test
    public void changeMaxOrderAcceptQtyFlag() {
        Shop shop=new Shop();

        Map<String,String> extraJson= Maps.newHashMap();
        ShopExtraInfo shopExtraInfo=new ShopExtraInfo();
        shopExtraInfo.setCompanyId(200L);

        extraJson.put(ShopConstants.SHOP_EXTRA_INFO, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(shopExtraInfo));
        shop.setExtra(extraJson);

        WarehouseDTO warehouseDTO=new WarehouseDTO();
        when(warehouseCacher.findByOutCodeAndBizId(anyString(),anyString())).thenReturn(warehouseDTO);
        when(jedisTemplate.execute(any(JedisTemplate.JedisAction.class))).thenReturn("1");

        shopMaxOrderLogic.changeMaxOrderAcceptQtyFlag(2,shop);


    }
}