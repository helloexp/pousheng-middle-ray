package com.pousheng.middle.web.task;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskReadService;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.AbstractRestApiTest;
import com.pousheng.middle.web.item.group.ItemGroups;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/12
 */
@Setter
public class TasksTest extends AbstractRestApiTest {

    @Configuration
    @Getter
    public static class MockitoBeans {
        @MockBean
        ScheduleTaskReadService scheduleTaskReadService;
        @SpyBean
        private ScheduleTasks api;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    ScheduleTaskReadService scheduleTaskReadService;

    private ScheduleTasks api;

    private ScheduleTask task = new ScheduleTask();

    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        super.init();
        api.setScheduleTaskReadService(scheduleTaskReadService);
    }

    @Test
    public void testPagingSuccess() {
        when(scheduleTaskReadService.paging(any())).thenReturn(Response.ok(new Paging<>(1L, Lists.newArrayList(task))));
        Paging<ScheduleTask> result = api.findBy(null, null, null, 1, 1);
        assertThat(result.getTotal(), is(1L));
        assertThat(result.getData().size(), is(1));
    }

}
