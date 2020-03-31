//package com.pousheng.middle.web.warehouses.component;
//
//import com.alibaba.fastjson.JSON;
//import com.pousheng.middle.web.MiddleConfiguration;
//import com.pousheng.middle.web.warehouses.dto.SendWarehouseDTO;
//import org.jetbrains.annotations.NotNull;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.Assert.*;
//import static org.hamcrest.Matchers.*;
//
///**
// * AUTHOR: zhangbin
// * ON: 2019/8/13
// */
//@RunWith(SpringRunner.class)
//public class MiddleWarehouseServiceTest {
//
//    private MiddleWarehouseService middleWarehouseService;
//
//    @Before
//    public void init() {
//        middleWarehouseService = new MiddleWarehouseService();
//    }
//
//    @Test
//    public void sortSendWarehouse() {
//        List<SendWarehouseDTO> dtoList = new ArrayList<>();
//
//        SendWarehouseDTO dto1 = getSendWarehouseDTO("300", 0, 100);
//        SendWarehouseDTO dto2 = getSendWarehouseDTO("200", 1, 100);
//        SendWarehouseDTO dto3 = getSendWarehouseDTO("100", 0, 2);
//        SendWarehouseDTO dto4 = getSendWarehouseDTO("200", 0, 100);
//        SendWarehouseDTO dto5 = getSendWarehouseDTO("325", 1, 100);
//        SendWarehouseDTO dto6 = getSendWarehouseDTO("325", 0, 1);
//        SendWarehouseDTO dto7 = getSendWarehouseDTO("100", 1, 120);
//        SendWarehouseDTO dto8 = getSendWarehouseDTO("325", 1, 120);
//        SendWarehouseDTO dto9 = getSendWarehouseDTO("325", 1, 10);
//        dtoList.add(dto1);
//        dtoList.add(dto2);
//        dtoList.add(dto3);
//        dtoList.add(dto4);
//        dtoList.add(dto5);
//        dtoList.add(dto6);
//        dtoList.add(dto7);
//        dtoList.add(dto8);
//        dtoList.add(dto9);
//        List<SendWarehouseDTO> sendWarehouseDTOS = middleWarehouseService.sortSendWarehouse(dtoList);
//        System.out.println(JSON.toJSONString(sendWarehouseDTOS));
//    }
//
//    @NotNull
//    private SendWarehouseDTO getSendWarehouseDTO(String companyCode, Integer type, Integer quantity) {
//        SendWarehouseDTO dto1 = new SendWarehouseDTO();
//        dto1.setCompanyCode(companyCode);
//        dto1.setWarehouseSubType(type);
//        dto1.setQuantity(quantity);
//        return dto1;
//    }
//}