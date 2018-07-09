package com.pousheng.middle.web.item.group;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.dto.ItemRuleDetail;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.model.ItemRule;
import com.pousheng.middle.group.service.*;
import com.pousheng.middle.web.AbstractRestApiTest;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import com.pousheng.middle.web.shop.cache.ShopChannelGroupCacher;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/14
 */
@Setter
public class ItemRulesTest extends AbstractRestApiTest {


    @Configuration
    @Getter
    public static class MockitoBeans {
        @MockBean
        private ItemRuleWriteService itemRuleWriteService;
        @MockBean
        private ItemRuleReadService itemRuleReadService;
        @MockBean
        private ItemRuleShopReadService itemRuleShopReadService;
        @MockBean
        private ItemRuleGroupReadService itemRuleGroupReadService;
        @MockBean
        private OpenShopReadService openShopReadService;
        @MockBean
        private ItemGroupReadService itemGroupReadService;
        @MockBean
        private ShopChannelGroupCacher shopChannelGroupCacher;
        @MockBean
        private OpenShopCacher openShopCacher;
        @MockBean
        private GroupRuleCacherProxy GroupRuleCacherProxy;
        @SpyBean
        private ItemRules api;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRulesTest.MockitoBeans.class;
    }

    private ItemRuleWriteService itemRuleWriteService;

    private ItemRuleReadService itemRuleReadService;

    private ItemRuleShopReadService itemRuleShopReadService;

    private ItemRuleGroupReadService itemRuleGroupReadService;

    private ItemGroupReadService itemGroupReadService;

    private ShopChannelGroupCacher shopChannelGroupCacher;

    private OpenShopCacher openShopCacher;

    private GroupRuleCacherProxy GroupRuleCacherProxy;

    private ItemRules api;

    private ItemRule rule = new ItemRule();



    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        super.init();
        api.setItemRuleWriteService(itemRuleWriteService);
        api.setItemRuleReadService(itemRuleReadService);
        api.setItemRuleShopReadService(itemRuleShopReadService);
        api.setItemRuleGroupReadService(itemRuleGroupReadService);
        api.setItemGroupReadService(itemGroupReadService);
        api.setShopChannelGroupCacher(shopChannelGroupCacher);
        api.setOpenShopCacher(openShopCacher);
        api.setGroupRuleCacherProxy(GroupRuleCacherProxy);
        when(itemRuleWriteService.createWithShop(any())).thenReturn(Response.ok(1L));
        when(itemRuleWriteService.updateShops(any(), any())).thenReturn(Response.ok(true));
        when(itemRuleWriteService.updateGroups(any(), any())).thenReturn(Response.ok(true));
        when(itemRuleWriteService.delete(any())).thenReturn(Response.ok(true));
        when(itemRuleShopReadService.findByRuleId(any())).thenReturn(Response.ok(Lists.newArrayList()));
        when(itemRuleGroupReadService.findByRuleId(any())).thenReturn(Response.ok(Lists.newArrayList()));
        when(itemRuleShopReadService.checkShopIds(any(),any())).thenReturn(Response.ok(false));
    }


    @Test
    public void testCreateSuccess() {
        Long id = api.create(new Long[]{1L, 2L});
        assertThat(id, is(1L));
    }

    @Test
    public void testCreateFail() {
        when(itemRuleShopReadService.checkShopIds(any(),any())).thenReturn(Response.ok(true));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("shop.belong.to.other.rule");
        api.create(new Long[]{1L, 2L});
    }

    @Test
    public void testUpdateGroupSuccess() {
        Boolean result = api.updateGroup(1L, new Long[]{1L, 2L});
        assertThat(result, is(true));
    }

    @Test
    public void testUpdateShopSuccess() {
        Boolean result = api.updateShop(1L, new Long[]{1L, 2L});
        assertThat(result, is(true));
    }

    @Test
    public void testDeleteSuccess() {
        Boolean result = api.delete(1L);
        assertThat(result, is(true));
    }

    @Test
    public void testPagingSuccess() {
        when(itemRuleReadService.paging(any())).thenReturn(Response.ok(new Paging<>(1L,Lists.newArrayList(rule))));
        Paging<ItemRuleDetail> result = api.findBy(null, null, null);
        assertThat(result.getData().size(),is(1));
        assertThat(result.getTotal(),is(1L));
    }
}
