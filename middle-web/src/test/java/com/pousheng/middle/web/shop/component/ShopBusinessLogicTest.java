package com.pousheng.middle.web.shop.component;

import com.pousheng.middle.shop.dto.ShopBusinessTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(SpringRunner.class)

public class ShopBusinessLogicTest {

    ShopBusinessLogic shopBusinessLogic;
    Map<DayOfWeek,String[]> weekMap;

    @Before
    public void init() throws Exception {
        shopBusinessLogic = new ShopBusinessLogic();
        weekMap = new HashMap<DayOfWeek,String[]>();
        weekMap.put(DayOfWeek.MONDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.TUESDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.WEDNESDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.THURSDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.FRIDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.SATURDAY,new String[]{"1","10:00","20:00"});
        weekMap.put(DayOfWeek.SUNDAY,new String[]{"1","10:00","20:00"});
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void valideBusinessTime() {
        Integer status = 1;
        String startTime = "9:30";
        String endTime = "15:30";
        Assert.assertTrue(shopBusinessLogic.valideBusinessTime(status,startTime,endTime));
    }

    @Test
    public void getExcpireDateTime() {
        Map shopBusinessTimeMap = this.weekMap;
        LocalTime orderTime = LocalTime.of(10,50);
        LocalDate orderDate = LocalDate.of(2018,5,15);
        long timeoutSurplus = 120;
        try {
            LocalDateTime resultDate = shopBusinessLogic.getExcpireDateTime(shopBusinessTimeMap,orderTime, orderDate, timeoutSurplus);

            Assert.assertEquals(resultDate,LocalDateTime.of(LocalDate.of(2018,5,15),LocalTime.of(12,50)));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void getWeekMap() {
        ShopBusinessTime shopBusinessTime = new ShopBusinessTime();
        shopBusinessTime.setOpeningStatus(1);
        shopBusinessTime.setOpeningStatusMon(1);
        shopBusinessTime.setOpeningStartTimeMon("10:00");
        shopBusinessTime.setOpeningEndTimeMon("16:00");
        Map<DayOfWeek,String[]> resultMap = shopBusinessLogic.getWeekMap(shopBusinessTime);
        System.out.println("test:"+resultMap.toString());
        String[] strArr = {shopBusinessTime.getOpeningStatusMon().toString(),shopBusinessTime.getOpeningStartTimeMon(),shopBusinessTime.getOpeningEndTimeMon()};
        Assert.assertThat(resultMap.get(DayOfWeek.MONDAY),
                equalTo(strArr));
    }

    @Test
    public void validShopOrderCapacity() {

        Assert.assertTrue(shopBusinessLogic.validShopOrderCapacity(weekMap));
    }

    @Test
    public void validTime() {
        //验证为false
        List<String> list = new ArrayList<String>();
        list.add("10:60");
        list.add("24:00");
        list.add("25:00");
        list.add("25:62");
        list.add("1");
        list.add("100");
        list.add("test");

        for(String timeStr : list) {
            Assert.assertFalse(shopBusinessLogic.validTime(timeStr));
        }
        //验证为true
        list.clear();
        list.add("1:00");
        list.add("02:03");
        list.add("2:13");
        list.add("23:13");
        list.add("00:13");
        list.add("00:13:10");
        for(String timeStr : list) {
            Assert.assertTrue(shopBusinessLogic.validTime(timeStr));
        }
    }
}