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
        itemGroupSku.groupId(1L).type(1).skuCode("2");
        return itemGroupSku;
    }

    @Test
    public void testDelete() {
        itemGroupSkuDao.delete(itemGroupSku.getId());
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void tesDeleteByGroupIdAndSkuId() {
        itemGroupSkuDao.deleteByGroupIdAndSkuCode(itemGroupSku.getGroupId(), itemGroupSku.getSkuCode());
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void testFindBySkuId() {
        List<ItemGroupSku> list = itemGroupSkuDao.findBySkuCode(itemGroupSku.getSkuCode());
        assertThat(list.get(0), is(itemGroupSku));
    }


    @Test
    public void testFindByGroupIdAndSkuId() {
        ItemGroupSku info = itemGroupSkuDao.findByGroupIdAndSkuCode(itemGroupSku.getGroupId(), itemGroupSku.getSkuCode());
        assertThat(info, is(itemGroupSku));
    }

    @Test
    public void testBatchDelete() {
        itemGroupSkuDao.batchDelete(Lists.newArrayList("12"), 1L, 1);
        assertNull(itemGroupSkuDao.findById(itemGroupSku.getId()));
    }

    @Test
    public void testCountGroupSku() {
        Long count = itemGroupSkuDao.countGroupSku(1L);
        assertThat(count, is(1L));
    }

}
