package dao;

import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.model.ItemRuleGroup;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
public class ItemRuleGroupDaoTest extends BaseDaoTest {

    @Autowired
    ItemRuleGroupDao ItemRuleGroupDao;

    private ItemRuleGroup ItemRuleGroup;

    @Before
    public void init() {
        ItemRuleGroup = make();
        ItemRuleGroupDao.create(ItemRuleGroup);
        assertNotNull(ItemRuleGroup.getId());
    }

    private ItemRuleGroup make() {
        ItemRuleGroup ItemRuleGroup = new ItemRuleGroup();
        ItemRuleGroup.ruleId(1L).groupId(2L);
        return ItemRuleGroup;
    }

    @Test
    public void testDeleteByRuleId() {
        ItemRuleGroupDao.deleteByRuleId(1L);
        assertNull(ItemRuleGroupDao.findById(ItemRuleGroup.getId()));
    }
    @Test
    public void testFindByRuleId() {
        ItemRuleGroupDao.create(new ItemRuleGroup().ruleId(2L).groupId(3L));
        List<ItemRuleGroup> list= ItemRuleGroupDao.findByRuleId(1L);
        assertThat(list.size(),is(1));
        assertThat(list.get(0).getId(),is(ItemRuleGroup.getId()));
    }

    @Test
    public void testFindByGroupId() {
        ItemRuleGroupDao.create(new ItemRuleGroup().ruleId(2L).groupId(3L));
        List<ItemRuleGroup> list= ItemRuleGroupDao.findByGroupId(3L);
        assertThat(list.size(),is(1));
        List<ItemRuleGroup> list2= ItemRuleGroupDao.findByGroupId(4L);
        assertThat(list2.size(),is(0));
    }


}
