package com.pousheng.middle.web.order;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.convert.ReverseLogisticsHelp;
import com.pousheng.middle.order.dto.reverseLogistic.*;
import com.pousheng.middle.order.enums.ReverseLogisticsTypeEnum;
import com.pousheng.middle.web.order.component.ReverseExpressInfoLogic;
import com.pousheng.middle.web.order.component.ReverseHeadlessInfoLogic;
import com.pousheng.middle.web.order.component.ReverseInstoreInfoLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * @author bernie
 * @date 2019/6/11
 */
@RestController
@Slf4j
@RequestMapping("api/reverse/logistics")
@Api(description = "逆物流")
public class AdminReverseLogisticsController {

    @Autowired
    private ReverseInstoreInfoLogic reverseInstoreInfoLogic;
    @Autowired
    private ReverseHeadlessInfoLogic reverseHeadlessInfoLogic;
    @Autowired
    private ReverseExpressInfoLogic reverseExpressInfoLogic;

    @ApiOperation("售后包裹单查询")
    @RequestMapping(value = "/query", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ReverseLogisticsQueryResponse> headlessStatistics(ReverseLogisticsQueryRequest request) {

        log.debug("Reverse.logistics.query.request ={}", JsonMapper.nonEmptyMapper().toJson(request));

        try {
            List<ReverseLogisticsTypeEnum> reverseLogisticsTypeEnumList = ReverseLogisticsHelp.checkQueryList(request);
            if (CollectionUtils.isEmpty((reverseLogisticsTypeEnumList))) {
                reverseLogisticsTypeEnumList.add(ReverseLogisticsTypeEnum.EXPRESS);
            }
            ReverseLogisticsQueryResponse reverseLogisticsQueryResponse = new ReverseLogisticsQueryResponse();

            int i = 0;
            for (ReverseLogisticsTypeEnum reverseLogisticsTypeEnum : reverseLogisticsTypeEnumList) {
                //分页查询数据
                if (i == 0) {
                    reverseLogisticsQueryResponse.setCurrentType(reverseLogisticsTypeEnum.name());
                    switch (reverseLogisticsTypeEnumList.get(i)) {
                        case INSTORE: {
                            Paging<ReverseInstoreDto> paging=reverseInstoreInfoLogic.paging(ReverseLogisticsHelp.convertInstoreCriteria(request));
                            reverseLogisticsQueryResponse.setPage(paging );
                            reverseLogisticsQueryResponse.setInstoreCount(paging.getTotal());
                            break;
                        }
                        case EXPRESS: {
                            Paging<ReverseExpressInfoDto>  paging= reverseExpressInfoLogic.paging(ReverseLogisticsHelp.convertExpressCriteria(request));
                            reverseLogisticsQueryResponse.setPage(paging);
                            reverseLogisticsQueryResponse.setExpressCount(paging.getTotal());
                            break;
                        }
                        case HEADLESS: {
                            Paging<ReverseHeadlessDto> paging= reverseHeadlessInfoLogic.paging(ReverseLogisticsHelp.convertHeadlessCriteria(request));
                            reverseLogisticsQueryResponse.setPage(paging);
                            reverseLogisticsQueryResponse.setHeadlessCount(paging.getTotal());
                            break;
                        }
                        default:
                            reverseLogisticsQueryResponse.setPage(Paging.empty());
                    }
                } else {
                    //查询数量
                    switch (reverseLogisticsTypeEnum) {
                        case INSTORE: {
                            reverseLogisticsQueryResponse.setInstoreCount(
                                reverseInstoreInfoLogic
                                    .countReverseInstore(ReverseLogisticsHelp.convertInstoreCriteria(request)));
                            break;
                        }
                        case EXPRESS: {
                            reverseLogisticsQueryResponse.setExpressCount(
                                reverseExpressInfoLogic
                                    .countReverseExpress(ReverseLogisticsHelp.convertExpressCriteria(request)));
                            break;
                        }
                        case HEADLESS: {
                            reverseLogisticsQueryResponse.setHeadlessCount( reverseHeadlessInfoLogic.countReverseHeadless(
                                ReverseLogisticsHelp.convertHeadlessCriteria(request)));
                            break;
                        }
                        default:
                    }
                }
                i++;
            }
            return Response.ok(reverseLogisticsQueryResponse);
        } catch (Exception e) {
            log.error("query.reverse.info.error {}", Throwables.getStackTraceAsString(e));
            return Response.fail("query.reverse.info.error");
        }
    }

    @ApiOperation("售后无头件统计")
    @RequestMapping(value = "/headless/statistics", method = RequestMethod.GET)
    public Response<ReverseLogisticsDto> reverseHeadlessStatistics(ReverseLogisticsQueryRequest request) {
        return reverseHeadlessInfoLogic.countReverseHeadless();

    }

    public static void main(String[] args) {

        ReverseLogisticsQueryRequest request=new ReverseLogisticsQueryRequest();
        request.setExpressNo("2222");
        System.out.println(JsonMapper.nonEmptyMapper().toJson(request));
    }


}
