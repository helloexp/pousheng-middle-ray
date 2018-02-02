package com.pousheng.middle.item;

import com.google.common.base.Objects;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.enums.PsSpuType;
import io.terminus.parana.spu.model.SkuTemplate;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Created by songrenfei on 2018/1/16
 */
public class PsItemTool {

    //是否为mPos商品
    public static Boolean isMopsItem(SkuTemplate exist){
        if(Objects.equal(exist.getType(), PsSpuType.MPOS.value())){
            return Boolean.TRUE;
        }

        return Boolean.FALSE;

    }
}
