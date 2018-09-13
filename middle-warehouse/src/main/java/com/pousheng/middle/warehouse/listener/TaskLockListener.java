package com.pousheng.middle.warehouse.listener;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.event.TaskLockEvent;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/9/13下午11:22
 */
@Slf4j
@Component
public class TaskLockListener {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        eventBus.register(this);
    }


    /**
     * 对于超时这类异常进行后续补偿措施，biz业务轮询
     * @param event 发货单id
     */
    @Subscribe
    @AllowConcurrentEvents
    public void createShipmentResultTask(TaskLockEvent event) {
        log.info("create lock task shipment id is {}", event.getShipmentId());
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.STOCK_API_TIME_OUT.toString());
        biz.setContext(mapper.toJson(event.getShipmentId()));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }
}
