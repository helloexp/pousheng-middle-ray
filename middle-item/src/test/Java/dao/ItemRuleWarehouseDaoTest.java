package dao;

import com.pousheng.middle.group.impl.dao.ItemRuleWarehouseDao;
import com.pousheng.middle.group.model.ItemRuleWarehouse;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: songrenfei
 * Desc: 商品规则与仓库关系映射表Dao 测试类
 * Date: 2018-07-13
 */
public class ItemRuleWarehouseDaoTest extends BaseDaoTest {


    @Autowired
    ItemRuleWarehouseDao itemRuleWarehouseDao;

    private ItemRuleWarehouse itemRuleWarehouse;

    @Before
    public void init() {
        itemRuleWarehouse = make();
        itemRuleWarehouseDao.create(itemRuleWarehouse);
        assertNotNull(itemRuleWarehouse.getId());
    }

    private ItemRuleWarehouse make() {
        ItemRuleWarehouse itemRuleWarehouse = new ItemRuleWarehouse();
        itemRuleWarehouse.ruleId(1L).warehouseId(2L);
        return itemRuleWarehouse;
    }

    @Test
    public void testDeleteByRuleId() {
        itemRuleWarehouseDao.deleteByRuleId(1L);
        assertNull(itemRuleWarehouseDao.findById(itemRuleWarehouse.getId()));
    }

    @Test
    public void testCheckWarehouseIds() {
        Boolean result = itemRuleWarehouseDao.checkWarehouseIds(2L, Lists.newArrayList(2L));
        assertThat(result, is(true));
    }

    @Test
    public void testFindByRuleId() {
        itemRuleWarehouseDao.create(new ItemRuleWarehouse().ruleId(2L).warehouseId(3L));
        List<ItemRuleWarehouse> list = itemRuleWarehouseDao.findByRuleId(1L);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is(itemRuleWarehouse.getId()));
    }

    @Test
    public void testFindWarehouseIds() {
        itemRuleWarehouseDao.create(new ItemRuleWarehouse().ruleId(2L).warehouseId(3L));
        List<Long> warehouseIds = itemRuleWarehouseDao.findWarehouseIds();
        assertThat(warehouseIds.size(), is(2));
        assertThat(warehouseIds, is(Lists.newArrayList(2L, 3L)));
    }


    @Test
    public void testFindRuleIdByWarehouseId() {
        itemRuleWarehouseDao.create(new ItemRuleWarehouse().ruleId(2L).warehouseId(3L));
        Long ruleId = itemRuleWarehouseDao.findRuleIdByWarehouseId(3L);
        assertThat(ruleId, is(2L));
    }
}
