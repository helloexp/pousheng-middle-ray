package com.pousheng.middle.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousheng.middle.task.dto.ItemGroupTask;
import io.terminus.common.utils.JsonMapper;

import java.io.IOException;

/**
 * @author zhaoxw
 * @date 2018/5/12
 */
public class TaskTransUtil {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.nonEmptyMapper().getMapper();

    public static ItemGroupTask trans(String taskJson) {
        if (taskJson != null) {
            try {
                return OBJECT_MAPPER.readValue(taskJson,ItemGroupTask.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;

    }
}
