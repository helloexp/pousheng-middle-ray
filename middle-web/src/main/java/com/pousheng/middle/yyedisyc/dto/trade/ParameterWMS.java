/**
 * 
 */
package com.pousheng.middle.yyedisyc.dto.trade;

/**
 * @author RAY
 *
 */
public class ParameterWMS {

	/**
	 * @author RAY POUS934 電商銷售單接口增加billsource參數
	 */
	public enum BillSource {

		衡康("0"), 中台("1"), 調帳單("2"), 寶唯雲聚("3"), 寶原雲聚("4");

		String code;

		private BillSource(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		@Override
		public String toString() {
			return "code: " + code;
		}

	}

}
