# PR #1 合并后联调记录

2026-11-06，PR [Update EscrowController](https://github.com/x12w/block-chain-lab/pull/1)
已合并，本地从提交 25c046b 开始复核。

实际启动 Spring Boot HTTP 服务，通过随机端口发送请求，再由 Service 调用本地 Hardhat 链。
所有账户在内存中随机生成，链 ID 为 31337；以下记录是本地自动化验证，不是 POTOS 三人验收。

发现并修复：查询不存在的订单时，Web3j 抛 ContractCallException，原代码误返回 502；
现在将其中的链上回滚映射为 409。单元测试改用真实异常类型，HTTP 集成测试覆盖实际回滚。
同时移除 Controller 测试里的固定示例私钥，改为运行时随机凭据。

运行 `bash gradlew test integrationTest bootJar --console=plain -Dhttps.protocols=TLSv1.2`：
12 个单元测试、2 个集成测试全部通过；25 次真实 HTTP 请求覆盖六接口和边界参数。
测试还验证超过 10 个区块的事件查询、失败仲裁回执及 Gas、退款 Transfer/OrderResolved、
买家确认、余额和 /load 恢复原合约。最终订单为 Completed/Refunded，余额为买家 70、卖家 30、托管 0。

原始材料：records/pr1-validation.log、四份 JUnit XML、summary.txt、
local-integration.json（Service 交易）、http-integration.json（HTTP 响应与交易回执摘要）。
交易哈希仅对应已销毁的本地测试账户和本地链，不能填入正式测试网交易表。
