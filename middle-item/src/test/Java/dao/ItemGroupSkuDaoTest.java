package dao;

import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.model.ItemGroupSku;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */
public class ItemGroupSkuDaoTest extends BaseDaoTest {

    @Autowired
    private ItemGroupSkuDao itemGroupSkuDao;

    private ItemGroupSku itemGroupSku;

    @Before
    public void init() {
        itemGroupSku = make();
        itemGroupSkuDao.create(itemGroupSku);
        assertNotNull(itemGroupSku.getId());
        itemGroupSku = itemGroupSkuDao.findById(itemGroupSku.getId());
    }

    private ItemGroupSku make() {
        ItemGroupSku itemGroupSku = new ItemGroupSku();
        itemGroupSku.groupId(1L).type(1).skuId(2L);
        return itemGroupSku;
    }

    @Test
    public void testDelete() {
        itemGroupSkuDao.delete(itemGroupSku.getId());
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void tesDeleteByGroupIdAndSkuId() {
        itemGroupSkuDao.deleteByGroupIdAndSkuId(itemGroupSku.getGroupId(), itemGroupSku.getSkuId());
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void testFindBySkuId() {
        List<ItemGroupSku> list = itemGroupSkuDao.findBySkuId(itemGroupSku.getSkuId());
        assertThat(list.get(0), is(itemGroupSku));
    }


    @Test
    public void testFindByGroupIdAndSkuId() {
        ItemGroupSku info = itemGroupSkuDao.findByGroupIdAndSkuId(itemGroupSku.getGroupId(), itemGroupSku.getSkuId());
        assertThat(info, is(itemGroupSku));
    }

    @Test
    public void testBatchDelete() {
        itemGroupSkuDao.batchDelete(Lists.newArrayList(2L), 1L, 1);
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void testCountGroupSku() {
        Long count = itemGroupSkuDao.countGroupSku(1L);
        assertThat(count, is(1L));
    }

}
