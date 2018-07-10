package manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.manager.ItemRuleManager;
import com.pousheng.middle.group.model.ItemGroupSku;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import service.AbstractServiceTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
public class ItemRuleManagerTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private ItemRuleManager itemRuleManager;
        @MockBean
        private ItemRuleDao itemRuleDao;
        @MockBean
        private ItemRuleGroupDao itemRuleGroupDao;
        @MockBean
        private ItemRuleShopDao itemRuleShopDao;


    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleManagerTest.MockitoBeans.class;
    }

    private ItemRuleManager itemRuleManager;

    private ItemRuleDao itemRuleDao;

    private ItemRuleGroupDao itemRuleGroupDao;

    private ItemRuleShopDao itemRuleShopDao;

    @Override
    protected void init() {
        itemRuleDao = get(ItemRuleDao.class);
        itemRuleGroupDao=get(ItemRuleGroupDao.class);
        itemRuleShopDao=get(ItemRuleShopDao.class);
        itemRuleManager = get(ItemRuleManager.class);
    }

    private ItemGroupSku mock() {
        return new ItemGroupSku().id(1L).skuCode("1").type(1);
    }


    @Test
    public void testCreateWithShopsSuccess() {
        when(itemRuleDao.create(any())).thenReturn(true);
        when(itemRuleShopDao.creates(any())).thenReturn(1);
        Response<Long> response = itemRuleManager.createWithShops(Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }

    @Test
    public void testCreateWithUnknownEx() {
        when(itemRuleDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemRuleManager.createWithShops(Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.create.fail"));
    }

    @Test
    public void testCreatesWithUnknownEx() {
        when(itemRuleDao.create(any())).thenReturn(true);
        when(itemRuleShopDao.creates(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemRuleManager.createWithShops(Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.create.fail"));
    }

    @Test
    public void testUpdateShopsWithDeleteFail() {
        when(itemRuleShopDao.deleteByRuleId(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleManager.updateShops(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));
    }

    @Test
    public void testUpdateShopsWithCreatesFail() {
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleShopDao.creates(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleManager.updateShops(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));
    }

    @Test
    public void testUpdateShopsSuccess() {
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleShopDao.creates(any())).thenReturn(1);
        Response<Boolean> response = itemRuleManager.updateShops(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testUpdateGroupsWithDeleteFail() {
        when(itemRuleGroupDao.deleteByRuleId(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleManager.updateGroups(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));
    }

    @Test
    public void testUpdateGroupsWithCreatesFail() {
        when(itemRuleGroupDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleGroupDao.creates(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleManager.updateGroups(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.update.fail"));
    }

    @Test
    public void testUpdateGroupsSuccess() {
        when(itemRuleGroupDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleGroupDao.creates(any())).thenReturn(1);
        Response<Boolean> response = itemRuleManager.updateGroups(1L,Lists.newArrayList(1L));
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }

    @Test
    public void testDeleteWithUnknownEx(){
        when(itemRuleDao.delete(any())).thenThrow(new NullPointerException());;
        Response<Boolean> response = itemRuleManager.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.delete.fail"));
    }

    @Test
    public void testDeleteGroupFail(){
        when(itemRuleDao.delete(any())).thenReturn(true);
        when(itemRuleGroupDao.deleteByRuleId(any())).thenThrow(new NullPointerException());;
        Response<Boolean> response = itemRuleManager.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.delete.fail"));
    }

    @Test
    public void testDeleteShopFail(){
        when(itemRuleDao.delete(any())).thenReturn(true);
        when(itemRuleGroupDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleShopDao.deleteByRuleId(any())).thenThrow(new NullPointerException());;
        Response<Boolean> response = itemRuleManager.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.delete.fail"));
    }

    @Test
    public void testDeleteSuccess(){
        when(itemRuleDao.delete(any())).thenReturn(true);
        when(itemRuleGroupDao.deleteByRuleId(any())).thenReturn(1);
        when(itemRuleShopDao.deleteByRuleId(any())).thenReturn(1);
        Response<Boolean> response = itemRuleManager.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }
}
