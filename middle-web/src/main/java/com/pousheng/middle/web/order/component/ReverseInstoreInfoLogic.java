package com.pousheng.middle.web.order.component;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.convert.ReverseHeadlessInfoConvert;
import com.pousheng.middle.order.convert.ReverseInstoreInfoConvert;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseInstoreCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseInstoreDto;
import com.pousheng.middle.order.model.ReverseHeadlessInfo;
import com.pousheng.middle.order.model.ReverseInstoreInfo;
import com.pousheng.middle.order.service.ReverseInstoreInfoReadService;
import com.pousheng.middle.order.service.ReverseInstoreInfoWriteService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author bernie
 * @date 2019/6/5
 */

@Component
@Slf4j
public class ReverseInstoreInfoLogic {

    @Autowired
    private ReverseInstoreInfoReadService reverseInstoreInfoReadService;
    @Autowired
    private ReverseInstoreInfoWriteService reverseInstoreInfoWriteService;

    public Response<Boolean> createOrUpdate(ReverseInstoreDto reverseInstoreDto) {

        try {

            Response<ReverseInstoreInfo> response = reverseInstoreInfoReadService
                .findReverseExpressInfoByInstoreNoAndDetailNo(reverseInstoreDto.getInstoreNo(),
                    reverseInstoreDto.getInstoreDetailNo());

            if (response.isSuccess()) {
                if (Objects.isNull(response.getResult())) {
                    Response<Long> createResponse = reverseInstoreInfoWriteService.createReverseInstoreInfo(
                        ReverseInstoreInfoConvert.convertDtoToDomain(reverseInstoreDto, null));
                    if (!createResponse.isSuccess()) {
                        Response.ok(Boolean.FALSE);
                    }
                } else {
                    reverseInstoreInfoWriteService.updateReverseInstoreInfo(
                        ReverseInstoreInfoConvert.convertDtoToDomain(reverseInstoreDto, response.getResult()));
                }
            } else {
                log.error(response.getError());
                return Response.fail(response.getError());
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.instore.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.instore.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    public Response<Boolean> batchCreateOrUpdate(List<ReverseInstoreDto> reverseInstoreDtoList) {

        try {
            List<ReverseInstoreInfo> reverseInstoreInfoList = ReverseInstoreInfoConvert.convertDtoToDomainList(reverseInstoreDtoList);
            if (!CollectionUtils.isEmpty(reverseInstoreInfoList)) {
                reverseInstoreInfoWriteService.batchCreateOrUpdate(reverseInstoreInfoList);
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.instore.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.instore.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    public Paging<ReverseInstoreDto> paging(ReverseInstoreCriteria reverseInstoreCriteria) {

        Response<Paging<ReverseInstoreInfo>> response = reverseInstoreInfoReadService.paging(reverseInstoreCriteria);
        if (response.isSuccess() && !response.getResult().isEmpty()) {
            List<ReverseInstoreDto> reverseInstoreDtos = ReverseInstoreInfoConvert.convertInstoreListDoToDto(
                response.getResult().getData());
            Paging<ReverseInstoreDto> paging = new Paging(response.getResult().getTotal(),
                reverseInstoreDtos);
            return paging;
        }

        return Paging.empty();
    }

    public Long countReverseInstore(ReverseInstoreCriteria reverseInstoreCriteria) {

        try {
            return reverseInstoreInfoReadService.countReverseInstore(reverseInstoreCriteria).getResult();
        } catch (Exception e) {
            log.error("count.reverse.instore.fail,error={}", Throwables.getStackTraceAsString(e));
            return 0L;
        }
    }

}
