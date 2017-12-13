package com.pousheng.middle.web.item.batchhandle;

import com.google.common.base.Throwables;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BatchHandleMposLogic {

    @Autowired
    private JedisTemplate jedisTemplate;

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
                        record.setState(value);
                        record.setType(type);
                        record.setId(Long.parseLong(attr[1]));
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
        return null;
    }

    /**
     * 查看导入文件异常记录
     * @param taskId
     * @return
     */
    public Response<Paging<AbnormalRecord>> getImportFileAbnormalRecord(String taskId,Integer pageNo,Integer pageSize){
//        Long userId = UserUtil.getUserId();
//        int limit = new PageInfo(pageNo,pageSize).getOffset();
//        String key = "mpos:" + userId + ":abnormal:flag:" + taskId;
//        try{
//            List<AbnormalRecord> list = jedisTemplate.execute(new JedisTemplate.JedisAction<List<AbnormalRecord>>() {
//                @Override
//                public List<AbnormalRecord> action(Jedis jedis) {
//                    List<AbnormalRecord> records = new ArrayList<>();
//                    jedis.lrange(key,limit,limit + pageSize).forEach(key -> {
//                        AbnormalRecord record = new AbnormalRecord();
//                        String value = jedis.get(key);
//                        String[] vals = value.split(":");
//                        record.setId(vals[0]);
//                        record.setReason(vals[1]);
//                        records.add(record);
//                    });
//                    return records;
//                }
//            });
//            long total = jedisTemplate.execute(new JedisTemplate.JedisAction<Long>() {
//                @Override
//                public Long action(Jedis jedis) {
//                    return jedis.llen(key);
//                }
//            });
//            return Response.ok(new Paging<>(total,list));
//        }catch (Exception e){
//            log.error("fail to get mpos flag record,cause:{}", Throwables.getStackTraceAsString(e));
//            return Response.fail("fail to get mpos flag record");
//        }
        return null;
    }

    /**
     * 查看打标，取消打标异常记录
     * @param taskId
     * @return
     */
    public Response<Paging<AbnormalRecord>> getMposFlagAbnormalRecord(String taskId,Integer pageNo,Integer pageSize,String type){
        Long userId = UserUtil.getUserId();
        int limit = new PageInfo(pageNo,pageSize).getOffset();
        String key = "mpos:" + userId + ":abnormal:" + type + ":" + taskId;
        try{
            long total = jedisTemplate.execute(new JedisTemplate.JedisAction<Long>() {
                @Override
                public Long action(Jedis jedis) {
                    return jedis.llen(key);
                }
            });
            List<AbnormalRecord> list = jedisTemplate.execute(new JedisTemplate.JedisAction<List<AbnormalRecord>>() {
                @Override
                public List<AbnormalRecord> action(Jedis jedis) {
                    List<AbnormalRecord> records = new ArrayList<>();
                    jedis.lrange(key,limit,limit + pageSize).forEach(value -> {
                        AbnormalRecord record = new AbnormalRecord();
                        String[] vals = value.split("~");
                        record.setCode(vals[0]);
                        record.setReason(vals[1]);
                        records.add(record);
                    });
                    return records;
                }
            });
            return Response.ok(new Paging<>(total,list));
        }catch (Exception e){
            log.error("fail to get mpos flag record,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("fail to get mpos flag record");
        }
    }

    /**
     * 导入excel
     * @param fileName
     * @param file
     */
    public void batchImport(String fileName,File file){

    }
}
