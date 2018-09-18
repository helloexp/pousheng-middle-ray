package com.pousheng.middle.web.erp;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.web.AbstractRestApiTest;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.shop.model.Shop;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author zhaoxw
 * @date 2018/5/16
 */
@Setter
public class FireCallTest extends AbstractRestApiTest {

    @Configuration
    @Getter
    public static class MockitoBeans {
        @MockBean
        private MiddleShopCacher middleShopCacher;
        @MockBean
        private GroupRuleCacherProxy GroupRuleCacherProxy;
        @MockBean
        private MappingReadService mappingReadService;
        @MockBean
        private SkuTemplateSearchReadService skuTemplateSearchReadService;
        @SpyBean
        private FireCall api;
        @MockBean
        private DateTimeFormatter dft;
        @MockBean
        private SpuImporter spuImporter;
        @MockBean
        private BrandImporter brandImporter;
        @MockBean
        private MaterialPusher materialPusher;
        @MockBean
        private ErpBrandCacher brandCacher;
        @MockBean
        private MposWarehousePusher mposWarehousePusher;
        @MockBean
        private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
        @MockBean
        private ShopCacher shopCacher;
        @MockBean
        private WarehouseCacher warehouseCacher;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return FireCallTest.MockitoBeans.class;
    }

    private MiddleShopCacher middleShopCacher;

    private GroupRuleCacherProxy GroupRuleCacherProxy;

    private SkuTemplateSearchReadService skuTemplateSearchReadService;


    private MappingReadService mappingReadService;

    private FireCall api;

    private String templateName = "ps_search.mustache";

    private WarehouseCacher warehouseCacher;

    private DateTimeFormatter dft;

    private SpuImporter spuImporter;

    private BrandImporter brandImporter;


    private MaterialPusher materialPusher;

    private ErpBrandCacher brandCacher;

    private ShopCacher shopCacher;

    private MposWarehousePusher mposWarehousePusher;

    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;



    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        super.init();
        api.setMiddleShopCacher(middleShopCacher);
        api.setGroupRuleCacherProxy(GroupRuleCacherProxy);
        api.setSkuTemplateSearchReadService(skuTemplateSearchReadService);
        api.setMappingReadService(mappingReadService);
    }

    @Test
    public void testSearchSizeSuccess() {
        Shop currentShop = new Shop();
        currentShop.setId(195L);
        Map<String, String> map = new HashMap<>();
        map.put("shopExtraInfo", "{\"shopInnerCod\":\"200000418\",\"companyId\":200,\"safeStock\":8,\"openShopId\":262,\"email\":\"SP110950@pousheng.com\"}");
        currentShop.setExtra(map);
        when(middleShopCacher.findByOuterIdAndBusinessId(any(), any())).thenReturn(currentShop);
        when(GroupRuleCacherProxy.findByShopId(any())).thenReturn(Lists.newArrayList(20L));
        Response<SearchedItemWithAggs<SearchSkuTemplate>> result = Response.ok(new SearchedItemWithAggs<>());
        result.getResult().setEntities(new Paging<>());
        Map<String, String> params = Maps.newHashMap();
        params.put("spuCode", "087470");
        params.put("groupIds", "20");
        when(skuTemplateSearchReadService.searchWithAggs(any(), any(), any(), any(), any()))
                .thenAnswer((Answer) invocation -> {
                    //获得函数调用的参数
                    return result;
                });
        api.searchSize("087470", null,200L, "SP110950");
        verify(skuTemplateSearchReadService, times(1)).searchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);


    }

    @Test
    public void testSearchSizeFailWithWrongShop() {
        Shop currentShop = new Shop();
        currentShop.setId(195L);
        Map<String, String> map = new HashMap<>();
        map.put("shopExtraInfo", "{\"shopInnerCod\":\"200000418\",\"companyId\":200,\"safeStock\":8,\"email\":\"SP110950@pousheng.com\"}");
        currentShop.setExtra(map);
        when(middleShopCacher.findByOuterIdAndBusinessId(any(), any())).thenReturn(currentShop);
        when(GroupRuleCacherProxy.findByShopId(any())).thenReturn(Lists.newArrayList(20L));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("shop.not.mapping.open.shop");
        api.searchSize("087470",null, 200L, "SP110950");

    }

    @Test
    public void testSearchSizeFailWithoutRule() {
        Shop currentShop = new Shop();
        currentShop.setId(195L);
        Map<String, String> map = new HashMap<>();
        map.put("shopExtraInfo", "{\"shopInnerCod\":\"200000418\",\"companyId\":200,\"safeStock\":8,\"openShopId\":262,\"email\":\"SP110950@pousheng.com\"}");
        currentShop.setExtra(map);
        when(middleShopCacher.findByOuterIdAndBusinessId(any(), any())).thenReturn(currentShop);
        when(GroupRuleCacherProxy.findByShopId(any())).thenReturn(Lists.newArrayList());
        api.searchSize("087470",null, 200L, "SP110950");
        Paging<SearchSkuTemplate> paging = api.searchSize("087470", null,200L, "SP110950");
        assertThat(paging.getData(), is(Lists.newArrayList()));
    }
}
