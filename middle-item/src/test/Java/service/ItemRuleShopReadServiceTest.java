package service;

import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.service.ItemRuleShopReadServiceImpl;
import com.pousheng.middle.group.model.ItemRuleShop;
import io.terminus.common.model.Response;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemRuleShopReadServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleShopDao itemRuleShopDao;
        @SpyBean
        private ItemRuleShopReadServiceImpl itemRuleShopReadService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleShopReadServiceTest.MockitoBeans.class;
    }


    ItemRuleShopDao itemRuleShopDao;
    ItemRuleShopReadServiceImpl itemRuleShopReadService;

    @Override
    protected void init() {
        itemRuleShopDao = get(ItemRuleShopDao.class);
        itemRuleShopReadService = get(ItemRuleShopReadServiceImpl.class);

    }

    private ItemRuleShop mock() {
        return new ItemRuleShop().ruleId(1L).shopId(1L);
    }

    @Test
    public void testFindByIdSuccess() {
        when(itemRuleShopDao.findById((Long) any())).thenReturn(mock());
        Response<ItemRuleShop> response = itemRuleShopReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(mock()));
    }

    @Test
    public void testFindByIdFail() {
        when(itemRuleShopDao.findById((Long) any())).thenThrow(new NullPointerException());
        Response<ItemRuleShop> response = itemRuleShopReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }


    @Test
    public void testPagingFail() {
        when(itemRuleShopDao.findById((Long) any())).thenThrow(new NullPointerException());
        Response<ItemRuleShop> response = itemRuleShopReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }


    @Test
    public void testFindByRuleIdFail() {
        when(itemRuleShopDao.findByRuleId(any())).thenThrow(new NullPointerException());
        Response<List<ItemRuleShop>> response = itemRuleShopReadService.findByRuleId(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }


    @Test
    public void testFindByRuleIdSuccess() {
        List<ItemRuleShop> list = Lists.newArrayList();
        when(itemRuleShopDao.findByRuleId(any())).thenReturn(list);
        Response<List<ItemRuleShop>> response = itemRuleShopReadService.findByRuleId(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(list));
    }


    @Test
    public void testFindShopIdsFail() {
        when(itemRuleShopDao.findShopIds()).thenThrow(new NullPointerException());
        Response<List<Long>> response = itemRuleShopReadService.findShopIds();
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }


    @Test
    public void testFindShopIdsSuccess() {
        List<Long> list = Lists.newArrayList();
        when(itemRuleShopDao.findShopIds()).thenReturn(list);
        Response<List<Long>> response = itemRuleShopReadService.findShopIds();
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(list));
    }

    @Test
    public void testCheckShopIds(){
        when(itemRuleShopDao.checkShopIds(any(),any())).thenReturn(true);
        Response<Boolean> response =itemRuleShopReadService.checkShopIds(1L,Lists.newArrayList());
        assertThat(response.isSuccess(),is(Boolean.TRUE));
        assertThat(response.getResult(),is(Boolean.TRUE));
    }

    @Test
    public void tesCheckShopIdsFail() {
        when(itemRuleShopDao.checkShopIds(any(),any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleShopReadService.checkShopIds(1L,Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }


    @Test
    public void testFindRuleIdByShopIdSuccess(){
        when(itemRuleShopDao.findRuleIdByShopId(any())).thenReturn(1L);
        Response<Long> response =itemRuleShopReadService.findRuleIdByShopId(1L);
        assertThat(response.isSuccess(),is(Boolean.TRUE));
        assertThat(response.getResult(),is(1L));
    }

    @Test
    public void testFindRuleIdByShopIdFail() {
        when(itemRuleShopDao.findRuleIdByShopId(any())).thenThrow(new NullPointerException());
        Response<Long> response =itemRuleShopReadService.findRuleIdByShopId(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.find.fail"));
    }

}


