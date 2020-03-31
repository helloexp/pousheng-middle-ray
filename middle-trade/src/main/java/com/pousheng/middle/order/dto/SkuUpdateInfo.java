/**
 * 
 */
package com.pousheng.middle.order.dto;

import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author RAY 2019.04.24: POUS-923 批量修改待處理的商品條碼 更新資料庫BEAN
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkuUpdateInfo implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6563519310044830480L;

	/**
	 * 訂單來源
	 */
	@CsvBindByPosition(position = 0)
	private String orderSouce;

	/**
	 * 訂單號，對應parana_sku_orders的out_id
	 */
	@CsvBindByPosition(position = 1)
	private String outId;

	/**
	 * 原貨品條碼，對應parana_sku_orders的out_sku_id
	 */
	@CsvBindByPosition(position = 2)
	private String oldSkuId;

	/**
	 * 修正後貨品條碼，對應parana_sku_orders的sku_code
	 */
	@CsvBindByPosition(position = 3)
	private String newSkuId;

	/**
	 * 2019.06.20 RAY orderSouce+outId+oldSkuId 如果一樣，不新增
	 * 
	 * @return
	 */
	public String getDistinctKey() {
		return orderSouce + outId + oldSkuId;
	}
}
