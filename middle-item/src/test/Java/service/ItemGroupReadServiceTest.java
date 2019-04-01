package service;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.dto.ItemGroupCriteria;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.service.ItemGroupReadServiceImpl;
import com.pousheng.middle.group.model.ItemGroup;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemGroupReadServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemGroupDao itemGroupDao;
        @SpyBean
        private ItemGroupReadServiceImpl itemGroupReadService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemGroupReadServiceTest.MockitoBeans.class;
    }


    ItemGroupDao itemGroupDao;
    ItemGroupReadServiceImpl itemGroupReadService;

    @Override
    protected void init() {
        itemGroupDao = get(ItemGroupDao.class);
        itemGroupReadService = get(ItemGroupReadServiceImpl.class);
    }

    private ItemGroup mock() {
        return new ItemGroup().id(1L).name("test").auto(false).relatedNum(0L);
    }


    @Test
    public void testFindByIdSuccess() {
        when(itemGroupDao.findById((Long) any())).thenReturn(mock());
        Response<ItemGroup> response = itemGroupReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(mock()));
    }

    @Test
    public void testFindByIdFail() {
        when(itemGroupDao.findById((Integer) any())).thenReturn(null);
        Response<ItemGroup> response = itemGroupReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.find.fail"));
    }

    @Test
    public void testFindByIdUnknownEx() {
        when(itemGroupDao.findById((Integer) any())).thenThrow(new NullPointerException());
        Response<ItemGroup> response = itemGroupReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.find.fail"));
    }

    @Test
    public void testFindByCriteriaSuccess() {
        when(itemGroupDao.paging(any(), any(), (Map<String, Object>) any())).thenReturn(new Paging<>(1L, Lists.newArrayList(mock())));
        ItemGroupCriteria criteria = new ItemGroupCriteria();
        criteria.setName("test");
        Response<Paging<ItemGroup>> response = itemGroupReadService.findByCriteria(criteria);
        assertThat(response.getResult().getTotal(), is(1L));
        assertThat(response.getResult().getData().get(0), is(mock()));
    }


    @Test
    public void testFindAutoGroupsUnknownEx() {
        when(itemGroupDao.findAutoGroups()).thenThrow(new NullPointerException());
        Response<List<ItemGroup>> response = itemGroupReadService.findAutoGroups();
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.find.fail"));
    }

    @Test
    public void testFindAutoGroupsSuccess() {
        when(itemGroupDao.findAutoGroups()).thenReturn(Lists.newArrayList());
        Response<List<ItemGroup>> response = itemGroupReadService.findAutoGroups();
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Lists.newArrayList()));
    }


    @Test
    public void testFindByIdsSuccess() {
        when(itemGroupDao.findByIds(any())).thenReturn(Lists.newArrayList(mock()));
        Response<List<ItemGroup>> response = itemGroupReadService.findByIds(Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult().get(0), is(mock()));
    }

    @Test
    public void testFindByIdsFail() {
        when(itemGroupDao.findByIds(any())).thenThrow(new NullPointerException());
        Response<List<ItemGroup>> response = itemGroupReadService.findByIds(Lists.newArrayList());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.find.fail"));
    }


    @Test
    public void testCheckSuccess() {
        when(itemGroupDao.findByName(any())).thenReturn(new ItemGroup());
        Response<Boolean> response = itemGroupReadService.checkName("测试");
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(false));
    }

    @Test
    public void testCheckUnknowEx() {
        when(itemGroupDao.findByName(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemGroupReadService.checkName("测试");
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.check.fail"));
    }
}
