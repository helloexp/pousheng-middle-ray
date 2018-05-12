package com.pousheng.middle.shop.api;

import com.google.common.eventbus.EventBus;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.AbstractRestApiTest;
import com.pousheng.middle.order.model.ZoneContract;
import com.pousheng.middle.order.service.ZoneContractReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.web.shop.AdminShops;
import com.pousheng.middle.web.shop.cache.ShopChannelGroupCacher;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.event.listener.CreateOpenShopRelationListener;
import com.pousheng.middle.web.user.component.ParanaUserOperationLogic;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import io.terminus.open.client.parana.item.SyncParanaShopService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.user.ext.UserTypeBean;
import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/12
 * Time: 下午1:41
 */
@Setter
public class AdminShopTests extends AbstractRestApiTest {


    @Configuration
    @Getter
    public static class MockitoBeans {
        @MockBean
        private ZoneContractReadService zoneContractReadService;
        @MockBean
        private PsShopReadService psShopReadService;
        @MockBean
        private SyncParanaShopService syncParanaShopService;
        @MockBean
        private UcUserOperationLogic ucUserOperationLogic;
        @MockBean
        private EventBus eventBus;
        @MockBean
        private ShopCacher shopCacher;
        @MockBean
        private ParanaUserOperationLogic paranaUserOperationLogic;
        @MockBean
        private MposWarehousePusher mposWarehousePusher;
        @MockBean
        private MemberShopOperationLogic memberShopOperationLogic;
        @MockBean
        private MiddleShopCacher middleShopCacher;
        @MockBean
        private UserTypeBean userTypeBean;
        @MockBean
        private ShopChannelGroupCacher shopChannelGroupCacher;
        @MockBean
        private CreateOpenShopRelationListener createOpenShopRelationListener;
        @SpyBean
        private AdminShops api;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    ZoneContractReadService zoneContractReadService;
    PsShopReadService psShopReadService;
    AdminShops api;
    Shop shop = new Shop();
    ZoneContract zoneContract1 = new ZoneContract();
    ZoneContract zoneContract2 = new ZoneContract();


    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        super.init();
    }

    @Before
    public void make(){
        shop.setId(109L);
        shop.setZoneId("1");
        shop.setOuterId("SC110006");
        shop.setBusinessId(200L);

        zoneContract1.setId(1L);
        zoneContract1.setZoneId("1");
        zoneContract1.setEmail("aaa@a.com");
        zoneContract1.setStatus(1);

        zoneContract1.setId(2L);
        zoneContract1.setZoneId("2");
        zoneContract1.setEmail("bbb@b.com");
        zoneContract1.setStatus(1);
    }

    @Test
    public void testZoneEmail() {
        List<String> list = api.findEmailList("SC110006",200L);
        System.out.print(list.size());
    }

}
