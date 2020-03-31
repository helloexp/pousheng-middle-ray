package com.pousheng.middle.web.utils.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.enums.TaskStatusEnum;
import com.pousheng.middle.task.enums.TaskTypeEnum;
import com.pousheng.middle.task.model.ScheduleTask;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;

/**
 * @author zhaoxw
 * @date 2018/5/11
 */
public class ScheduleTaskUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();

    public static ScheduleTask transItemGroupTask(ItemGroupTask itemGroupTask) {
        try {
            if(itemGroupTask.getUserId()==null){
                throw new JsonResponseException("permission.check.current.user.empty");
            }
            ScheduleTask scheduleTask = new ScheduleTask();
            String extra = OBJECT_MAPPER.writeValueAsString(itemGroupTask);
            scheduleTask.setStatus(TaskStatusEnum.INIT.value());
            scheduleTask.setType(TaskTypeEnum.ITEM_GROUP.value());
            scheduleTask.setBusinessId(itemGroupTask.getGroupId());
            scheduleTask.setUserId(itemGroupTask.getUserId());
            scheduleTask.setBusinessType(itemGroupTask.getType());
            scheduleTask.setExtraJson(extra);
            return scheduleTask;
        } catch (JsonProcessingException e) {
            throw new JsonResponseException(e.getMessage());
        }
    }

    public static ScheduleTask transItemGroupImportTask(ItemGroupTask itemGroupTask) {
        try {
            if(itemGroupTask.getUserId()==null){
                throw new JsonResponseException("permission.check.current.user.empty");
            }
            ScheduleTask scheduleTask = new ScheduleTask();
            String extra = OBJECT_MAPPER.writeValueAsString(itemGroupTask);
            scheduleTask.setStatus(TaskStatusEnum.INIT.value());
            scheduleTask.setType(TaskTypeEnum.ITEM_GROUP_IMPORT.value());
            scheduleTask.setBusinessId(itemGroupTask.getGroupId());
            scheduleTask.setUserId(itemGroupTask.getUserId());
            scheduleTask.setBusinessType(itemGroupTask.getType());
            scheduleTask.setExtraJson(extra);
            return scheduleTask;
        } catch (JsonProcessingException e) {
            throw new JsonResponseException(e.getMessage());
        }
    }




}
