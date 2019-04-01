package service;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.service.ItemRuleGroupReadServiceImpl;
import com.pousheng.middle.group.model.ItemRuleGroup;
import io.terminus.common.model.Response;
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
public class ItemRuleGroupReadServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleGroupDao itemRuleGroupDao;
        @SpyBean
        private ItemRuleGroupReadServiceImpl itemRuleGroupReadService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleGroupReadServiceTest.MockitoBeans.class;
    }


    ItemRuleGroupDao itemRuleGroupDao;
    ItemRuleGroupReadServiceImpl itemRuleGroupReadService;

    @Override
    protected void init() {
        itemRuleGroupDao = get(ItemRuleGroupDao.class);
        itemRuleGroupReadService = get(ItemRuleGroupReadServiceImpl.class);

    }

    private ItemRuleGroup mock() {
        return new ItemRuleGroup().ruleId(1L).groupId(1L);
    }

    @Test
    public void testFindByIdSuccess() {
        when(itemRuleGroupDao.findById((Long) any())).thenReturn(mock());
        Response<ItemRuleGroup> response = itemRuleGroupReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(mock()));
    }

    @Test
    public void testFindByIdFail() {
        when(itemRuleGroupDao.findById((Long) any())).thenThrow(new NullPointerException());
        Response<ItemRuleGroup> response = itemRuleGroupReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.find.fail"));
    }

    @Test
    public void testFindByGroupIdSuccess() {
        when(itemRuleGroupDao.findByGroupId(any())).thenReturn(Lists.newArrayList(mock()));
        Response<List<ItemRuleGroup>> response = itemRuleGroupReadService.findByGroupId(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult().get(0), is(mock()));
    }

    @Test
    public void testFindByGroupIdFail() {
        when(itemRuleGroupDao.findByGroupId(any())).thenThrow(new NullPointerException());
        Response<List<ItemRuleGroup>> response = itemRuleGroupReadService.findByGroupId(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.find.fail"));
    }

    @Test
    public void testFindByRuleIdSuccess() {
        when(itemRuleGroupDao.findByRuleId(any())).thenReturn(Lists.newArrayList(mock()));
        Response<List<ItemRuleGroup>> response = itemRuleGroupReadService.findByRuleId(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult().get(0), is(mock()));
    }

    @Test
    public void testFindByRuleIdFail() {
        when(itemRuleGroupDao.findByRuleId(any())).thenThrow(new NullPointerException());
        Response<List<ItemRuleGroup>> response = itemRuleGroupReadService.findByRuleId(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.find.fail"));
    }


}


