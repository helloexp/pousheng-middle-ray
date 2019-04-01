package com.pousheng.middle.web.shop.component;

import com.pousheng.middle.shop.dto.ShopBusinessTime;
import com.pousheng.middle.shop.enums.ShopOpeningStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Description: 门店营业相关逻辑
 * User: liangyj
 * Date: 2018/5/11
 */
@Slf4j
@Component
public class ShopBusinessLogic {

    /**
     * @Description 校验请求门店经营时间
     * @Date 2018/5/9
     * @param status 营业状态 1：营业 2：歇业
     * @param startTime 营业开始时间
     * @param endTime 营业结束时间
     * @param weekStr 周几的描述
     * @return java.lang.String
     */
    public boolean valideBusinessTime(Integer status,String startTime,String endTime) {
        boolean validResult = true;

        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

        if (status != null && status.equals(ShopOpeningStatus.OPENING.value())) {
            try {
                if (!validTime(startTime) || !validTime(endTime)) {
                    validResult = false;
                }
                if ( timeFormatter.parse(startTime).compareTo(timeFormatter.parse(endTime)) > 0 )  {
                    validResult = false;
                }
            } catch (Exception e) {
                validResult = false;
            }
        }
        return validResult;
    }

    /**
     * @Description 获取门店订单到期时间
     * @Date 2018/5/11
     * @param shopBusinessTimeMap
     * @param orderTime
     * @param tempDate
     * @param timeoutSurplus
     * @return java.time.LocalDateTime
     */
    public LocalDateTime getExcpireDateTime(Map<DayOfWeek, String[]> shopBusinessTimeMap,
                                            LocalTime orderTime,
                                            LocalDate tempDate,
                                            long timeoutSurplus) throws Exception {

        try {
            DayOfWeek dayOfWeek = tempDate.getDayOfWeek();

            //判断当天是否可以满足
            String[] openingArr = shopBusinessTimeMap.get(((DayOfWeek) dayOfWeek));
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            Integer openStatus = Integer.parseInt(openingArr[0]);
            LocalTime startTime = LocalTime.parse(openingArr[1]);
            LocalTime endTime = LocalTime.parse(openingArr[2]);

            //如果当天不营业，顺延一天回调
            if (openStatus == null || !openStatus.equals(ShopOpeningStatus.OPENING.value())) {
                //订单时间设置为第二天的凌晨
                orderTime = LocalTime.MIN;
                return getExcpireDateTime(shopBusinessTimeMap,orderTime,tempDate.plusDays(1),timeoutSurplus);
            }

            //如果接单时间晚于开始营业时间，将开始时间设置为接单时间
            if (orderTime.compareTo(startTime) > 0) {
                startTime = orderTime;
            }

            //当日剩余营业时间是否满足过期设置的时间值，如果满足返回到期时间，如果不满足顺延一天回调
            Duration between = Duration.between(startTime, endTime);
            long betweenMinutes = between.toMinutes();

            if (betweenMinutes >= timeoutSurplus) {
                return LocalDateTime.of(tempDate,startTime.plusMinutes(timeoutSurplus));
            }

            if ((startTime.plusMinutes(timeoutSurplus)).compareTo(endTime) > 0) {
                timeoutSurplus = timeoutSurplus - betweenMinutes;
                return getExcpireDateTime(shopBusinessTimeMap,orderTime,tempDate.plusDays(1),timeoutSurplus);
            }

        } catch (Exception e) {
            throw new Exception("shop.order.expire.time.fail");
        }
        return null;
    }


    /**
     * @Description 将shop扩展字段中的shopBusiness 周一至周日的营业时间转为map
     * @Date 2018/5/11
     * @param shopBusinessTime
     * @return java.util.Map<java.time.DayOfWeek,java.lang.String[]>
     */
    public Map<DayOfWeek,String[]> getWeekMap(ShopBusinessTime shopBusinessTime) {

        Map<DayOfWeek,String[]> weekMmap = new HashMap<DayOfWeek,String[]>();
        String[] openingArr = null;

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusMon() == null ? "" : shopBusinessTime.getOpeningStatusMon().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeMon();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeMon();
        weekMmap.put(DayOfWeek.MONDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusTue() == null ? "" : shopBusinessTime.getOpeningStatusTue().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeTue();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeTue();
        weekMmap.put(DayOfWeek.TUESDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusWed() == null ? "" : shopBusinessTime.getOpeningStatusWed().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeWed();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeWed();
        weekMmap.put(DayOfWeek.WEDNESDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusThu() == null ? "" : shopBusinessTime.getOpeningStatusThu().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeThu();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeThu();
        weekMmap.put(DayOfWeek.THURSDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusFri() == null ? "" : shopBusinessTime.getOpeningStatusFri().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeFri();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeFri();
        weekMmap.put(DayOfWeek.FRIDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusSat() == null ? "" : shopBusinessTime.getOpeningStatusSat().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeSat();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeSat();
        weekMmap.put(DayOfWeek.SATURDAY,openingArr);

        openingArr = new String[3];
        openingArr[0] = shopBusinessTime.getOpeningStatusSun() == null ? "" : shopBusinessTime.getOpeningStatusSun().toString();
        openingArr[1] = shopBusinessTime.getOpeningStartTimeSun();
        openingArr[2] = shopBusinessTime.getOpeningEndTimeSun();
        weekMmap.put(DayOfWeek.SUNDAY,openingArr);

        return weekMmap;
    }

    /**
     * @Description 校验门店营业状态和营业时间是否可以处理订单
     * @Date 2018/5/11
     * @param weekTimeMap 门店营业时间map
     * @return boolean
     */
    public boolean validShopOrderCapacity(Map<DayOfWeek,String[]> weekTimeMap) {
        boolean result = false;
        for (Map.Entry<DayOfWeek, String[]> entry : weekTimeMap.entrySet()) {

            String[] timeArr = entry.getValue();

            if (timeArr[0] != null
                    && timeArr[0].equals(String.valueOf(ShopOpeningStatus.OPENING.value()))
                    && validTime(timeArr[1])
                    && validTime(timeArr[2])
                    && LocalTime.parse(timeArr[1]).compareTo(LocalTime.parse(timeArr[2])) < 0) {
                result = true;
                return result;
            }

        }

        return result;

    }

    /**
     * @Description 校验时间HH:mm 是否有效
     * @Date   2018/5/11
     * @param  timeStr 时间，格式要求：HH:mm
     * @return boolean
     */
    public boolean validTime(String timeStr) {
        try {
            if (timeStr == null || "".equals(timeStr)) {
                return false;
            }

            String[] timeArr = timeStr.split(":");
            if (timeArr.length != 2 && timeArr.length != 3)  {
                return false;
            }

            int hourInt = Integer.parseInt(timeArr[0]);
            int minuteInt = Integer.parseInt(timeArr[1]);
            if (hourInt < 0 || hourInt > 23) {
                return false;
            }

            if (minuteInt < 0 || minuteInt > 59) {
                return false;
            }
        } catch (Exception e) {
            return  false;
        }

        return  true;
    }

}
