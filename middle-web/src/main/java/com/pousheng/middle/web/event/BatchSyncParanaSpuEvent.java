package com.pousheng.middle.web.event;

import lombok.Data;

import java.util.List;

/**
 * 批量同步品牌事件
 * Created by songrenfei on 2017/6/7
 */
@Data
public class BatchSyncParanaSpuEvent {

    private String taskId;

    private List<Long> spuIds;
}
