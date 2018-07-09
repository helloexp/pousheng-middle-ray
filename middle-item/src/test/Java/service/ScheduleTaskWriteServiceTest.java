package service;

import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.impl.dao.ScheduleTaskDao;
import com.pousheng.middle.task.impl.service.ScheduleTaskWriteServiceImpl;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import io.terminus.common.model.Response;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ScheduleTaskWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ScheduleTaskDao scheduleTaskDao;
        @MockBean
        private ItemGroupDao itemGroupDao;
        @SpyBean
        private ScheduleTaskWriteServiceImpl scheduleTaskWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ScheduleTaskWriteServiceTest.MockitoBeans.class;
    }

    ScheduleTaskDao scheduleTaskDao;

    ItemGroupDao itemGroupDao;

    ScheduleTaskWriteService scheduleTaskWriteService;

    @Override
    protected void init() {
        scheduleTaskDao = get(ScheduleTaskDao.class);
        itemGroupDao = get(ItemGroupDao.class);
        scheduleTaskWriteService = get(ScheduleTaskWriteServiceImpl.class);
    }

    private ScheduleTask mock() {
        return new ScheduleTask().id(1L).type(TaskTypeEnum.ITEM_GROUP.value()).status(TaskStatusEnum.INIT.value())
                .userId(0L).businessId(1L);
    }


    @Test
    public void testCreateSuccess() {
        when(scheduleTaskDao.create(any())).thenReturn(true);
        Response<Long> response = scheduleTaskWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1L));
    }

    @Test
    public void testCreateUnknownEx() {
        when(scheduleTaskDao.create(any())).thenThrow(new NullPointerException());
        Response<Long> response = scheduleTaskWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("schedule.task.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(scheduleTaskDao.update(any())).thenReturn(true);
        Response<Boolean> response = scheduleTaskWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(true));
    }

    @Test
    public void testUpdateUnknownEx() {
        when(scheduleTaskDao.update(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = scheduleTaskWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("schedule.task.update.fail"));
    }



    @Test
    public void testCreatesSuccess() {
        when(scheduleTaskDao.creates(any())).thenReturn(1);
        Response<Integer> response = scheduleTaskWriteService.creates(Lists.newArrayList(mock()));
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1));
    }

    @Test
    public void testBatchCreatesUnknownEx() {
        when(scheduleTaskDao.creates(any())).thenThrow((new NullPointerException()));
        Response<Integer> response = scheduleTaskWriteService.creates(Lists.newArrayList(mock()));
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("schedule.task.create.fail"));
    }

}


