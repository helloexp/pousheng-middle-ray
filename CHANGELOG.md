# RELEASE NOTES

## 2.1.2 (2018-05-31)

* 修复Skx去数逻辑

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