package dao;

import com.pousheng.middle.group.dto.ItemGroupCriteria;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.model.ItemGroup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */
public class ItemGroupDaoTest extends BaseDaoTest {

    @Autowired
    private ItemGroupDao itemGroupDao;

    private ItemGroup itemGroup;

    @Before
    public void init() {
        itemGroup = make();
        itemGroupDao.create(itemGroup);
        assertNotNull(itemGroup.getId());
    }

    private ItemGroup make() {
        ItemGroup itemGroup = new ItemGroup();
        itemGroup.name("测试").auto(true).relatedNum(0L).setType(1);
        return itemGroup;
    }

    @Test
    public void testPaging (){
        ItemGroupCriteria criteria =new ItemGroupCriteria();
        criteria.setName("测");
        assertNotNull(itemGroupDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap()).getData());
        assertThat(itemGroupDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap()).getTotal(),is(1L));
    }

    @Test
    public void testFindByName() {
        assertNotNull(itemGroupDao.findByName("测试"));
    }

    @Test
    public void testFindAutoGroups() {
        assertNotNull(itemGroupDao.findAutoGroups());
    }


    @Test
    public void testUpdate() {
        itemGroup = itemGroupDao.findById(itemGroup.getId());
        itemGroup.setRelatedNum(100L);
        itemGroup.setAuto(false);
        itemGroup.setType(1);
        itemGroupDao.update(itemGroup);
        ItemGroup updated = itemGroupDao.findById(itemGroup.getId());
        assertThat(updated.getRelatedNum(), is(100L));
        assertThat(updated.getAuto(), is(false));
    }

    @Test
    public void testDelete() {
        itemGroupDao.delete(itemGroup.getId());
        assertNull(itemGroupDao.findById(itemGroup.getId()));
    }
}
