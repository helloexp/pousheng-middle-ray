package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.model.OperationLog;
import io.terminus.common.model.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.util.DateUtil.now;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;


/**
 * Author: sunbo
 * Desc: Dao 测试类
 * Date: 2017-07-31
 */
public class OperationLogDaoTest extends BaseDaoTest {



    @Autowired
    private OperationLogDao operationLogDao;

    private OperationLog operationLog;

    @Before
    public void init() {
        operationLog = make();

        operationLogDao.create(operationLog);
        assertNotNull(operationLog.getId());
    }

    @Test
    public void findById() {
        OperationLog operationLogExist = operationLogDao.findById(operationLog.getId());

        assertNotNull(operationLogExist);
    }

    @Test
    public void update() {
        // todo
        operationLog.setUpdatedAt(now());
        operationLogDao.update(operationLog);

        OperationLog  updated = operationLogDao.findById(operationLog.getId());
        // todo
        //assertEquals(updated.getHasDisplay(), Boolean.TRUE);
    }

    @Test
    public void delete() {
        operationLogDao.delete(operationLog.getId());

        OperationLog deleted = operationLogDao.findById(operationLog.getId());
        assertNull(deleted);
    }

    @Test
    public void paging() {
        Map<String, Object> params = new HashMap<>();
        //todo
        //params.put("userId", operationLog.getUserId());
        Paging<OperationLog > operationLogPaging = operationLogDao.paging(0, 20, params);

        assertThat(operationLogPaging.getTotal(), is(1L));
        assertEquals(operationLogPaging.getData().get(0).getId(), operationLog.getId());
    }

    private OperationLog make() {
        OperationLog operationLog = new OperationLog();

        
        operationLog.setType(7);
        
        operationLog.setOperatorName("");
        
        operationLog.setOperateId(877l);
        
        operationLog.setContent("");
        
        operationLog.setCreatedAt(new Date());
        
        operationLog.setUpdatedAt(new Date());
        

        return operationLog;
    }

}