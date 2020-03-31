package com.pousheng.middle.yyedisyc.dto.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 发货单同步到云聚ERP
 * @author: yjc
 * @date: 2018/7/31下午3:59
 */
public class YJErpShipmentInfo implements Serializable {
    private static final long serialVersionUID = -4313716161216754939L;

    /**
     * 外部订单号-> 中台发货单号
     */
    @JsonProperty(value = "other_order_sn")
    private String other_order_sn;

    /**
     * 收货人
     */
    @JsonProperty(value = "consignee")
    private String consignee;

    /**
     * 省份(编码)
     */
    @JsonProperty(value = "province")
    private String province;

    /**
     * 城市(编码)
     */
    @JsonProperty(value = "city")
    private String city;

    /**
     * 区域(编码)
     */
    @JsonProperty(value = "area")
    private String area;

    /**
     * 0:验证省市区编码 1:验证省市区名称
     */
    @JsonProperty(value = "check_area")
    private Integer check_area;

    /**
     * 省
     */
    @JsonProperty(value = "province_name")
    private String province_name;

    /**
     * 市
     */
    @JsonProperty(value = "city_name")
    private String city_name;

    /**
     * 区
     */
    @JsonProperty(value = "area_name")
    private String area_name;

    /**
     * 联系地址
     */
    @JsonProperty(value = "address")
    private String address;

    /**
     * 邮编
     */
    @JsonProperty(value = "zipcode")
    private String zipcode;

    /**
     * 联系电话
     */
    @JsonProperty(value = "telephone")
    private String telephone;

    /**
     * 手机号码
     */
    @JsonProperty(value = "mobile")
    private String mobile;

    /**
     * 备注信息
     */
    @JsonProperty(value = "message")
    private String message;

    /**
     * 下单店铺名称
     */
    @JsonProperty(value = "order_from")
    private String order_from;

    /**
     * 下单店铺code
     */
    @JsonProperty(value = "shop_code")
    private String shop_code;

    /**
     * 下单店铺账套
     */
    @JsonProperty(value = "shop_companycpde")
    private String shop_companycpde;

    /**
     * 下单店铺渠道
     */
    @JsonProperty(value = "shop_channels")
    private String shop_channels;


    /**
     * 有货商品可以先发货:1，等待所有商品到货一起发货:2
     * 默认 1
     */
    @JsonProperty(value = "delivery_option")
    private Integer delivery_option;

    /**
     * 寄件人
     */
    @JsonProperty(value = "sender")
    private String sender;

    /**
     * 寄件人电话
     */
    @JsonProperty(value = "sender_mobile")
    private String sender_mobile;

    /**
     * 寄件人地址
     */
    @JsonProperty(value = "sender_address")
    private String sender_address;

    /**
     * 保价金额
     */
    @JsonProperty(value = "insured_price")
    private Float insured_price;

    /**
     * 代收金额
     */
    @JsonProperty(value = "collection_amount")
    private Float collection_amount;

    /**
     * 快递公司id
     */
    @JsonProperty(value = "logistics_company_id")
    private Integer logistics_company_id;

    /**
     * 快递单号
     */
    @JsonProperty(value = "logistics_order")
    private String logistics_order;

    /**
     * 配送方式 普通快递、EMS、顺丰
     */
    @JsonProperty(value = "delivery_type_id")
    private Integer delivery_type_id;

    /**
     * 是否开发票 1:是 0:否 默认0
     */
    @JsonProperty(value = "is_invoice")
    private Integer is_invoice;

    /**
     * 发票类型 参数（不开发票 0，公司 1，个人 2） 默认 0
     */
    @JsonProperty(value = "invoice_type")
    private Integer invoice_type;

    /**
     * 发票抬头
     */
    @JsonProperty(value = "invoice_title")
    private String invoice_title;

    /**
     * 订单商品
     */
    private List<YJErpShipmentProductInfo> product_list;

    @JsonIgnore
    public String getOther_order_sn() {
        return other_order_sn;
    }

    @JsonIgnore
    public void setOther_order_sn(String other_order_sn) {
        this.other_order_sn = other_order_sn;
    }

    @JsonIgnore
    public String getConsignee() {
        return consignee;
    }

    @JsonIgnore
    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    @JsonIgnore
    public String getProvince() {
        return province;
    }

    @JsonIgnore
    public void setProvince(String province) {
        this.province = province;
    }

    @JsonIgnore
    public String getCity() {
        return city;
    }

    @JsonIgnore
    public void setCity(String city) {
        this.city = city;
    }

    @JsonIgnore
    public String getArea() {
        return area;
    }

    @JsonIgnore
    public void setArea(String area) {
        this.area = area;
    }

    @JsonIgnore
    public Integer getCheck_area() {
        return check_area;
    }

    @JsonIgnore
    public void setCheck_area(Integer check_area) {
        this.check_area = check_area;
    }

    @JsonIgnore
    public String getProvince_name() {
        return province_name;
    }

    @JsonIgnore
    public void setProvince_name(String province_name) {
        this.province_name = province_name;
    }

    @JsonIgnore
    public String getCity_name() {
        return city_name;
    }

    @JsonIgnore
    public void setCity_name(String city_name) {
        this.city_name = city_name;
    }

    @JsonIgnore
    public String getArea_name() {
        return area_name;
    }

    @JsonIgnore
    public void setArea_name(String area_name) {
        this.area_name = area_name;
    }

    @JsonIgnore
    public String getAddress() {
        return address;
    }

    @JsonIgnore
    public void setAddress(String address) {
        this.address = address;
    }

    @JsonIgnore
    public String getZipcode() {
        return zipcode;
    }

    @JsonIgnore
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    @JsonIgnore
    public String getTelephone() {
        return telephone;
    }

    @JsonIgnore
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @JsonIgnore
    public String getMobile() {
        return mobile;
    }

    @JsonIgnore
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @JsonIgnore
    public String getMessage() {
        return message;
    }

    @JsonIgnore
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonIgnore
    public String getOrder_from() {
        return order_from;
    }

    @JsonIgnore
    public void setOrder_from(String order_from) {
        this.order_from = order_from;
    }

    @JsonIgnore
    public Integer getDelivery_option() {
        return delivery_option;
    }

    @JsonIgnore
    public void setDelivery_option(Integer delivery_option) {
        this.delivery_option = delivery_option;
    }

    @JsonIgnore
    public String getSender() {
        return sender;
    }

    @JsonIgnore
    public void setSender(String sender) {
        this.sender = sender;
    }

    @JsonIgnore
    public String getSender_mobile() {
        return sender_mobile;
    }

    @JsonIgnore
    public void setSender_mobile(String sender_mobile) {
        this.sender_mobile = sender_mobile;
    }

    @JsonIgnore
    public String getSender_address() {
        return sender_address;
    }

    @JsonIgnore
    public void setSender_address(String sender_address) {
        this.sender_address = sender_address;
    }

    @JsonIgnore
    public Float getInsured_price() {
        return insured_price;
    }

    @JsonIgnore
    public void setInsured_price(Float insured_price) {
        this.insured_price = insured_price;
    }

    @JsonIgnore
    public Float getCollection_amount() {
        return collection_amount;
    }

    @JsonIgnore
    public void setCollection_amount(Float collection_amount) {
        this.collection_amount = collection_amount;
    }

    @JsonIgnore
    public Integer getLogistics_company_id() {
        return logistics_company_id;
    }

    @JsonIgnore
    public void setLogistics_company_id(Integer logistics_company_id) {
        this.logistics_company_id = logistics_company_id;
    }

    @JsonIgnore
    public String getLogistics_order() {
        return logistics_order;
    }

    @JsonIgnore
    public void setLogistics_order(String logistics_order) {
        this.logistics_order = logistics_order;
    }

    @JsonIgnore
    public Integer getDelivery_type_id() {
        return delivery_type_id;
    }

    @JsonIgnore
    public void setDelivery_type_id(Integer delivery_type_id) {
        this.delivery_type_id = delivery_type_id;
    }

    @JsonIgnore
    public Integer getIs_invoice() {
        return is_invoice;
    }

    @JsonIgnore
    public void setIs_invoice(Integer is_invoice) {
        this.is_invoice = is_invoice;
    }

    @JsonIgnore
    public Integer getInvoice_type() {
        return invoice_type;
    }

    @JsonIgnore
    public void setInvoice_type(Integer invoice_type) {
        this.invoice_type = invoice_type;
    }

    @JsonIgnore
    public String getInvoice_title() {
        return invoice_title;
    }

    @JsonIgnore
    public void setInvoice_title(String invoice_title) {
        this.invoice_title = invoice_title;
    }

    public List<YJErpShipmentProductInfo> getProduct_list() {
        return product_list;
    }

    public void setProduct_list(List<YJErpShipmentProductInfo> product_list) {
        this.product_list = product_list;
    }
    @JsonIgnore
    public String getShop_code() {
        return shop_code;
    }
    @JsonIgnore
    public void setShop_code(String shop_code) {
        this.shop_code = shop_code;
    }

    public String getShop_companycpde() {
        return shop_companycpde;
    }

    public void setShop_companycpde(String shop_companycpde) {
        this.shop_companycpde = shop_companycpde;
    }

    @JsonIgnore
    public String getShop_channels() {
        return shop_channels;
    }
    @JsonIgnore
    public void setShop_channels(String shop_channels) {
        this.shop_channels = shop_channels;
    }
}
