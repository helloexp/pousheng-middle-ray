package com.pousheng.middle.web.warehouses.dto;

import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-20 11:56<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSendImprtDTO implements Serializable {
    private static final long serialVersionUID = 8014955530170522837L;

    private String companyCode;
    private String outCode;
    private Long warehouseId;

    private boolean hasError;
    private String errorMsg;
}
