package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.ExpressCode;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by tony on 2017/6/27.
 */
public class ExpressCodeDaoTest extends BaseDaoTest {

    @Autowired
    private ExpressCodeDao expressCodeDao;

    private ExpressCode expressCode;

    @Before
    public void init() throws Exception {
        expressCode = make();
        expressCodeDao.create(expressCode);
        assertNotNull(expressCode.getId());
    }

    /**
     * 根据id查询
     */
    @Test
    public void findById() {
        ExpressCode expressCodeExist = expressCodeDao.findById(expressCode.getId());
        assertNotNull(expressCodeExist);
    }

    /**
     * 根据id查询
     */
    @Test
    public void findByExpressName() {
        ExpressCode expressCodeExist = expressCodeDao.findByExpressName(expressCode.getExpressName());
        assertNotNull(expressCodeExist);
    }

    /**
     * 根据相应条件做更新
     */
    @Test
    public void update() {
        expressCode.setJdExpressCode("jdExpressCode1");
        expressCode.setSuningExpressCode("suningExpressCode1");
        expressCodeDao.update(expressCode);
        ExpressCode updated = expressCodeDao.findById(expressCode.getId());
        assertEquals(expressCode.getJdExpressCode(), String.valueOf("jdExpressCode1"));
    }

    /**
     * 根据主键删除
     */
    @Test
    public void delete() {
        expressCodeDao.delete(expressCode.getId());

        ExpressCode deleted = expressCodeDao.findById(expressCode.getId());
        assertNull(deleted);
    }

    /**
     * 分页查询
     */
    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("expressName", expressCode.getExpressName());
        Paging<ExpressCode> expressCodePaging = expressCodeDao.paging(0, 20, params);

        assertThat(expressCodePaging.getTotal(), is(1L));
        assertEquals(expressCodePaging.getData().get(0).getId(), expressCode.getId());
    }


    private ExpressCode make() throws Exception {
        ExpressCode expressCode = new ExpressCode();
        expressCode.setExpressName("中通");
        expressCode.setOfficalExpressCode("officalZhongTong");
        expressCode.setPoushengExpressCode("poushengZhongTong");
        expressCode.setJdExpressCode("jdZhongTong");
        expressCode.setTaobaoExpressCode("taobaoZhongTong");
        expressCode.setSuningExpressCode("suningZhongTong");
        expressCode.setFenqileExpressCode("fenqileZhongTong");
        expressCode.setHkExpressCode("hkExpressZhongTong");
        expressCode.setCreatedAt(new Date());
        expressCode.setUpdatedAt(new Date());
        return expressCode;
    }
}
