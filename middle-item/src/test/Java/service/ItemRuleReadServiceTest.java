package service;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.dto.ItemRuleCriteria;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.impl.manager.ItemRuleManager;
import com.pousheng.middle.group.impl.service.ItemRuleReadServiceImpl;
import com.pousheng.middle.group.model.ItemRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.cache.ShopCacher;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemRuleReadServiceTest extends AbstractServiceTest {

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleDao itemRuleDao;
        @MockBean
        private ItemGroupDao itemGroupDao;
        @SpyBean
        private ItemRuleReadServiceImpl itemRuleReadService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleReadServiceTest.MockitoBeans.class;
    }


    ItemRuleDao itemRuleDao;
    ItemRuleReadServiceImpl itemRuleReadService;
    ItemGroupDao itemGroupDao;

    @Override
    protected void init() {
        itemRuleDao = get(ItemRuleDao.class);
        itemRuleReadService = get(ItemRuleReadServiceImpl.class);
        itemGroupDao = get(ItemGroupDao.class);

    }

    private ItemRule mock() {
        return new ItemRule();
    }

    @Test
    public void testFindByIdSuccess() {
        when(itemRuleDao.findById((Long) any())).thenReturn(mock());
        Response<ItemRule> response = itemRuleReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(mock()));
    }

    @Test
    public void testFindByIdUnknownEx() {
        when(itemRuleDao.findById((Long) any())).thenThrow(new NullPointerException());
        Response<ItemRule> response = itemRuleReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.find.fail"));
    }


    @Test
    public void testPagingSuccess() {
        ItemRuleCriteria criteria = new ItemRuleCriteria();
        when(itemRuleDao.paging(any(), any(), (Map<String, Object>) any())).thenReturn(new Paging<>(1L, Lists.newArrayList(mock())));
        criteria.setId(1L);
        Response<Paging<ItemRule>> response = itemRuleReadService.paging(criteria);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult().getTotal(), is(1L));
    }


    @Test
    public void testPagingUnknownEx() {
        ItemRuleCriteria criteria = new ItemRuleCriteria();
        when(itemRuleDao.paging(any(), any(), (Map<String, Object>) any())).thenThrow(new NullPointerException());
        criteria.setId(1L);
        Response<Paging<ItemRule>> response = itemRuleReadService.paging(criteria);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.find.fail"));
    }


}


