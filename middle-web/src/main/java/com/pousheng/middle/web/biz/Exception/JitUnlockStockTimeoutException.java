package com.pousheng.middle.web.biz.Exception;

import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;

import java.util.List;

/**
 * 处理异常
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/29
 * pousheng-middle
 */
public class JitUnlockStockTimeoutException extends RuntimeException {
    public JitUnlockStockTimeoutException() {
    }

    public JitUnlockStockTimeoutException(String message) {
        super(message);
    }

    public JitUnlockStockTimeoutException(Throwable cause) {
        super(cause);
    }

    public JitUnlockStockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    private List<InventoryTradeDTO> data;

    public List<InventoryTradeDTO> getData() {
        return data;
    }

    public void setData(List<InventoryTradeDTO> data) {
        this.data = data;
    }
}
