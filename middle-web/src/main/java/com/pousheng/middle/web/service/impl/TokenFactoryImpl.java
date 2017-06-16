package com.pousheng.middle.web.service.impl;

import com.google.common.base.Optional;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.token.Token;
import io.terminus.open.client.common.token.TokenFactory;
import io.terminus.open.client.common.token.model.OpenToken;
import io.terminus.open.client.token.impl.service.OpenTokenReadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by songrenfei on 2017/6/6
 */
@Slf4j
@Component
public class TokenFactoryImpl implements TokenFactory {

    @RpcConsumer
    private OpenTokenReadServiceImpl openTokenReadService;

    @Override
    public Token get(Long shopId, OpenClientChannel channel) {

        Response<Optional<OpenToken>> response = openTokenReadService.findByShopIdAndChannel(shopId, channel);
        if(!response.isSuccess()){
            log.error("find token by shop id:{} and channel:{} fail,error:{}",shopId,channel.toString(),response.getError());
            throw new OpenClientException(500,"open.token.find.fail");
        }
        Optional<OpenToken> openTokenOptional = response.getResult();


        if(!openTokenOptional.isPresent()){
            log.error("not find open token by shop id:{} channel:{}",shopId,channel);
            throw new OpenClientException(500,"open.token.not.exist");
        }

        OpenToken openToken = openTokenOptional.get();

        return new Token(openToken.getAppKey(),openToken.getSecret(),openToken.getGateway(),null);
    }

}
