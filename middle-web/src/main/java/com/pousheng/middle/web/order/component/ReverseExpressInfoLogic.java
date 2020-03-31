package com.pousheng.middle.web.order.component;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.convert.ReverseExpressConvert;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseExpressInfoDto;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.model.ReverseExpressInfo;
import com.pousheng.middle.order.service.ReverseExpressInfoReadService;
import com.pousheng.middle.order.service.ReverseExpressInfoWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/4
 */
@Component
@Slf4j
public class ReverseExpressInfoLogic {

    @Autowired
    private ReverseExpressInfoReadService reverseExpressInfoReadService;
    @Autowired
    private ReverseExpressInfoWriteService reverseExpressInfoWriteService;

    @Transactional(rollbackFor = Exception.class)
    public Response<Boolean> createOrUpdate(ReverseExpressInfoDto expressInfoDto) {

        try {

            Response<ReverseExpressInfo> response = reverseExpressInfoReadService
                .findReverseExpressInfoByTransferOrderIdAndLineNo(expressInfoDto.getTransferOrderId(),
                    expressInfoDto.getLineNo());
            if (response.isSuccess()) {
                if (Objects.nonNull(response.getResult())) {
                    return reverseExpressInfoWriteService.updateReverseExpressInfo(
                        ReverseExpressConvert.convertExpressDtoToDo(expressInfoDto, response.getResult()));
                } else {
                    Response<Long> createResponse = reverseExpressInfoWriteService.createReverseExpressInfo(
                        ReverseExpressConvert.convertExpressDtoToDo(expressInfoDto, response.getResult()));
                    if (!createResponse.isSuccess()) {
                        Response.ok(Boolean.TRUE);
                    }
                }
            } else {
                log.error(response.getError());
                return Response.fail(response.getError());
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.express.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.express.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    public Response<Boolean> batchCreateOrUpdate(List<ReverseExpressInfoDto> expressInfoDtoList) {

        try {
            List<ReverseExpressInfo> expressInfoList = ReverseExpressConvert.convertExpressListDtoToDoList(
                expressInfoDtoList);
            if (!CollectionUtils.isEmpty(expressInfoList)) {
                reverseExpressInfoWriteService.batchCreateOrUpdate(expressInfoList);
            }
        } catch (Exception e) {
            log.error("createdOrUpdate.reverse.express.fail,error={}", Throwables.getStackTraceAsString(e));
            return Response.fail("createdOrUpdate.reverse.express.fail");
        }
        return Response.ok(Boolean.TRUE);

    }

    public Paging<ReverseExpressInfoDto> paging(ReverseExpressCriteria reverseExpressCriteria) {

        Response<Paging<ReverseExpressInfo>> response = reverseExpressInfoReadService.paging(reverseExpressCriteria);
        if (response.isSuccess() && !response.getResult().isEmpty()) {
            List<ReverseExpressInfoDto> reverseExpressInfoDtoList = ReverseExpressConvert.convertExpressListDoToDto(
                response.getResult().getData());
            Paging<ReverseExpressInfoDto> paging = new Paging(response.getResult().getTotal(),
                reverseExpressInfoDtoList);
            return paging;
        }

        return Paging.empty();
    }

    public Long countReverseExpress(ReverseExpressCriteria reverseExpressCriteria) {

        try {
            return reverseExpressInfoReadService.countReverseInstore(reverseExpressCriteria).getResult();
        } catch (Exception e) {
            log.error("count.reverse.express.fail,error={}", Throwables.getStackTraceAsString(e));
            return 0L;
        }
    }

}
