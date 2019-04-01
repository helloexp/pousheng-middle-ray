package service;

import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.service.ItemRuleShopWriteServiceImpl;
import com.pousheng.middle.group.model.ItemRuleShop;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemRuleShopWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleShopDao itemRuleShopDao;
        @SpyBean
        private ItemRuleShopWriteServiceImpl itemRuleShopWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleShopWriteServiceTest.MockitoBeans.class;
    }

    ItemRuleShopDao itemRuleShopDao;
    ItemRuleShopWriteServiceImpl itemRuleShopWriteService;

    @Override
    protected void init() {
        itemRuleShopDao = get(ItemRuleShopDao.class);
        itemRuleShopWriteService = get(ItemRuleShopWriteServiceImpl.class);
    }

    private ItemRuleShop mock() {
        return new ItemRuleShop().ruleId(1L).shopId(1L);
    }

    @Test
    public void testCreateSuccess() {
        when(itemRuleShopDao.create(any())).thenReturn(true);
        Response<Long> response = itemRuleShopWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }

    @Test
    public void testCreateUnknownEx() {
        when(itemRuleShopDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemRuleShopWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemRuleShopDao.update(any())).thenReturn(true);
        Response<Boolean> response = itemRuleShopWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(true));
    }


    @Test
    public void testUpdateUnknownEx() {
        when(itemRuleShopDao.update(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleShopWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.update.fail"));
    }


    @Test
    public void testDeleteByIdSuccess() {
        when(itemRuleShopDao.delete(any())).thenReturn(true);
        Response<Boolean> response = itemRuleShopWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteByIdUnknownEx() {
        when(itemRuleShopDao.delete(any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemRuleShopWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.shop.delete.fail"));
    }


}


