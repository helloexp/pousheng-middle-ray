package com.pousheng.middle.web.item.group;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.AbstractRestApiTest;
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
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/12
 */
@Setter
public class ItemGroupsTest extends AbstractRestApiTest {

    @Configuration
    @Getter
    public static class MockitoBeans {
        @MockBean
        private ItemGroupReadService itemGroupReadService;
        @MockBean
        private ItemGroupWriteService itemGroupWriteService;
        @MockBean
        ItemRuleGroupReadService itemRuleGroupReadService;
        @MockBean
        ScheduleTaskWriteService scheduleTaskWriteService;
        @SpyBean
        private ItemGroups api;
    }

    @Override
    protected Class<?> mockitoBeans() {
        return MockitoBeans.class;
    }

    private ItemGroupReadService itemGroupReadService;

    private ItemGroupWriteService itemGroupWriteService;

    private ItemRuleGroupReadService itemRuleGroupReadService;

    private ScheduleTaskWriteService scheduleTaskWriteService;

    private ItemGroups api;

    private ItemGroup group = new ItemGroup().relatedNum(0L).name("测试").id(1L);

    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        super.init();
        api.setItemGroupReadService(itemGroupReadService);
        api.setItemGroupWriteService(itemGroupWriteService);
        api.setItemRuleGroupReadService(itemRuleGroupReadService);
        api.setScheduleTaskWriteService(scheduleTaskWriteService);
        when(itemGroupReadService.findById(any())).thenReturn(Response.ok(group));
        when(itemGroupReadService.findByCriteria(any())).thenReturn(Response.ok(new Paging<>()));
        when(itemGroupWriteService.create(any())).thenReturn(Response.ok(1L));
        when(itemGroupReadService.checkName(any())).thenReturn(Response.ok(true));
        when(itemRuleGroupReadService.findByGroupId(any())).thenReturn(Response.ok(Lists.newArrayList()));

    }

    @Test
    public void testFindByIdFail() {
        when(itemGroupReadService.findById(any())).thenReturn(Response.fail(
                "item.group.find.fail"
        ));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("item.group.find.fail");
        api.getById(1L);
    }

    @Test
    public void testFindByIdSuccess() {
        when(itemGroupReadService.findById(any())).thenReturn(Response.fail(
                "item.group.find.fail"
        ));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("item.group.find.fail");
        api.getById(1L);
    }


    @Test
    public void testCreateWithDumplateName() {
        when(itemGroupReadService.checkName(any())).thenReturn(Response.ok(false));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("item.group.name.duplicate");
        api.create(group);
    }

    @Test
    public void testCreateSuccess() {
        when(itemGroupWriteService.create(any())).thenReturn(Response.ok(1L));
        Long id = api.create(group);
        assertThat(id, is(1L));
    }

    @Test
    public void testPagingSuccess() {
        when(itemGroupReadService.findByCriteria(any())).thenReturn(Response.ok(new Paging<>(1L,Lists.newArrayList(group))));
        Paging<ItemGroup> result = api.findBy(null,null,null,null,null,null,null,null);
        assertThat(result.getTotal(), is(1L));
        assertThat(result.getData().size(), is(1));
    }

    @Test
    public void testCheckName() {
        when(itemGroupReadService.checkName(any())).thenReturn(Response.ok(false));
        Boolean result = api.check("test");
        assertThat(result, is(false));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemGroupWriteService.update(any())).thenReturn(Response.ok(true));
        Boolean result = api.update(group);
        assertThat(result, is(true));
    }

    @Test
    public void updateAutoRuleSuccess() {
        when(itemGroupWriteService.updateAutoRule(any(),any())).thenReturn(Response.ok(true));
        Boolean result = api.updateAutoRule(group.getId(),new ItemGroupAutoRule());
        assertThat(result, is(true));
    }

    @Test
    public void updateAutoRuleWithoutId() {
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("item.group.id.not.exist");
        Boolean result = api.updateAutoRule(null,new ItemGroupAutoRule());
        assertThat(result, is(false));
    }

    @Test
    public void DeleteWithUsedFail(){
        when(itemRuleGroupReadService.findByGroupId(any())).thenReturn(Response.ok(Lists.newArrayList(new ItemRuleGroup())));
        thrown.expect(JsonResponseException.class);
        thrown.expectMessage("item.group.is.used");
        Boolean result = api.delete(1L);
        assertThat(result, is(false));
    }

    @Test
    public void DeleteSuccess(){
        when(itemGroupWriteService.delete(any())).thenReturn(Response.ok(true));
        when(scheduleTaskWriteService.creates(any())).thenReturn(Response.ok(1));
        Boolean result = api.delete(1L);
        assertThat(result, is(true));
    }


}
