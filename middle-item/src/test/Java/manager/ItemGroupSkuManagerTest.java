package manager;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.impl.manager.ItemGroupSkuManager;
import com.pousheng.middle.group.model.ItemGroupSku;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import service.AbstractServiceTest;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
public class ItemGroupSkuManagerTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @SpyBean
        private ItemGroupSkuManager itemGroupSkuManager;
        @MockBean
        private ItemGroupSkuDao itemGroupSkuDao;
        @MockBean
        private ItemGroupDao itemGroupDao;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemGroupSkuManagerTest.MockitoBeans.class;
    }

    ItemGroupSkuManager itemGroupSkuManager;

    ItemGroupSkuDao itemGroupSkuDao;

    ItemGroupDao itemGroupDao;

    @Override
    protected void init() {
        itemGroupSkuDao = get(ItemGroupSkuDao.class);
        itemGroupDao = get(ItemGroupDao.class);
        itemGroupSkuManager = get(ItemGroupSkuManager.class);
    }

    private ItemGroupSku mock() {
        return new ItemGroupSku().id(1L).skuCode("1").type(1);
    }


    @Test
    public void testCreateSuccess() {
        when(itemGroupSkuDao.create(any())).thenReturn(true);
        Response<Long> response = itemGroupSkuManager.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1L));
    }

    @Test
    public void testCreateRepeat() {
        when(itemGroupSkuDao.findByGroupIdAndSkuCode(any(), any())).thenReturn(mock());
        Response<Long> response = itemGroupSkuManager.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.is.exist"));
    }

    @Test
    public void testCreateUnknownEx() {
        when(itemGroupSkuDao.create(any())).thenReturn(false);
        Response<Long> response = itemGroupSkuManager.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.create.fail"));
    }


    @Test
    public void testBatchCreateSuccess() {
        List<String> skuCodes = Lists.newArrayList("1", "2", "3");
        when(itemGroupSkuDao.creates(any())).thenReturn(skuCodes.size());
        Response<Integer> response = itemGroupSkuManager.batchCreate(skuCodes, 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(skuCodes.size()));
    }


    @Test
    public void testBatchDeleteSuccess() {
        List<String> skuCodes = Lists.newArrayList("1", "2", "3");
        when(itemGroupSkuDao.batchDelete(any(),any(),any(),any())).thenReturn(skuCodes.size());
        Response<Integer> response = itemGroupSkuManager.batchDelete(skuCodes, 1L, 1,1);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(skuCodes.size()));
    }

    @Test
    public void testDelete(){
        when(itemGroupSkuDao.deleteByGroupIdAndSkuCode(any(),any())).thenReturn(true);
        Response<Boolean> response = itemGroupSkuManager.deleteByGroupIdAndSkuId(1L, "1");
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteToUpdateUnknownEx(){
        when(itemGroupSkuDao.deleteByGroupIdAndSkuCode(any(),any())).thenReturn(true);
        Response<Boolean> response = itemGroupSkuManager.deleteByGroupIdAndSkuId(1L, "1");
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getError(), is("item.group.sku.delete.fail"));
    }

    @Test
    public void testDeleteUnknownEx(){
        when(itemGroupSkuDao.deleteByGroupIdAndSkuCode(any(),any())).thenThrow((new NullPointerException()));
        Response<Boolean> response = itemGroupSkuManager.deleteByGroupIdAndSkuId(1L, "1");
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.sku.delete.fail"));
    }
}
