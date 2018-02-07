package com.pousheng.middle.order.cache;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.impl.dao.AddressGpsDao;
import com.pousheng.middle.order.model.AddressGps;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author:  songrenfei
 * Date: 2017-12-12
 */
@Component
@Slf4j
public class AddressGpsCacher {

    private final LoadingCache<String, AddressGps> addressGpsLoadingCache;

    @Autowired
    public AddressGpsCacher(@Value("${cache.duration.in.minutes: 60}")
                               Integer duration, final AddressGpsDao addressGpsDao) {
        this.addressGpsLoadingCache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.HOURS)
                .maximumSize(2000)
                .build(new CacheLoader<String, AddressGps>() {
                    @Override
                    public AddressGps load(String businessIdAndType) throws Exception {
                        List<String> idAndType = Splitters.COLON.splitToList(businessIdAndType);

                        final AddressGps addressGps = addressGpsDao.findByBusinessIdAndType(Long.valueOf(idAndType.get(0)), AddressBusinessType.fromInt(Integer.valueOf(idAndType.get(1))));
                        if (addressGps == null) {
                            log.error("not find address gps by businessId:{} and business type:{}", Long.valueOf(idAndType.get(0)),Integer.valueOf(idAndType.get(1)));
                            throw new ServiceException("address.gps.not.found");
                        }
                        return addressGps;
                    }
                });
    }

    /**
     * 根据业务id和业务类型查询地址定位信息
     *
     * @param businessId 业务id
     * @param businessType 业务类型{@link AddressBusinessType}
     * @return 对应的定位
     */
    public AddressGps findByBusinessIdAndType(Long businessId,Integer businessType){
        return addressGpsLoadingCache.getUnchecked(Joiners.COLON.join(businessId,businessType));
    }
}
