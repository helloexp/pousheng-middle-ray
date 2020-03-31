package service;

import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.service.ItemGroupWriteServiceImpl;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.item.enums.AttributeEnum;
import com.pousheng.middle.item.enums.AttributeRelationEnum;
import io.terminus.common.model.Response;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author zhaoxw
 * @date 2018/5/5
 */
public class ItemGroupWriteServiceTest extends AbstractServiceTest {


    @Configuration
    public static class MockitoBeans {
        @MockBean
        private ItemGroupDao itemGroupDao;
        @SpyBean
        private ItemGroupWriteServiceImpl itemGroupWriteService;

    }

    @Override
    protected Class<?> mockitoBeans() {
        return ItemGroupWriteServiceTest.MockitoBeans.class;
    }


    ItemGroupDao itemGroupDao;
    ItemGroupWriteServiceImpl itemGroupWriteService;

    @Override
    protected void init() {
        itemGroupDao = get(ItemGroupDao.class);
        itemGroupWriteService = get(ItemGroupWriteServiceImpl.class);
    }

    private ItemGroup mock() {
        List<ItemGroupAutoRule> rules = Lists.newArrayList();
        rules.add(new ItemGroupAutoRule().name("brandId").relation(1).value("test"));
        return new ItemGroup().id(1L).name("test").auto(false).relatedNum(0L).groupRule(rules);
    }

    @Test
    public void testCreateSuccess() {
        when(itemGroupDao.create(any())).thenReturn(true);
        Response<Long> response = itemGroupWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(1L));
    }


    @Test
    public void testCreateUnknownEx() {
        when(itemGroupDao.create(any())).thenReturn(false);
        Response<Long> response = itemGroupWriteService.create(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.create.fail"));
    }

    @Test
    public void testUpdateSuccess() {
        when(itemGroupDao.update(any())).thenReturn(true);
        Response<Boolean> response = itemGroupWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testUpdateUnknownEx() {
        when(itemGroupDao.update(any())).thenReturn(false);
        Response<Boolean> response = itemGroupWriteService.update(mock());
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.update.fail"));
    }

    @Test
    public void testUpdateAutoRuleWithWrongId(){
        when(itemGroupDao.findById((Long)any())).thenReturn(null);
        ItemGroupAutoRule rule=new ItemGroupAutoRule();
        rule.name(AttributeEnum.BRAND.value()).relation(AttributeRelationEnum.IN.value()).value("abc");
        Response<Boolean> response = itemGroupWriteService.updateAutoRule(1L,rule);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.not.exist"));
    }


    @Test
    public void testUpdateAutoRuleUnknownEx(){
        ItemGroup itemGroup=new ItemGroup().name("测试").relatedNum(0L).auto(false);
        when(itemGroupDao.findById((Long)any())).thenReturn(itemGroup);
        when(itemGroupDao.update(any())).thenThrow(new NullPointerException());
        ItemGroupAutoRule rule=new ItemGroupAutoRule();
        rule.name(AttributeEnum.BRAND.value()).relation(AttributeRelationEnum.IN.value()).value("abc");
        Response<Boolean> response = itemGroupWriteService.updateAutoRule(1L,rule);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.update.fail"));
    }


    @Test
    public void testUpdateAutoRuleSuccess(){
        ItemGroup itemGroup=new ItemGroup().name("测试").relatedNum(0L).auto(false);
        when(itemGroupDao.findById((Long)any())).thenReturn(itemGroup);
        when(itemGroupDao.update(any())).thenReturn(true);
        ItemGroupAutoRule rule=new ItemGroupAutoRule();
        rule.name(AttributeEnum.BRAND.value()).relation(AttributeRelationEnum.IN.value()).value("abc");
        Response<Boolean> response = itemGroupWriteService.updateAutoRule(1L,rule);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }


    @Test
    public void testDeleteUnknownEx(){
        when(itemGroupDao.delete(any())).thenThrow(new NullPointerException());
        Response<Boolean> response = itemGroupWriteService.delete(1L);
        assertThat(response.isSuccess(), is(Boolean.FALSE));
        assertThat(response.getError(), is("item.group.delete.fail"));
    }

    @Test
    public void testDeleteSuccess(){
        when(itemGroupDao.delete(any())).thenReturn(true);
        Response<Boolean> response = itemGroupWriteService.delete(1L);
        assertThat(response.isSuccess(), is(Boolean.TRUE));
        assertThat(response.getResult(), is(Boolean.TRUE));
    }



}


