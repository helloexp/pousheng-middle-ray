package service;

import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.impl.service.ItemRuleGroupWriteServiceImpl;
import com.pousheng.middle.group.model.ItemRuleGroup;
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
public class ItemRuleGroupWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemRuleGroupDao itemRuleGroupDao;
        @SpyBean
        private ItemRuleGroupWriteServiceImpl itemRuleGroupWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemRuleGroupWriteServiceTest.MockitoBeans.class;
    }

    ItemRuleGroupDao itemRuleGroupDao;
    ItemRuleGroupWriteServiceImpl itemRuleGroupWriteService;

    @Override
    protected void init() {
        itemRuleGroupDao = get(ItemRuleGroupDao.class);
        itemRuleGroupWriteService = get(ItemRuleGroupWriteServiceImpl.class);
    }

    private ItemRuleGroup mock() {
        return new ItemRuleGroup().ruleId(1L).groupId(1L);
    }

    @Test
    public void testCreateSuccess() {
        when(itemRuleGroupDao.create(any())).thenReturn(true);
        Response<Long> response = itemRuleGroupWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }

    @Test
    public void testCreateUnknownEx() {
        when(itemRuleGroupDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemRuleGroupWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemRuleGroupDao.update(any())).thenReturn(true);
        Response<Boolean> response = itemRuleGroupWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(true));
    }


    @Test
    public void testUpdateUnknownEx() {
        when(itemRuleGroupDao.update(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemRuleGroupWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.update.fail"));
    }


    @Test
    public void testDeleteByIdSuccess() {
        when(itemRuleGroupDao.delete(any())).thenReturn(true);
        Response<Boolean> response = itemRuleGroupWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteByIdUnknownEx() {
        when(itemRuleGroupDao.delete(any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemRuleGroupWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.rule.group.delete.fail"));
    }


}


