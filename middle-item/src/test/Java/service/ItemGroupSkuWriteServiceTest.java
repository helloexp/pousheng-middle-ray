package service;

import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.impl.manager.ItemGroupSkuManager;
import com.pousheng.middle.group.impl.service.ItemGroupSkuWriteServiceImpl;
import com.pousheng.middle.group.model.ItemGroupSku;
import com.pousheng.middle.group.service.ItemGroupSkuWriteService;
import io.terminus.common.model.Response;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ItemGroupSkuWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private ItemGroupSkuManager itemGroupSkuManager;
        @MockBean
        private ItemGroupSkuDao itemGroupSkuDao;
        @MockBean
        private ItemGroupDao itemGroupDao;
        @SpyBean
        private ItemGroupSkuWriteServiceImpl itemGroupSkuWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemGroupSkuWriteServiceTest.MockitoBeans.class;
    }

    ItemGroupSkuDao itemGroupSkuDao;

    ItemGroupDao itemGroupDao;

    ItemGroupSkuWriteService itemGroupSkuWriteService;

    ItemGroupSkuManager itemGroupSkuManager;

    @Override
    protected void init() {
        itemGroupSkuDao = get(ItemGroupSkuDao.class);
        itemGroupDao = get(ItemGroupDao.class);
        itemGroupSkuWriteService = get(ItemGroupSkuWriteServiceImpl.class);
        itemGroupSkuManager = get(ItemGroupSkuManager.class);
    }

    private ItemGroupSku mock() {
        return new ItemGroupSku().id(1L).skuCode("1").type(1);
    }


    @Test
    public void testCreateSuccess() {
        when(itemGroupSkuDao.create(any())).thenReturn(true);
        Response<Long> response = itemGroupSkuWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1L));
    }

    @Test
    public void testCreateUnknownEx() {
        when(itemGroupSkuDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemGroupSkuWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemGroupSkuDao.update(any())).thenReturn(true);
        Response<Boolean> response = itemGroupSkuWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(true));
    }

    @Test
    public void testUpdateUnknownEx() {
        when(itemGroupSkuDao.update(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemGroupSkuWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.update.fail"));
    }



    @Test
    public void testDeleteByIdSuccess() {
        when(itemGroupSkuDao.delete(any())).thenReturn(true);
        Response<Boolean> response = itemGroupSkuWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteByIdUnknownEx() {
        when(itemGroupSkuDao.delete(any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemGroupSkuWriteService.deleteById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.delete.fail"));
    }

    @Test
    public void testCreateItemGroupSkuRepeat() {
        when(itemGroupSkuDao.findByGroupIdAndSkuCode(any(),any())).thenReturn(mock());
        Response<Long> response = itemGroupSkuWriteService.createItemGroupSku(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.is.exist"));
    }

    @Test
    public void testCreateItemGroupSkuSuccess() {
        when(itemGroupSkuDao.findByGroupIdAndSkuCode(any(),any())).thenReturn(null);
        when(itemGroupSkuDao.create(any())).thenReturn(true);
        Response<Long> response = itemGroupSkuWriteService.createItemGroupSku(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1L));
    }

    @Test
    public void testCreateItemGroupSkuUnknownEx() {
        when(itemGroupSkuDao.findByGroupIdAndSkuCode(any(),any())).thenReturn(null);
        when(itemGroupSkuDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = itemGroupSkuWriteService.createItemGroupSku(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.create.fail"));
    }


    @Test
    public void testBatchCreateSuccess() {
        when(itemGroupSkuDao.creates(any())).thenReturn(1);
        Response<Integer> response = itemGroupSkuWriteService.batchCreate(Lists.newArrayList("1"), 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1));
    }

    @Test
    public void testBatchCreateUnknownEx() {
        when(itemGroupSkuDao.creates(any())).thenThrow((new NullPointerException()));
        Response<Integer> response = itemGroupSkuWriteService.batchCreate(Lists.newArrayList("1"), 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.create.fail"));
    }

    @Test
    public void testBatchDeleteSuccess() {
        when(itemGroupSkuDao.batchDelete(any(), any(), any(), any())).thenReturn(1);
        Response<Integer> response = itemGroupSkuWriteService.batchDelete(Lists.newArrayList("1"), 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1));
    }

    @Test
    public void testBatchDeleteUnknownEx() {
        when(itemGroupSkuDao.batchDelete(any(), any(), any(), any())).thenThrow((new NullPointerException()));
        Response<Integer> response = itemGroupSkuWriteService.batchDelete(Lists.newArrayList("1"), 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.delete.fail"));
    }


    @Test
    public void testDeleteByGroupIdAndSkuCodeSuccess() {
        when(itemGroupSkuDao.deleteByGroupIdAndSkuCode(any(), any())).thenReturn(true);
        Response<Boolean> response = itemGroupSkuWriteService.deleteByGroupIdAndSkuCode(1L, "1");
        assertThat(response.isSuccess(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteByGroupIdAndSkuCodeUnknownEx() {
        when(itemGroupSkuDao.deleteByGroupIdAndSkuCode(any(), any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemGroupSkuWriteService.deleteByGroupIdAndSkuCode(1L, "1");
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.delete.fail"));
    }

}


