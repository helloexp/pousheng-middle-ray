package com.pousheng.middle.open.api;

import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;

/**
 * Created by songrenfei on 2017/7/10
 */
@OpenBean
@Slf4j
public class DemoOpenApi {


    @OpenMethod(key = "hk.hello.world.api", paramNames = {"name","domain"}, httpMethods = RequestMethod.GET)
    public String helloWord(@NotEmpty(message = "name.empty") String name,String domain) {
        log.info("HK-HELLER-WORLD-START param name is:{} ", name);

        log.info("HK-HELLER-WORLD-END");
        return "hello world:" + name;
    }



    @OpenMethod(key = "hk.create.user.api", paramNames = {"name","age"}, httpMethods = RequestMethod.POST)
    public String createUser(@NotEmpty(message = "name.empty") String name,
                            @NotNull(message = "name.empty") Integer age) {
        log.info("CREATE-USER-START param name is:{} age:{} ", name,age);

        log.info("CREATE-USER-END");
        return name;
    }

    @OpenMethod(key = "create.middle.user.api", paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public String createMiddleUser(User user) {
        log.info("CREATE-USER-START param user is:{}", user);

        log.info("CREATE-USER-END");
        return user.getName();
    }




}
