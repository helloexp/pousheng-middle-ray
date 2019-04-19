package com.pousheng.middle.task.impl.converter;

import com.pousheng.middle.task.dto.TaskDTO;
import com.pousheng.middle.task.model.Task;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 16:55<br/>
 */
@Component
public class TaskLogicConverter {
    public TaskDTO domain2dto(Task task) {
        if (task == null) {
            return null;
        }

        TaskDTO dto = new TaskDTO();
        BeanMapper.copy(task, dto);
        if (!StringUtils.isEmpty(task.getContextJson())) {
            Map<String, Object> content = JsonMapper.nonEmptyMapper().fromJson(task.getContextJson(), HashMap.class);
            dto.setContent(content);
        }
        return dto;
    }
}
