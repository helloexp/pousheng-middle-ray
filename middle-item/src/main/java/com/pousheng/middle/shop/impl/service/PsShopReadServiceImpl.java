package com.pousheng.middle.shop.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.shop.impl.dao.ShopExtDao;
import com.pousheng.middle.shop.service.PsShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Params;
import io.terminus.parana.shop.impl.dao.ShopDao;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by songrenfei on 2017/12/6
 */
@Slf4j
@Service
@RpcProvider
public class PsShopReadServiceImpl implements PsShopReadService{

    @Autowired
    private ShopDao shopDao;
    @Autowired
    private ShopExtDao shopExtDao;


    @Override
    public Response<Paging<Shop>> pagination(String name, Long userId, Integer type, Integer status, String outerId, Long businessId, List<String> zoneIds, Integer pageNo, Integer pageSize) {
        try {
            Map<String, Object> criteria = Maps.newHashMap();
            criteria.put("name",Params.trimToNull(name));
            criteria.put("userId",userId);
            criteria.put("type",type);
            criteria.put("status",status);
            criteria.put("outerId",outerId);
            criteria.put("businessId",businessId);
            criteria.put("zoneIds",zoneIds);

            PageInfo page = new PageInfo(pageNo, pageSize);
            return Response.ok(shopDao.paging(page.getOffset(), page.getLimit(), criteria));
        } catch (Exception e) {
            log.error("paging shop failed, name={}, userId={}, type={}, status={},outerId={},business={}, pageNo={}, pageSize={}, cause:{}",
                    name, userId, type, status,outerId,businessId, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }

    @Override
    public Response<Optional<Shop>> findByOuterIdAndBusinessId(String outerId, Long businessId) {
        try {
            return Response.ok(Optional.fromNullable(shopExtDao.findByOuterIdAndBusinessId(outerId,businessId)));
        } catch (Exception e){
            log.error("find shop by outer id:{} business id:{},cause:{}",outerId,businessId,Throwables.getStackTraceAsString(e));
            return Response.fail("shop.find.fail");
        }
    }



	@Override
	public Response<Paging<Shop>> pagingWithExpresssCompany(String name, Long userId, Integer type, Integer status,
			String outerId, Long businessId, List<String> zoneIds, String expresssCompany, Integer pageNo,
			Integer pageSize) {
		try {
			Map<String, Object> criteria = Stream
					.of(new Object[][] { { "name", Params.trimToNull(name) }, { "userId", userId }, { "type", type },
							{ "status", status }, { "outerId", outerId }, { "businessId", businessId },
							{ "zoneIds", zoneIds }, { "expresssCompany", expresssCompany } })
					.filter(data -> data[1] != null)
					.collect(Collectors.toMap(data -> data[0].toString(), data -> data[1]));

			PageInfo page = new PageInfo(pageNo, pageSize);
			return Response.ok(shopExtDao.pagingWithExpresssCompany(page.getOffset(), page.getLimit(), criteria));
		} catch (Exception e) {
			log.error(
					"paging shop failed, name={}, userId={}, type={}, status={},outerId={},business={}, expresssCompany={}, pageNo={}, pageSize={}, cause:{}",
					name, userId, type, status, outerId, businessId, expresssCompany, pageNo, pageSize,
					Throwables.getStackTraceAsString(e));
			return Response.fail("shop.find.fail");
		}
	}


}
