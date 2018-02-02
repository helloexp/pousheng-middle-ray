package com.pousheng.middle.web.item.batchhandle;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BatchHandleMposLogic {

    @Autowired
    private JedisTemplate jedisTemplate;
    @Autowired
    private PsSkuTemplateWriteService psSkuTemplateWriteService;
    @Autowired
    private PushMposItemComponent pushMposItemComponent;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;

    /**
     * 获取批量打标，取消打标记录
     * @return
     */
    public Response<List<BatchHandleRecord>> getMposFlagRecord(){
        Long userId = UserUtil.getUserId();
        try{
            List<BatchHandleRecord> list = jedisTemplate.execute(new JedisTemplate.JedisAction<List<BatchHandleRecord>>() {
                @Override
                public List<BatchHandleRecord> action(Jedis jedis) {
                    List<BatchHandleRecord> records = new ArrayList<>();
                    jedis.keys("mpos:" + userId + ":flag:*").forEach(key -> {
                        BatchHandleRecord record = new BatchHandleRecord();
                        String[] attr = key.split("~");
                        record.setCreateAt(new Date(Long.parseLong(attr[1])));
                        String value = jedis.get(key);
                        record.setName(attr[1]);
                        String type = attr[0].substring(("mpos:" + userId + ":flag").length());
                        record.setType(type);
                        String[] val = value.split("~");
                        if(val.length == 1) {
                            record.setState(value);
                        }else{
                            if(Objects.equals(val[0],PsItemConstants.SYSTEM_ERROR)){
                                record.setState(val[0]);
                                record.setMessage(val[1]);
                            }else {
                                record.setState(val[0]);
                                record.setUrl(val[1]);
                            }
                        }
                        records.add(record);
                    });
                    return records.stream().sorted((r1, r2) -> r2.getCreateAt().compareTo(r1.getCreateAt())).collect(Collectors.toList());
                }
            });
            return Response.ok(list);
        }catch (Exception e){
            log.error("fail to get mpos flag record,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("fail to get mpos flag record");
        }
    }

    /**
     * 获取批量文件导入记录
     * @return
     */
    public Response<List<BatchHandleRecord>> getImportFileRecord(){
        Long userId = UserUtil.getUserId();
        try{
            List<BatchHandleRecord> list = jedisTemplate.execute(new JedisTemplate.JedisAction<List<BatchHandleRecord>>() {
                @Override
                public List<BatchHandleRecord> action(Jedis jedis) {
                    List<BatchHandleRecord> records = new ArrayList<>();
                    jedis.keys("mpos:" + userId + ":import:*").forEach(key -> {
                        BatchHandleRecord record = new BatchHandleRecord();
                        String[] attr = key.split("~");
                        record.setCreateAt(new Date(Long.parseLong(attr[1])));
                        String value = jedis.get(key);
                        record.setName(attr[0].substring(("mpos:" + userId + ":import:").length()));
                        String[] val = value.split("~");
                        if(val.length == 1) {
                            record.setState(value);
                        }else{
                            record.setState(val[0]);
                            record.setUrl(val[1]);
                        }
                        records.add(record);
                    });
                    return records.stream().sorted((r1, r2) -> r2.getCreateAt().compareTo(r1.getCreateAt())).collect(Collectors.toList());
                }
            });
            return Response.ok(list);
        }catch (Exception e){
            log.error("fail to get mpos import record,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("fail to get mpos import record");
        }
    }

    /**
     * 获取导出文件记录
     * @return
     */
    public Response<List<BatchHandleRecord>> getExportFileRecord(){
        Long userId = UserUtil.getUserId();
        try{
            List<BatchHandleRecord> list = jedisTemplate.execute(new JedisTemplate.JedisAction<List<BatchHandleRecord>>() {
                @Override
                public List<BatchHandleRecord> action(Jedis jedis) {
                    List<BatchHandleRecord> records = new ArrayList<>();
                    jedis.keys("mpos:" + userId + ":export:*").forEach(key -> {
                        BatchHandleRecord record = new BatchHandleRecord();
                        String[] attr = key.split("~");
                        record.setCreateAt(new Date(Long.parseLong(attr[1])));
                        String value = jedis.get(key);
                        record.setName(attr[1]);
                        String name = attr[0].substring(("mpos:" + userId + ":export:").length());
                        record.setName(name);
                        String[] val = value.split("~");
                        if(val.length == 1) {
                            record.setState(value);
                        }else {
                            record.setState(val[0]);
                            record.setUrl(val[1]);
                        }
                        records.add(record);
                    });
                    return records.stream().sorted((r1, r2) -> r2.getCreateAt().compareTo(r1.getCreateAt())).collect(Collectors.toList());
                }
            });
            return Response.ok(list);
        }catch (Exception e){
            log.error("fail to get mpos export record,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("fail to get mpos export record");
        }
    }


    /**
     * 打标并设置默认折扣
     * @param id        商品
     * @param type      打标/取消打标
     */
    public void makeFlagAndSetDiscount(Long id,Integer type){
        log.info("sku(id:{}) make flag start..",id);
        Integer discount = 100;
        Response<SkuTemplate> rExist = skuTemplateReadService.findById(id);
        if(!rExist.isSuccess()){
            log.error("sku(id:{}) make flag failed,cause:{}",id,rExist.getError());
            return ;
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setType(type);
        //如果本来不包含折扣，默认设置折扣
        if(Objects.equals(type,PsSpuType.MPOS.value()) && !extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
            extra.put(PsItemConstants.MPOS_DISCOUNT,discount.toString());
            toUpdate.setExtra(extra);
            Integer originPrice = 0;
            if (exist.getExtraPrice() != null&&exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
                originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
            }
            if(!Objects.isNull(originPrice)){
                toUpdate.setPrice(originPrice);
            }
        }
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
        }
        log.info("sku(id:{}) make flag over..",exist.getId());
    }

    /**
     * 设置折扣
     * @param id        商品id
     * @param discount  折扣
     */
    public void setDiscount(Long id,Integer discount){
        Response<SkuTemplate> rExist = skuTemplateReadService.findById(id);
        if(!rExist.isSuccess()){
            log.error("find sku template by id:{} failed,cause:{}",id,rExist.getError());
            return ;
        }
        SkuTemplate exist = rExist.getResult();
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        Integer originPrice = 0;
        if (exist.getExtraPrice() != null&&exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
            originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
        }
        if(!Objects.isNull(originPrice)){
            Map<String,String> extra = exist.getExtra();
            if(CollectionUtils.isEmpty(extra)){
                extra = Maps.newHashMap();
            }
            extra.put(PsItemConstants.MPOS_DISCOUNT,discount.toString());
            toUpdate.setExtra(extra);
            toUpdate.setPrice(calculatePrice(discount,originPrice));
        }else{
            return ;
        }
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(PsItemConstants.ERROR_UPDATE_FAIL);
        }
        //同步电商
        pushMposItemComponent.updatePrice(Lists.newArrayList(exist),toUpdate.getPrice());

    }



    private static Integer calculatePrice(Integer discount, Integer originPrice){
        BigDecimal ratio = new BigDecimal("100");  // 百分比的倍率
        BigDecimal discountDecimal = new BigDecimal(discount);
        BigDecimal percentDecimal =  discountDecimal.divide(ratio,2, BigDecimal.ROUND_HALF_UP);
        return percentDecimal.multiply(BigDecimal.valueOf(originPrice)).intValue();
    }

}
