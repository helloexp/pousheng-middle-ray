package dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.pousheng.middle.task.impl.dao.TaskDao;
import com.pousheng.middle.task.model.Task;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 15:51<br/>
 */
public class TaskDaoTest extends BaseDaoTest {
    @Autowired
    private TaskDao taskDao;

    private Task gen() {
        Task t = new Task();
        t.setType("type");
        t.setStatus("status");
        t.setDetailJson("{}");
        t.setContextJson("{}");
        return t;
    }

    @Test
    public void test_PagingByStatusAndType() {
        Task taskA = gen();
        Task taskB = gen();
        Task taskC = gen();
        taskC.setStatus("statusC");
        Task taskD = gen();
        taskD.setType("typeD");

        taskDao.creates(ImmutableList.of(taskA, taskB, taskC));
        taskDao.create(taskD);

        PageInfo page = PageInfo.of(1, 10);
        Map<String, Object> conditions = Maps.newHashMap();
        conditions.putAll(page.toMap());
        conditions.put("status", "status");
        conditions.put("type", "type");
        conditions.put("sort", "id_asc");
        conditions.put("exclude", Collections.singletonList(1));
        Paging<Task> found = taskDao.paging(conditions);
        assertThat(found.getTotal()).isEqualTo(1l);
    }
}
