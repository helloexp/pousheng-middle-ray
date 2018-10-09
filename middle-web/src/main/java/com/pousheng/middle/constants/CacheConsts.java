package com.pousheng.middle.constants;

/**
 * 缓存相关常量
 *
 * @author tanlongjun
 */
public class CacheConsts {

    /**
     * 默认锁超时时间
     */
    public static final String DEFUALT_LOCK_TTL="5";

    /**
     * 锁超时时间 长一些
     */
    public static final String LONG_LOCK_TTL="60";

    /**
     * key不存在
     */
    public static final String NIL = "nil";


    public interface JITCacheKeys{
        /**
         * JIT前缀
         */
        String PREFIX="JIT:API:";

        /**
         * 同步订单
         */
        String ORDER_SYNC_LOCK_KEY_PATTERN=PREFIX+"SYNC:ORDER:{0}";
    }

    /**
     * 店铺超过最大接单量标志位
     * # 格式 currentDate
     * SHOP:MAX:ORDER:LIMIT:{shopWarehouseId}:{currentDate}
     *
     * # 示例
     * SHOP:MAX:ORDER:LIMIT:1818:20181010
     */
    public static final String SHOP_MAX_ORDER_LIMIT_PATTERN="SHOP:MAX:ORDER:LIMIT:{0}:{1}";

    /**
     * 过期秒数
     */
    public interface ExpireSecond {

        int ONE_MINUTE = 60;

        int ONE_HOUR = 60 * ONE_MINUTE;

        int ONE_DAY = 24 * ONE_HOUR;
    }

}
