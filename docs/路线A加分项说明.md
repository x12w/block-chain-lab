# 路线A三个加分项

本次在已完成的路线A上增加订单时间线、卖家发货状态和双人仲裁，评分任务书中三项各3分。原POTOS合约及两笔已结算订单保留；发货和双人仲裁由独立的 `EscrowEnhanced` 合约实现。

## 验收状态

| 项目 | 实现与已保存证据 | 验收范围 |
|---|---|---|
| 订单时间线 | `GET /api/escrow/orders/{id}/events`；原订单0有3条事件，订单1有4条事件 | 已通过真实POTOS只读HTTP验收，另复查运行中的8080服务 |
| 卖家发货 | `Shipped=5`、`markShipped(id)`，仅卖家可在截止时间前将Paid订单改为Shipped | 本地EVM与真实HTTP验收通过；测试网协作记录待补 |
| 双人仲裁 | 两个不同的非零地址各投一票，意见一致时执行一次结算 | 本地EVM与真实HTTP验收通过；测试网协作记录待补 |

19项单元测试、5项本地集成测试通过，测试数按JUnit测试方法统计，每个集成方法内另含多笔交易和多条断言。本地随机账户、交易和余额均明确标为chainId31337，不能填入POTOS正式交易表。

增强合约现已部署至POTOS：`0x917Dc833eadBa6544F1fd80498715da39Ac739f4`，部署块`2829547`，version=2。第一仲裁地址为袁嘉骏的BE，第二仲裁地址为魏琪霖现有买家钱包。本次为课程角色演示，魏琪霖兼任买家和第二仲裁人。部署通过不等于发货、退款和双人投票协作已验收；这些仍待钱包实际操作。

2026-11-07首次增强订单0创建成功，付款却在截止后95秒入块而回滚，订单保持Created且过期；授权仍3个、余额40/60/0。真实失败回执和独立复核已归档于`evidence/bonus/potos/expired-order0-*`。这是操作时间安排不足，不计为发货或双人仲裁成功证据。接续方案使用更长时限及新的实际订单ID，详见协作步骤。

## 订单时间线

`OrderTimeline.java`查询创建、付款、完成、退款、争议、裁决、发货和投票八种事件。使用合约地址、事件topic0和indexed订单ID筛选；每次日志查询至多10个区块，每个JSON-RPC批次至多8个请求。按区块号、交易索引、日志索引排序，排除removed日志并去重；RPC错误或响应缺失时整体失败，不返回伪装为完整结果的部分记录。

原订单的真实事件顺序：

| 订单 | 事件与区块号 |
|---|---|
| 0 | OrderCreated 2822499 → OrderPaid 2822648 → OrderCompleted 2828331 |
| 1 | OrderCreated 2822524 → OrderPaid 2822676 → OrderDisputed 2823067 → OrderResolved 2823583 |

订单1最后一条事件的 `sellerWins=false`，对应既有退款结果。每条响应包含完整交易哈希、区块哈希、合约地址和事件位置。接口从部署区块扫至查询开始时的最新区块，历史跨度较长时查询较慢；本版没有增量缓存。

## 发货与退款

状态枚举保留原来的0至4，追加 `Shipped=5`。仅订单卖家能调用 `markShipped`，订单须处于Paid且 `block.timestamp <= deadline`。这样已到期的未发货订单不会被迟到发货夺走退款资格。

买家超期退款要求状态仍为Paid且 `block.timestamp > deadline`。已发货订单不能通过超期路径退款，但仍可由买家确认收货，或由买卖双方发起争议后走仲裁。为兼容原路线A流程，买家在Paid或Shipped均可确认。

`POST /orders/{id}/mark-shipped`使用当前后端签名账户。原仲裁BE不是卖家，调用该接口应被合约拒绝；正式协作时卖家在Remix用本人钱包调用，无需向后端提供卖家私钥。

## 双人仲裁

构造函数接收 `first` 和 `second` 两个不同的非零地址。`resolve(id,sellerWins)`只允许这两个地址对Disputed订单投票。第一票成功仅记录 `ArbitrationVoted`，HTTP200和成功回执不等于订单已结算；应继续查询状态及投票详情。

两票意见冲突时，订单仍为Disputed、托管余额不变。未结算前投票人可以改投不同意见，同一地址重复提交同一意见会失败。两个地址意见一致后，合约改变状态、支付既定买家或卖家，并发出 `OrderResolved`；结算后再次投票被状态检查拒绝。

## 新增接口

以下路径均以 `/api/escrow` 为前缀；原有部署、加载、地址、订单、列表和裁决接口保持可用。

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/orders/{id}/events` | 查询一个订单的全部已支持事件 |
| POST | `/deploy-enhanced?secondArbiter=地址` | 部署增强合约，第一仲裁人为当前后端签名账户 |
| POST | `/load-enhanced?address=地址&fromBlock=部署块` | 校验链、合约代码和version=2后加载增强合约 |
| GET | `/arbitrators` | 两位仲裁人、当前后端签名人和票数门槛 |
| GET | `/orders/{id}/votes` | 两个地址是否已投票及各自意见 |
| POST | `/orders/{id}/mark-shipped` | 使用当前后端账户签名发货交易 |
| POST | `/orders/{id}/resolve?sellerWins=false` | 原裁决路径；加载增强合约时表示当前签名人投票 |

无效参数返回400，合约状态或权限不满足返回409，RPC等上游异常返回502。失败的链上交易保留回执信息。错误加载增强合约不会覆盖此前有效绑定；服务重启后需要重新加载。

## 复现与材料

先按README启动chainId31337本地链，再执行：

```bash
bash gradlew test integrationTest bootJar --console=plain
python3 scripts/summarize-bonus.py
```

单独执行真实POTOS时间线只读验收：

```bash
bash gradlew potosTimelineAcceptance --console=plain
```

证据目录：`evidence/bonus/timeline/`保存真实HTTP响应；`evidence/bonus/enhanced/`保存JUnit报告、本地交易回执、HTTP请求与源码摘要；`evidence/bonus/screenshots/`保存注明来源的记录展示图。每类截图明确区分真实POTOS查询与本地自动化验证。

测试网剩余协作见[加分项测试网操作说明](加分项测试网操作说明.md)。三人贡献百分比继续留空，由小组自行填写。得分由教师结合实现、证据及答辩评定。
