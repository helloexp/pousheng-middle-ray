package com.pousheng.middle.web.order.component;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationWriteService;
import io.terminus.common.utils.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by penghui on 2018/1/18
 */
@Component
public class AutoCompensateLogic {

    @Autowired
    private AutoCompensationWriteService autoCompensationWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 同步失败，创建自动补偿任务
     */
    public void createAutoCompensationTask(Map<String,Object> param, Integer type,String error){
        AutoCompensation autoCompensation = new AutoCompensation();
        Map<String,String> extra = Maps.newHashMap();
        extra.put("param",mapper.toJson(param));
        extra.put("error",error);
        autoCompensation.setType(type);
        autoCompensation.setStatus(0);
        autoCompensation.setExtra(extra);
        autoCompensation.setTime(0);
        autoCompensationWriteService.create(autoCompensation);
    }

    /**
     * 同步成功，修改任务状态
     */
    public void updateAutoCompensationTask(Long id){
        AutoCompensation autoCompensation = new AutoCompensation();
        autoCompensation.setId(id);
        autoCompensation.setStatus(2);
        autoCompensationWriteService.update(autoCompensation);
    }

    /**
     * 同步失败，增加失败次数
     */
    public void autoCompensationTaskExecuteFail(AutoCompensation autoCompensation){
        AutoCompensation update = new AutoCompensation();
        update.setId(autoCompensation.getId());
        update.setStatus(0);
        if(autoCompensation.getTime() == null){
            autoCompensation.setTime(0);
        }
        update.setExtra(autoCompensation.getExtra());
        update.setTime(autoCompensation.getTime() + 1);
        autoCompensationWriteService.update(update);
    }

}
