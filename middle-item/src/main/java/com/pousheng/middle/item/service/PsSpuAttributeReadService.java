package com.pousheng.middle.item.service;

import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SpuAttribute;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/7
 */
public interface PsSpuAttributeReadService {

    Response<List<SpuAttribute>> findBySpuIds(List<Long> spuIds);
}
