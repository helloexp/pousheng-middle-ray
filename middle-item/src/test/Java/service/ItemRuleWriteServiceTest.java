package service;

import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.manager.ItemRuleManager;
import com.pousheng.middle.group.impl.service.ItemRuleWriteServiceImpl;
import com.pousheng.middle.group.model.ItemRule;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemRuleWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleDao itemRuleDao;
        @MockBean
        private ItemRuleGroupDao itemRuleGroupDao;
        @MockBean
        private ItemRuleShopDao itemRuleShopDao;
        @MockBean
        private ItemGroupDao itemGroupDao;
        @MockBean
        private ShopCacher shopCacher;
        @SpyBean
        private ItemRuleManager itemRuleManager;
        @SpyBean
        private ItemRuleWriteServiceImpl itemRuleWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleWriteServiceTest.MockitoBeans.class;
    }


    ItemRuleDao itemRuleDao;
    ItemRuleWriteServiceImpl itemRuleWriteService;
    ItemRuleManager itemRuleManager;
    ItemRuleGroupDao itemRuleGroupDao;
    ItemRuleShopDao itemRuleShopDao;
    ItemGroupDao itemGroupDao;
    ShopCacher shopCacher;

    @Override
    protected void init() {
        itemRuleDao = get(ItemRuleDao.class);
        itemRuleWriteService = get(ItemRuleWriteServiceImpl.class);
        itemRuleManager = get(ItemRuleManager.class);
        itemRuleShopDao = get(ItemRuleShopDao.class);
        itemRuleGroupDao = get(ItemRuleGroupDao.class);
        itemGroupDao = get(ItemGroupDao.class);
        shopCacher = get(ShopCacher.class);
    }

    private ItemRule mock() {
        return new ItemRule();
    }

    @Test
    public void testCreateSuccess() {
        when(itemRuleDao.create(any())).thenReturn(true);
        Response<Long> response = itemRuleWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }

    @Test
    public void testCreateUnknownEx() {
        when(itemRuleDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemRuleWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemRuleDao.update(any())).thenReturn(true);
        Response<Boolean> response = itemRuleWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(true));
    }


    @Test
    public void testUpdateUnknownEx() {
        when(itemRuleDao.update(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));
    }


    @Test
    public void testDeleteByIdSuccess() {
        when(itemRuleDao.delete(any())).thenReturn(true);
        Response<Boolean> response = itemRuleWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteByIdUnknownEx() {
        when(itemRuleDao.delete(any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemRuleWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.delete.fail"));
    }


    @Test
    public void createWithShopSuccess() {
        when(itemRuleDao.create(any())).thenReturn(true);
        when(itemRuleShopDao.checkShopIds(any(), any())).thenReturn(false);
        Response<Long> response = itemRuleWriteService.createWithShop(Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.TRUE));

    }

    @Test
    public void createWithShopUnknownEx() {
        when(itemRuleDao.create(any())).thenThrow((new NullPointerException()));
        Response<Long> response = itemRuleWriteService.createWithShop(Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.create.fail"));

    }


    @Test
    public void updateShopsSuccess(){
        when(itemRuleShopDao.checkShopIds(any(), any())).thenReturn(false);
        when(itemRuleShopDao.creates(any())).thenReturn(1);
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        Response<Boolean> response = itemRuleWriteService.updateShops(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));

    }

    @Test
    public void updateShopsUnknownEx(){
        when(itemRuleShopDao.checkShopIds(any(), any())).thenReturn(false);
        when(itemRuleShopDao.creates(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleWriteService.updateShops(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));

    }


    @Test
    public void updateGroupsSuccess(){
        when(itemRuleShopDao.creates(any())).thenReturn(1);
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        Response<Boolean> response = itemRuleWriteService.updateGroups(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));

    }


    @Test
    public void updateGroupsUnknownEx(){
        when(itemRuleGroupDao.creates(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleWriteService.updateGroups(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));

    }

    @Test
    public void testDeleteSuccess() {
        when(itemRuleDao.delete(any())).thenReturn(true);
        when(itemRuleGroupDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        Response<Boolean> response = itemRuleWriteService.delete(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteUnknownEx() {
        when(itemRuleDao.delete(any())).thenThrow((new NullPointerException()));
        when(itemRuleGroupDao.deleteByRuleId(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleWriteService.delete(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.delete.fail"));
    }



}


