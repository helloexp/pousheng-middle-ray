package com.pousheng.middle.web.order.component;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.convert.ReverseExpressConvert;
import com.pousheng.middle.order.convert.ReverseHeadlessInfoConvert;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressInfoDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseLogisticsDto;
import com.pousheng.middle.order.enums.HeadlessProcessTypeEnum;
import com.pousheng.middle.order.model.ReverseExpressInfo;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import com.pousheng.middle.order.service.ReverseHeadlessInfoReadService;
import com.pousheng.middle.order.service.ReverseHeadlessInfoWriteService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/5
 */

@Component
@Slf4j
public class ReverseHeadlessInfoLogic {

    @Autowired
    private ReverseHeadlessInfoReadService reverseHeadlessInfoReadService;
    @Autowired
    private ReverseHeadlessInfoWriteService reverseHeadlessInfoWriteService;

    public Response<Boolean> createOrUpdate(ReverseHeadlessDto reverseHeadlessDto) {

        try {

            Response<ReverseHeadlessInfo> response = reverseHeadlessInfoReadService.findReverseHeadlessInfoByUniqueNo(
                reverseHeadlessDto.getUniqueNo());

            if (response.isSuccess()) {
                if (Objects.isNull(response.getResult())) {
                    Response<Long> createResponse = reverseHeadlessInfoWriteService.createReverseHeadlessInfo(
                        ReverseHeadlessInfoConvert.convertHeadLessDtoToDo(reverseHeadlessDto, null));
                    if (!createResponse.isSuccess()) {
                        Response.ok(Boolean.FALSE);
                    }
                } else {
                    reverseHeadlessInfoWriteService.updateReverseHeadlessInfo(
                        ReverseHeadlessInfoConvert.convertHeadLessDtoToDo(reverseHeadlessDto, response.getResult()));
                }
            } else {
                log.error(response.getError());
                return Response.fail(response.getError());
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.headless.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.headless.fail");
        }
        return Response.ok(Boolean.TRUE);

    }


    public Response<Boolean> batchCreateOrUpdate(List<ReverseHeadlessDto> reverseHeadlessDtoList) {

        try {
            List<ReverseHeadlessInfo> reverseHeadlessInfoList = ReverseHeadlessInfoConvert.convertHeadLessDtoToDoList(reverseHeadlessDtoList);
            if (!CollectionUtils.isEmpty(reverseHeadlessInfoList)) {
                reverseHeadlessInfoWriteService.batchCreateOrUpdate(reverseHeadlessInfoList);
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.express.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.express.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    public Paging<ReverseHeadlessDto> paging(ReverseHeadlessCriteria reverseHeadlessCriteria) {

        Response<Paging<ReverseHeadlessInfo>> response = reverseHeadlessInfoReadService.paging(reverseHeadlessCriteria);
        if (response.isSuccess()) {
            List<ReverseHeadlessDto> ReverseHeadlessDtos = ReverseHeadlessInfoConvert.convertHeadLessListDoToDto(
                response.getResult().getData());
            Paging<ReverseHeadlessDto> paging = new Paging(response.getResult().getTotal(),
                ReverseHeadlessDtos);
            return paging;
        }
        return Paging.empty();
    }

    /**
     * 无头件数据统计
     *
     * @return
     */
    public Long countReverseHeadless(ReverseHeadlessCriteria reverseHeadlessCriteria) {

        try {
            return reverseHeadlessInfoReadService.countReverseHeadless(reverseHeadlessCriteria).getResult();
        } catch (Exception e) {
            log.error("count.reverse.headless.fail,error={}", Throwables.getStackTraceAsString(e));
            return 0L;
        }
    }

    /**
     * 无头件数量统计
     *
     * @return
     */
    public Response<ReverseLogisticsDto> countReverseHeadless() {
        ReverseLogisticsDto reverseLogisticsDto = new ReverseLogisticsDto();
        try {
            //今日新增无头件
            ReverseHeadlessCriteria reverseHeadlessCriteria = new ReverseHeadlessCriteria();
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.INIT.name());
            reverseHeadlessCriteria.setStartAt(DateTime.now().withTimeAtStartOfDay().toDate());

            reverseLogisticsDto.setTodayNoProcessCount(
                reverseHeadlessInfoReadService.countReverseHeadless(reverseHeadlessCriteria).getResult());

            //待处理无头件总计
            reverseHeadlessCriteria.setStartAt(null);
            reverseLogisticsDto.setTotalNoProcessCount(
                reverseHeadlessInfoReadService.countReverseHeadless(reverseHeadlessCriteria).getResult());

            //无头件退回
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.REFUSE.name());
            reverseLogisticsDto.setRefuseCount(
                reverseHeadlessInfoReadService.countReverseHeadless(reverseHeadlessCriteria).getResult());

            //无头件盘盈
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.PROFIT.name());
            reverseLogisticsDto.setProfitCount(
                reverseHeadlessInfoReadService.countReverseHeadless(reverseHeadlessCriteria).getResult());
        } catch (Exception e) {
            log.error("count.reverse.headless.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("count.reverse.headless.fail");
        }
        return Response.ok(reverseLogisticsDto);
    }

}
