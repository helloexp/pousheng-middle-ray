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
}
