package com.pousheng.middle.web.user;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.enums.ZoneGroupEnum;
import com.pousheng.middle.order.model.ZoneContract;
import com.pousheng.middle.order.service.ZoneContractReadService;
import com.pousheng.middle.order.service.ZoneContractWriteService;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static com.pousheng.middle.order.constant.TradeConstants.STATUS_DISABLE;

@Slf4j
@RestController
@RequestMapping("/api/zonecontracts")
public class ZoneContractsApis {


    @Autowired
    private EventBus eventBus;

    @Autowired
    private ZoneContractReadService zoneContractReadService;


    @Autowired
    private ZoneContractWriteService zoneContractWriteService;


    @GetMapping("/paging")
    @ApiOperation("分页查询区部联系人")
    public Paging<ZoneContract> paging(@RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize,
                                       @RequestParam(required = false,value = "zoneName") String zoneName) {
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-PAGING-START param: pageNo [{}] pageSize [{}] zoneName [{}]",pageNo,pageSize,zoneName);
        }
        Response<Paging<ZoneContract>> resp = zoneContractReadService.pagination(zoneName, pageNo, pageSize);

        if (!resp.isSuccess()) {

            throw new JsonResponseException(resp.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-PAGING-END param: pageNo [{}] pageSize [{}] zoneName [{}] ,resp: [{}]",pageNo,pageSize,zoneName,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp.getResult();
    }

    @ApiOperation("新建或者更新区部联系人")
    @PostMapping("/create")
    public Map<String, Long> editZoneContract(@RequestBody ZoneContract zoneContract) {
        String zoneContractStr = JsonMapper.nonEmptyMapper().toJson(zoneContract);
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-CREATE-START param: zoneContract [{}] ",zoneContractStr);
        }
        validateParm(zoneContract);
        Long id;
        if (Objects.isNull(zoneContract.getId())) {

            //todo 一个区部是否只有一个联系人

            //新建区部联系人
            Response<Long> response = zoneContractWriteService.create(zoneContract);
            if (!response.isSuccess()) {
                log.error("creatZoneContract failed,cause={}", response.getError());
                throw new JsonResponseException("zone.contract.create.fail");
            }
            id = response.getResult();

        } else {

            //更新区部联系人
            Response<Boolean> response = zoneContractWriteService.update(zoneContract);
            if (!response.isSuccess()) {
                log.error("updateZoneContract failed,cause={}", response.getError());
                throw new JsonResponseException("zone.contract.update.fail");
            }
            id = zoneContract.getId();

        }
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-CREATE-END param: zoneContract [{}] ,resp: [{}]",zoneContractStr,id);
        }
        return Collections.singletonMap("id", id);
    }

    @ApiOperation("删除区部联系人")
    @GetMapping("/del/{id}")
    public Response<Boolean> del(@PathVariable("id") Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-DEL-START param: id [{}] ",id);
        }
        ZoneContract zoneContract = new ZoneContract();
        zoneContract.setId(id);
        zoneContract.setStatus(STATUS_DISABLE);
        Response<Boolean> response = zoneContractWriteService.update(zoneContract);
        if (!response.isSuccess()) {
            log.error("zonecontract del failed,cause={}", response.getError());
            throw new JsonResponseException("zone.contract.delete.fail");
        }
        if(log.isDebugEnabled()){
            log.debug("API-ZONECONTRACTS-DEL-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response;
    }

    private void validateParm(ZoneContract zoneContract) {

        if (Arguments.isEmpty(zoneContract.getZoneId())) {
            throw new JsonResponseException("zone.id.empty");
        }
        if (Arguments.isEmpty(zoneContract.getZoneName())) {
            throw new JsonResponseException("zone.name.empty");
        }

        if (StringUtils.isEmpty(zoneContract.getName())) {
            throw new JsonResponseException("zone.contract.name.empty");

        }
        if (StringUtils.isEmpty(zoneContract.getEmail())) {
            throw new JsonResponseException("zone.contract.email.empty");
        }
        if (Objects.isNull(zoneContract.getGroup())) {
            throw new JsonResponseException("zone.contract.group.empty");
        }

        if (!ZoneGroupEnum.contains(zoneContract.getGroup())) {
            throw new JsonResponseException("zone.contract.group.illegal");
        }
    }
}
