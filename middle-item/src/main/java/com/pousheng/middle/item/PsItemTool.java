package com.pousheng.middle.item;

import com.google.common.base.Objects;
import com.pousheng.middle.item.constant.PsItemConstants;
import io.terminus.parana.spu.model.SkuTemplate;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Created by songrenfei on 2018/1/16
 */
public class PsItemTool {

    //是否为mPos商品
    public static Boolean isMopsItem(SkuTemplate exist){
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            return Boolean.FALSE;
        }
        if(!extra.containsKey(PsItemConstants.MPOS_FLAG)){
            return Boolean.FALSE;
        }
        String flag = extra.get(PsItemConstants.MPOS_FLAG);
        return Objects.equal(flag,PsItemConstants.MPOS_ITEM);

    }
}
