package com.pousheng.middle.web.item;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.service.MappingWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;


/**
 * Description: 商品映射推送比例设置
 * User: support 9
 * Date: 2018/8/31
 */
@Api(description = "商品映射API")
@RestController
@RequestMapping("/api/open-client")
@Slf4j
public class ItemMappings {

    private MappingWriteService mappingWriteService;
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    @Autowired
    public ItemMappings(MappingWriteService mappingWriteService,
                        PoushengCompensateBizReadService poushengCompensateBizReadService) {
        this.mappingWriteService = mappingWriteService;
        this.poushengCompensateBizReadService = poushengCompensateBizReadService;

    }
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @ApiOperation("设置商品推送比例")
    @PostMapping(value = "/item-mapping/{id}/ratio", produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("设置商品推送比例")
    public boolean updateSkuCode(@PathVariable("id") Long id,
                                 @RequestParam(required = false) String ratio) {
        if (log.isDebugEnabled()) {
            log.debug("update item-mapping(id:{}) set ratio:{}", id, ratio);
        }
        Integer rat = null;
        try {
            if (!StringUtils.isEmpty(ratio)) {
                rat = Integer.valueOf(ratio);
            }
        } catch (NumberFormatException nfe) {
            log.error("ratio:{} is not number", ratio);
            throw new JsonResponseException("ratio.setting.illegal");
        }
        if (Objects.nonNull(rat) && (rat <= 0 || rat > 100)) {
            log.error("fail to setting ratio:{}", ratio);
            throw new JsonResponseException("ratio.setting.illegal");
        }
        Response<Boolean> response = mappingWriteService.updatePushRatio(id, rat);
        if (!response.getResult()) {
            log.error("fail to update item-mapping(id:{}) ratio:{}, cause: {}", id, ratio, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("update item-mapping(id:{}) set ratio:{}, res:{}", id, ratio, response.getResult());
        }
        return response.getResult();
    }

    @ApiOperation("批量导入设置商品推送比例")
    @PostMapping(value = "/item-mapping/import")
    @OperationLogType("批量导入设置商品推送比例")
    public void importFile(String url) {
        if (StringUtils.isEmpty(url)) {
            log.error("faild import file(path:{}) ,cause: file path is empty", url);
            throw new JsonResponseException("file.path.is.empty");
        }
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_ITEM_PUSH_RATIO.toString());
        biz.setContext(url);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo 第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询导入文件的处理记录")
    @RequestMapping(value = "/import/result/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> create(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                @RequestParam(required = false, value = "pageSize") Integer pageSize) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizType(PoushengCompensateBizType.IMPORT_ITEM_PUSH_RATIO.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }
}
