package dao;

import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.model.ItemRuleShop;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
public class ItemRuleShopDaoTest extends BaseDaoTest {

    @Autowired
    ItemRuleShopDao itemRuleShopDao;

    private ItemRuleShop itemRuleShop;

    @Before
    public void init() {
        itemRuleShop = make();
        itemRuleShopDao.create(itemRuleShop);
        assertNotNull(itemRuleShop.getId());
    }

    private ItemRuleShop make() {
        ItemRuleShop itemRuleShop = new ItemRuleShop();
        itemRuleShop.ruleId(1L).shopId(2L);
        return itemRuleShop;
    }

    @Test
    public void testDeleteByRuleId() {
        itemRuleShopDao.deleteByRuleId(1L);
        assertNull(itemRuleShopDao.findById(itemRuleShop.getId()));
    }

    @Test
    public void testCheckShopIds() {
        Boolean result = itemRuleShopDao.checkShopIds(2L, Lists.newArrayList(2L));
        assertThat(result, is(true));
    }

    @Test
    public void testFindByRuleId() {
        itemRuleShopDao.create(new ItemRuleShop().ruleId(2L).shopId(3L));
        List<ItemRuleShop> list= itemRuleShopDao.findByRuleId(1L);
        assertThat(list.size(),is(1));
        assertThat(list.get(0).getId(),is(itemRuleShop.getId()));
    }

    @Test
    public void testFindShopIds(){
        itemRuleShopDao.create(new ItemRuleShop().ruleId(2L).shopId(3L));
        List<Long> shopIds=itemRuleShopDao.findShopIds();
        assertThat(shopIds.size(),is(2));
        assertThat(shopIds,is(Lists.newArrayList(2L,3L)));
    }


    @Test
    public void testFindRuleIdByShopId(){
        itemRuleShopDao.create(new ItemRuleShop().ruleId(2L).shopId(3L));
        Long ruleId=itemRuleShopDao.findRuleIdByShopId(3L);
        assertThat(ruleId,is(2L));
    }


}
