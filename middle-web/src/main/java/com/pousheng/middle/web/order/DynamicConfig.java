package com.pousheng.middle.web.order;

import com.pousheng.middle.web.redis.ServerSwitchOnOperationLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.redis.utils.JedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Created by penghui on 2018/6/12
 */
@Api(description = "针对redis处理异步任务的动态配置")
@RestController
@Slf4j
@RequestMapping("/dynamic/config")
public class DynamicConfig {

    @Autowired
    private JedisTemplate jedisTemplate;
    @Autowired
    private ServerSwitchOnOperationLogic serverSwitchOnOperationLogic;

    @ApiOperation("设置每组push数量")
    @RequestMapping(value = "/group/push/size", method = RequestMethod.PUT)
    public void setGroupSize(@RequestParam String key, @RequestParam Integer size) {
        jedisTemplate.execute(jedis -> {
            jedis.set(key, size.toString());
        });
    }

    @ApiOperation("查询每组push数量")
    @RequestMapping(value = "/group/push/size", method = RequestMethod.GET)
    public Integer getGroupSize(String key) {
        String size = jedisTemplate.execute(jedis -> {
            return jedis.get(key);
        });
        if (StringUtils.isEmpty(size)) {
            return null;
        }
        return Integer.valueOf(size);
    }

    @ApiOperation("查询生产者/消费者开关")
    @RequestMapping(value = "/group/push/switch", method = RequestMethod.GET)
    public Boolean getSwitch(String key) {
        String swit = jedisTemplate.execute(jedis -> {
            return jedis.get(key);
        });
        return !Objects.equals(swit, "off");
    }

    @ApiOperation("设置生产者/消费者开关")
    @RequestMapping(value = "/group/push/switch", method = RequestMethod.PUT)
    public void setSwitch(String key, Boolean bol) {
        String swit = bol ? "on" : "off";
        jedisTemplate.execute(jedis -> {
            jedis.set(key, swit);
        });
    }


    @ApiOperation("开启中台服务")
    @RequestMapping(value = "/server/open", method = RequestMethod.PUT)
    public String openServer() {
        serverSwitchOnOperationLogic.openServer();
        return "success";
    }

    @ApiOperation("关闭中台服务")
    @RequestMapping(value = "/server/close", method = RequestMethod.PUT)
    public String closeServer() {
        serverSwitchOnOperationLogic.closeServer();
        return "success";
    }


    @ApiOperation("查询中台服务")
    @RequestMapping(value = "/server/get", method = RequestMethod.GET)
    public Boolean getServer() {
        return serverSwitchOnOperationLogic.serverIsOpen();
    }


}
