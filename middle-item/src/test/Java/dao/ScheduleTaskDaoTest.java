package dao;

import com.pousheng.middle.task.dto.TaskSearchCriteria;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.impl.dao.ScheduleTaskDao;
import com.pousheng.middle.task.model.ScheduleTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */
public class ScheduleTaskDaoTest extends BaseDaoTest {

    @Autowired
    private ScheduleTaskDao scheduleTaskDao;

    private ScheduleTask scheduleTask;

    @Before
    public void init() {
        scheduleTask = make();
        scheduleTaskDao.create(scheduleTask);
        assertNotNull(scheduleTask.getId());
    }

    private ScheduleTask make() {
        ScheduleTask scheduleTask = new ScheduleTask();
        scheduleTask.type(TaskTypeEnum.ITEM_GROUP.value()).status(TaskStatusEnum.INIT.value())
                .userId(0L).businessId(1L);
        return scheduleTask;
    }

    @Test
    public void testPaging (){
        TaskSearchCriteria criteria =new TaskSearchCriteria();
        criteria.setType(TaskTypeEnum.ITEM_GROUP.value());
        criteria.setBusinessId(1L);
        assertNotNull(scheduleTaskDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap()).getData());
        Assert.assertThat(scheduleTaskDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap()).getTotal(),is(1L));
    }

    @Test
    public void testUpdate() {
        scheduleTask = scheduleTaskDao.findById(scheduleTask.getId());
        scheduleTask.setStatus(TaskStatusEnum.EXECUTING.value());
        scheduleTask.setResult("测试");
        scheduleTaskDao.update(scheduleTask);
        ScheduleTask updated = scheduleTaskDao.findById(scheduleTask.getId());
        assertThat(updated.getStatus(), is(TaskStatusEnum.EXECUTING.value()));
        assertThat(updated.getResult(), is("测试"));
    }

    @Test
    public void findFirstByTypeAndStatus() {
        ScheduleTask task = scheduleTaskDao.findFirstByTypeAndStatus(TaskTypeEnum.ITEM_GROUP.value(),TaskStatusEnum.INIT.value());
        assertThat(scheduleTask.getId(), is(task.getId()));
    }

}
