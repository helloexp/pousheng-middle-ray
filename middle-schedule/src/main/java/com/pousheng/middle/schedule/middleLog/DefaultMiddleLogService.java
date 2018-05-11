package com.pousheng.middle.schedule.middleLog;

import io.terminus.applog.core.criteria.MemberAppLogDtoCriteria;
import io.terminus.applog.core.dao.MemberApplicationLogDao;
import io.terminus.applog.core.model.BaseOperator;
import com.pousheng.auth.dao.UserDao;
import com.pousheng.auth.model.MiddleUser;
import io.terminus.applog.core.model.MemberAppLogKey;
import io.terminus.applog.core.model.MemberAppLogOutput;
import io.terminus.applog.core.model.MemberApplicationLog;
import io.terminus.applog.rest.service.ApplogService;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/21
 * Time: 下午7:47
 */
@Service
public class DefaultMiddleLogService implements ApplogService {

    @Autowired
    private MemberApplicationLogDao memberApplicationLogDao;

    @Autowired
    private UserDao userDao;

    @Override
    public BaseOperator getBaseOperator() {
        ParanaUser user = UserUtil.getCurrentUser();

        if (user == null) {
            return BaseOperator.create().id(0L).name("null");
        }

        MiddleUser middleUser = userDao.findByName(user.getName());

        if (middleUser == null) {
            return BaseOperator.create().id(0L).name("null");
        }

        return BaseOperator.create().id(middleUser.getOutId()).name(user.getName());
    }

    @Override
    public String getOperatorIdByName(String s) {
        if (Arguments.isNull(s)) {
            return "";
        }
        MemberAppLogDtoCriteria criteria = new MemberAppLogDtoCriteria();
        criteria.setPageNo(1);criteria.setPageSize(100000);
        List<MemberApplicationLog> list = memberApplicationLogDao.paging(criteria.toMap()).getData();
        for (MemberApplicationLog memberApplicationLog : list) {
            String[] metadata = memberApplicationLog.getMetadata().split(",");
            if (metadata[0].contains(s)) {
                return memberApplicationLog.getOperatorId();
            }

        }
        return "-1";
    }

    @Override
    public void setExtraInfo(MemberAppLogOutput output) {

    }
    @Override
    public void returnKeyAndLog(MemberAppLogKey key, MemberApplicationLog log) {

    }
}
