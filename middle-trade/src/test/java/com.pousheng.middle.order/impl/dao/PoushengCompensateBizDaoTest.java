package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * 宝胜业务处理模块单元测试
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public class PoushengCompensateBizDaoTest extends BaseDaoTest {

    @Autowired
    private PoushengCompensateBizDao poushengCompensateBizDao;

    private PoushengCompensateBiz poushengCompensateBiz;

    @Before
    public void init() throws Exception{
        poushengCompensateBiz = make(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        poushengCompensateBizDao.create(poushengCompensateBiz);
        assertNotNull(poushengCompensateBiz.getId());
    }

    /**
     * 根据id查询
     */
    @Test
    public void findById() {
        PoushengCompensateBiz poushengCompensateBizExist = poushengCompensateBizDao.findById(poushengCompensateBiz.getId());
        assertNotNull(poushengCompensateBiz);
    }

    /**
     * 更新状态
     */
    @Test
    public void updateStatus(){
        String newStatus = PoushengCompensateBizStatus.FAILED.name();
        poushengCompensateBizDao.updateStatus(poushengCompensateBiz.getId(),poushengCompensateBiz.getStatus(),newStatus);
        PoushengCompensateBiz actl = poushengCompensateBizDao.findById(poushengCompensateBiz.getId());
        assertThat(newStatus,is(actl.getStatus()));
    }

    /**
     * 更新操作
     */
    @Test
    public void update(){
        poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.PROCESSING.name());
        poushengCompensateBiz.setLastFailedReason("2232");
        poushengCompensateBiz.setContext("erreeer");
        poushengCompensateBiz.setBizId("2443434");
        poushengCompensateBiz.setBizType(PoushengCompensateBizType.NOTIFY_HK.name());
        boolean result = poushengCompensateBizDao.update(poushengCompensateBiz);
        assertTrue(result);
    }

    /**
     * 分页查询
     */
    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("bizType", poushengCompensateBiz.getBizType());
        Paging<PoushengCompensateBiz> expressCodePaging = poushengCompensateBizDao.paging(0, 20, params);

        assertThat(expressCodePaging.getTotal(), is(1L));
        assertEquals(expressCodePaging.getData().get(0).getId(), poushengCompensateBiz.getId());
    }


    /**
     *
     * @param status
     * @return
     */
    private  PoushengCompensateBiz make(String status) throws Exception{
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizId("1122424");
        biz.setBizType(PoushengCompensateBizType.NOTIFY_HK.name());
        biz.setContext("");
        biz.setStatus(status);
        biz.setLastFailedReason("ddd");
        return biz;
    }

}
