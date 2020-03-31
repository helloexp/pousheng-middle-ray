package service;

import com.google.common.collect.Lists;
import com.pousheng.middle.task.dto.TaskSearchCriteria;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.impl.dao.ScheduleTaskDao;
import com.pousheng.middle.task.impl.service.ScheduleTaskReadServiceImpl;
import com.pousheng.middle.task.model.ScheduleTask;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ScheduleTaskReadServiceTest extends AbstractServiceTest {

    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ScheduleTaskDao scheduleTaskDao;
        @SpyBean
        private ScheduleTaskReadServiceImpl scheduleTaskReadService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ScheduleTaskReadServiceTest.MockitoBeans.class;
    }


    ScheduleTaskDao scheduleTaskDao;
    ScheduleTaskReadServiceImpl scheduleTaskReadService;

    @Override
    protected void init() {
        scheduleTaskDao = get(ScheduleTaskDao.class);
        scheduleTaskReadService = get(ScheduleTaskReadServiceImpl.class);

    }

    private ScheduleTask mock() {
        return new ScheduleTask().id(1L).type(TaskTypeEnum.ITEM_GROUP.value()).status(TaskStatusEnum.INIT.value())
                .userId(0L).businessId(1L);
    }

    @Test
    public void testFindByIdSuccess() {
        when(scheduleTaskDao.findById((Long) any())).thenReturn(mock());
        Response<ScheduleTask> response = scheduleTaskReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(mock()));
    }

    @Test
    public void testFindByIdUnknownEx() {
        when(scheduleTaskDao.findById((Long) any())).thenThrow(new NullPointerException());
        Response<ScheduleTask> response = scheduleTaskReadService.findById(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("schedule.task.find.fail"));
    }


    @Test
    public void testPagingSuccess() {
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        when(scheduleTaskDao.paging(any(), any(), (Map<String, Object>) any())).thenReturn(new Paging<>(1L, Lists.newArrayList(mock())));
        Response<Paging<ScheduleTask>> response = scheduleTaskReadService.paging(criteria);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult().getTotal(), is(1L));
    }


    @Test
    public void testPagingDetailUnknownEx() {
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        when(scheduleTaskDao.paging(any(), any(), (Map<String, Object>) any())).thenThrow(new NullPointerException());
        Response<Paging<ScheduleTask>> response = scheduleTaskReadService.paging(criteria);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("schedule.task.find.fail"));
    }


}


