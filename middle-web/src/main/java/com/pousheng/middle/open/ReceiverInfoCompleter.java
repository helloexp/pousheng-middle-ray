package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.hksyc.dto.trade.ReceiverInfoHandleResult;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.Municipality;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.parana.order.model.ReceiverInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by cp on 9/4/17
 */
@Component
@Slf4j
public class ReceiverInfoCompleter {

    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;
    @Autowired
    private GDMapSearchService gdMapSearchService;

    /**
     * 当地址无法解析是 截取前n位解析
     */
    private static final Integer INTERCEPT_LENGTH = 9;

    public void complete(ReceiverInfo receiverInfo){
        ReceiverInfoHandleResult handleResult = new ReceiverInfoHandleResult();
        handleResult.setSuccess(Boolean.TRUE);
        List<String> errors = Lists.newArrayList();
        Map<String,String> extraMap = Maps.newHashMap();
        //特殊处理直辖市,京东市一级的地址是区，用省的字段填充市
        List<String> municipalities = Lists.newArrayList(Municipality.SHANGHAI.getName(),Municipality.SHANGHAI.getDesc(),Municipality.BEIJING.getName(),Municipality.BEIJING.getDesc()
                ,Municipality.TIANJIN.getName(),Municipality.TIANJIN.getDesc(),Municipality.CHONGQING.getName(),Municipality.CHONGQING.getDesc());
        if (municipalities.contains(receiverInfo.getProvince())){
            if (!municipalities.contains(receiverInfo.getCity())){
                receiverInfo.setRegion(receiverInfo.getCity());
                receiverInfo.setCity(receiverInfo.getProvince());
            }
        }

        if (Arguments.isNull(receiverInfo.getProvince()) || Arguments.isNull(receiverInfo.getCity())) {
            // 调用高德api
            Optional<Location> result = queryAddress(receiverInfo.getDetail());
            if (result != null && result.isPresent()) {
                Location location = result.get();
                log.info("api from gd location info {}", location);
                receiverInfo.setCity(location.getCityname());
                receiverInfo.setProvince(location.getPname());
            } else {
                Optional<Location> vagueResult = queryAddress(receiverInfo.getDetail().substring(0, receiverInfo.getDetail().length() > INTERCEPT_LENGTH ? INTERCEPT_LENGTH : receiverInfo.getDetail().length()));
                if (vagueResult != null && vagueResult.isPresent()) {
                    Location location = vagueResult.get();
                    log.info("api from gd vague location info {}", location);
                    receiverInfo.setCity(location.getCityname());
                    receiverInfo.setProvince(location.getPname());
                    extraMap.put(TradeConstants.VAUGE_ADDRESS, Boolean.TRUE.toString());
                }
            }
        }

        //目前中台省的pi都是1，所以这里直接写死，如果有变动的话这里也需要做对应的修改
        //移除省市区转换 removed by longjun.tlj
        Long provinceId = queryAddressId(1L,receiverInfo.getProvince());
        if (Arguments.notNull(provinceId)){
            receiverInfo.setProvinceId(Integer.valueOf(provinceId.toString()));
        } else {
            handleResult.setSuccess(Boolean.FALSE);
            errors.add("第三方渠道省：" + receiverInfo.getProvince()+ "未匹配到中台的省");
        }
        //如果市一级地址为空，则用区一级地址顶上
        if (!Objects.isNull(receiverInfo.getCity())&&StringUtils.isEmpty(receiverInfo.getCity().trim())){
            receiverInfo.setCity(receiverInfo.getRegion());
        }
        Long cityId = queryAddressId(provinceId,receiverInfo.getCity());
        if (Arguments.notNull(cityId)){
            receiverInfo.setCityId(Integer.valueOf(cityId.toString()));
        } else {
            handleResult.setSuccess(Boolean.FALSE);
            errors.add("第三方渠道市："+ receiverInfo.getCity()+ "未匹配到中台的市");
        }
        extraMap.put("handleResult", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(handleResult));
        receiverInfo.setExtra(extraMap);

    }

    public void completePushOrderAddress(OpenFullOrderAddress address){
        ReceiverInfoHandleResult handleResult = new ReceiverInfoHandleResult();
        handleResult.setSuccess(Boolean.TRUE);
        List<String> errors = Lists.newArrayList();
        //特殊处理直辖市,京东市一级的地址是区，用省的字段填充市
        List<String> municipalities = Lists.newArrayList(Municipality.SHANGHAI.getName(),Municipality.SHANGHAI.getDesc(),Municipality.BEIJING.getName(),Municipality.BEIJING.getDesc()
                ,Municipality.TIANJIN.getName(),Municipality.TIANJIN.getDesc(),Municipality.CHONGQING.getName(),Municipality.CHONGQING.getDesc());
        if (municipalities.contains(address.getProvince())){
            if (!municipalities.contains(address.getCity())){
                address.setRegion(address.getCity());
                address.setCity(address.getProvince());
            }
        }
        //目前中台省的pi都是1，所以这里直接写死，如果有变动的话这里也需要做对应的修改
        //移除省市区转换 removed by longjun.tlj
        Long provinceId = queryAddressId(1L,address.getProvince());
        if(Arguments.notNull(provinceId)){
            address.setProvinceId(provinceId);
        }
        Long cityId = queryAddressId(provinceId,address.getCity());
        if(Arguments.notNull(cityId)){
            address.setCityId(cityId);
        }
    }

    private Long queryAddressId(Long pid,String name){
        //pid为null则直接返回null
        if(Arguments.isNull(pid)){
            return null;
        }
        if(StringUtils.isEmpty(name)){
            return null;
        }
        Optional<WarehouseAddress> wo1 = warehouseAddressCacher.findByPidAndName(pid,name);
        if(wo1.isPresent()){
            return wo1.get().getId();
        }

        String splitName = name.substring(0,2);
        Optional<WarehouseAddress> wo2 = warehouseAddressCacher.findByPidAndName(pid,splitName);
        if(wo2.isPresent()){
            return wo2.get().getId();
        }

        return null;
    }

    private Optional<Location> queryAddress(String detail) {
        Response<Optional<Location>> locationRes = gdMapSearchService.searchByAddress(detail);
        if (!locationRes.isSuccess()) {
            log.error("find location by address:{} fail,error:{}", detail, locationRes.getError());
            return Optional.absent();
        }
        return locationRes.getResult();
    }

}
