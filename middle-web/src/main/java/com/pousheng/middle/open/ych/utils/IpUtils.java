package com.pousheng.middle.open.ych.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by cp on 10/7/17.
 */
@Slf4j
public abstract class IpUtils {

    public static boolean isPrivateIPAddress(String ipAddress) {
        InetAddress ia;
        try {
            InetAddress ad = InetAddress.getByName(ipAddress);
            byte[] ip = ad.getAddress();
            ia = InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            log.error("invalid ip address:{}", ipAddress, e);
            return false;
        }
        return ia.isLoopbackAddress() || ia.isSiteLocalAddress();
    }

}
