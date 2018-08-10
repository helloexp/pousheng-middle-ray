# 2.8.0（2018-08-10）

* 55178 斯凯奇调用中台接口优化
* 32994 中台对接外部平台店铺可界面配置
* 33002 中台对接斯凯奇，开发中台售后报表
* 35014 mpos库存查询尺码按照数字排序
* 39522 仅退款流程优化
* 40263 中台订单状态为待收货但同步电商平台失败
* 50103 商品分组-自动规则，当前自动规则无法自行排除之前通过自动规则加到商品分组的商品
* 51894 中台支持yyedi回传部分发货明细
* 拒收单同步恒康
* 售后仅退款不暂用退款数量

# 2.7.0（2018-08-01）

* 33001	中台已发货状态的发货单需要可以查询快递状态，查询ESP
* 33401	将中台订单/售后单商品优惠金额传给恒康
* 35013	手动派单不检核安全库存
* 35591	中台货品查询品牌筛选改成输入建议框
* 38777	创建售后订单时校验退货仓与店铺必须属同一公司：仅提醒，但不拦截
* 49983	云聚对接中台优化需求
* 50672	mPOS门店拒单后中台撤销发货单失败

# 2.6.0(2018-07-23)

* 32991	订单中心批量处理，弹出提示框
* 33845	拣货单打印模板调整
* 42462	中台商品分组，需增加同公司、全国的属性并与库存、派单关联
* 46143	中台商品分组，需要可以关联仓库
* 47907	中台库存 指定库存列表增加导入功能
* 48015	中台库存 中台库存管理列表增加字段-中台可用库存
* 48933	中台库存  优化-库存管理增加“货号”查询以及仓库查询控件做模糊匹配

# 2.5.1(2018-07-17)

* 圆通预置单号&物流号展示等
* mPOS派单和拒单时店铺的库存信息保存到数据库（方便Roger做报表）

# 2.5.0 (2018-07-12)
* 商品分组（线上店铺、线下店铺）
* 线上店铺库存推送调整
* 门店管理（接单时间、接单量）

# 2.4.0 (2018-07-09)
* 新版库存中心
* 用户操作日志

# 2.3.1 （2018-07-02）

* 新版派单逻辑
* 查询库存由查询恒康改造为查询中台
* 派单逻辑增加同公司
* 计算可用库存时 不计算非可用状态仓库的可用库存
* pmpid: 45333http://pmp.terminus.io/console/project/178/issues/45333

# 2.3.0 (2018-06-27)
* 云聚bbc

# 2.3.0 (2018-06-20)
* 增加能力中心文件

# 2.3.0 (2018-06-12)

* 京东匡威全渠道店铺
* 非mpos商品不参与门店发货
* 彭晖redis队列

## 2.2.0 (2018-06-12)

*【BUG】问题：YYEDI 通知发货单超时 http://pmp.terminus.io/console/project/178/issues/38115 负责人：谢洪
*【BUG】问题：YYEDI 通知退货完成超时 http://pmp.terminus.io/console/project/178/issues/38117 负责人：谢洪
*【BUG】EventBus-TaobaoConfirmRefundEvent(天猫或苏宁售后确认收货事件）优化 http://pmp.terminus.io/console/project/178/issues/37687 负责人：梁云举
*【BUG】EventBus- RefundShipmentEvent(售后换货时生成发货单事件，通知更新售后单状态)优化 http://pmp.terminus.io/console/project/178/issues/37686 负责人：梁云举
*【BUG】EventBus-UnLockStockEvent(发货单取消后回退库存)优化 http://pmp.terminus.io/console/project/178/issues/37685 负责人：谢洪
*【BUG】EventBus- ModifyMobileEvent(客服编辑订单收货信息)优化 http://pmp.terminus.io/console/project/178/issues/37684 负责人：谢洪
*【BUG】EventBus- OpenClientOrderSyncEvent(天猫脱敏信息事件)优化 http://pmp.terminus.io/console/project/178/issues/37683 负责人：梁云举
*【BUG】问题：天猫同步的单据未存入运费 http://pmp.terminus.io/console/project/178/issues/37681 负责人：赵小涛
*【BUG】问题：发货单状态与订单状态不同步，状态机未得到严格执行 http://pmp.terminus.io/console/project/178/issues/37679 负责人：谢洪
*【BUG】EventBus- NotifyHkOrderDoneEvent(通知恒康发货单时间)优化 http://pmp.terminus.io/console/project/178/issues/37682 负责人：谢洪
*【BUG】订单详情取消发货单状态更新问题调整 http://pmp.terminus.io/console/project/178/issues/39006 负责人：梁云举
# RELEASE NOTES

## 2.1.5 (2018-06-05)
 * 京东货到付款不拆单
## 2.1.4 (2018-06-05)

* 京东优惠分配调整
* 修复shopReadeService.findByName改为shopReadeService.findById

## 2.1.3 (2018-06-04)

* 修复Skx下载电商在售商品接口时间处理方式


## 2.1.2 (2018-05-31)

* 修复Skx取数逻辑
* 【BUG】生产：天猫Levis售后换货发货单，在订单派发中心收件人电话显示为空 http://pmp.terminus.io/console/project/178/issues/38147
* 【BUG】生产环境-中台系统卖家信息中的电话号码不能同步到EDI http://pmp.terminus.io/console/project/178/issues/38561
* 【BUG】锁定库存为负数 http://pmp.terminus.io/console/project/178/issues/38638

## 2.1.1 (2018-05-30)

* 增加全量处理库存同步的批处理任务
* 增加trade.job.enable: ${TRADE_JOB_ENABLE}、stock.job.enable: ${STOCK_JOB_ENABLE}环境变量区分交易job和库存job
* 增加leader.path: ${POUSHENG_ZK_PATH}环境变量，区分库存和交易集群leader
* 增加库存推送任务处理临时表(pousheng_temp_sku_stock_updated)，减少全量库存推送第三方请求次数
* 增加 trigger_insert_on_sku_stocks 、 trigger_update_on_sku_stocks 触发器


## 2.1.0 (2018-05-25)

* 恒康库存库存改造，先将数据落到DB然后再消费
* 库存推送调优

## 2.0.9 (2018-05-24)

* 发货单重复最终方案

## 2.0.8 (2018-05-22)

* 同步恒康接口日志优化
* 京东订单取值修改
* 批量处理订单时重复生成发货单的修复
* 同步订单派发中心失败的取消不经过yyedi直接取消
* 同步订单派发中心失败的取消不经过yyedi直接取消
* 34567:预发环境-斯凯奇发货单 取消失败

## 2.0.7 (2018-05-21)

*  中台schema完善
*  仓库批量设置修改（仓库id相等判断）
* skx查询电商在售接口调整由查询推送记录改为查询映射表

## 2.0.6 无


## 2.0.5 (2018-05-17)

 *【bug】预发环境-斯凯奇发货单 取消失败和skx库存查询问题    34567   hotfix/fix-shipment-cancel-bug
 *【新增】默认发货仓规则批量添加仓库  "http://pmp.terminus.io/console/project/178/issues/33172"
 *【新增】中午外网访问限制（5月17日上线）    http://pmp.terminus.io/console/project/141/issues/29499
 *【bug】生产&测试：发货单导出文件中，发货单状态与搜索结果不一致。没有区分订单派发中心和Mpos，BUGID-35758
## 2.0.4 (2018-05-10)

*  【BUG】生产环境-中台导出订单/售后订单/发货单没有按照权限导出	http://pmp.terminus.io/console/project/141/issues/31747 ok
*  【任务】发货单显示拒单理由	http://pmp.terminus.io/console/project/178/issues/33168 ok
*  【任务】发货单根据订单来源和发货仓库所属的店铺展现。	http://pmp.terminus.io/console/project/178/issues/33252 ok
*  【任务】中台仓库库存不正确	http://pmp.terminus.io/console/project/178/issues/34627 ok
*  【BUG】修复发货单和售后单导出里的id为code	http://pmp.terminus.io/console/project/178/issues/34951

## 2.0.3 (2018-04-26)

* 苏宁特卖
* 修复宝胜pos单的bug http://pmp.terminus.io/console/project/178/issues/33391
* 修复发货单无头件相关数据


## 2.0.2 (2018-04-25,本次不变更版本号)
* 修复售后单拉取问题
* 售后单添加关联单号字段

## 2.0.2 (2018-04-24)

* 中台二期迭代迭代需求
* 中台对接skx
* 区部联系人


## 2.0.1 (2018-04-18)

* 修复同步恒康仓库时仓库安全库存覆盖问题 pmp:http://pmp.terminus.io/console/project/141/issues/32134

## 2.0.0 (2018-04-17)

* 新增“下单店铺所属公司”代码到YYEDI PMPID: 30747
* 发货单发货更新订单状态优化 PMPID: 31336
* 中台发送邮件获取邮箱需要实时从会员中心获取门店邮箱信息 PMPID: 31678
* 预发中台订单状态错误 PMPID: 30280
* 中台门店管理列表获取仓库地址和电话需要实时从会员中心获取仓库信息 PMPID:31847