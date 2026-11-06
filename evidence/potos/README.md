# POTOS 正式测试网：成员 1 已完成部分

2026-11-06，链 ID 60600。用户在本机终端隐藏输入后端专用私钥并启动服务；
通过 localhost REST 接口发送测试网交易，原始响应在 records/。
这部分是真实 POTOS 数据，与 evidence/pr1-integration 的本地模拟数据分开。

- SimpleStorage：0x2EB79Bf8A18EBF0518501eFa0b6DB9D48075D786，set(100) 成功，get 返回 100。
- 新 Escrow：0xd7320d80b687766bA73997B97dAE61Ff1f9ed737，部署区块 2821876。
- GET address 与链上 arbiter 均匹配 0x4c688b060912faf95324a43C0190C59E7cE8aB3c。
- /load 已验证；07-escrow-empty-orders.json 是部署初期的历史空列表，不代表当前状态。
- verified-receipts.json 来自 RPC eth_getTransactionReceipt，验证 status、from、目标合约和部署区块。
- verification-summary.json 为上述只读核验摘要；08-token-preflight.json 为组长代币只读核验摘要。
  本次合约部署信息以 docs/route-a-deployment.json 为准。

截图包括 4A-3 三条响应、set 交易浏览器页，以及 4A-5 的部署/仲裁人信息。
终端响应截图使用 show-potos-evidence.py 展示已保存的实际 curl 输出，并明确标注为已保存记录；
没有为截图重复部署或修改响应。Controller 的九张完整代码截图在 evidence/pr1-integration/screenshots/。

买家创建/授权/付款已完成。buyer-verified-receipts.json 为五笔实际 RPC 回执；
buyer-verification.json 为参数、事件和同一区块的历史付款快照，当时两笔Paid，余额为买家 10、卖家 30、Escrow 60。
buyer-paid-http-precheck.json 为实际后端 GET 预检，Paid/Paid 不能替代正式 Paid/Disputed。

两张买家核验终端截图由 show-buyer-evidence.py 展示保存的真实记录，不冒充 Remix 界面。
原浏览器截图包已校验导入 buyer/，包含 Remix 创建/查询图和浏览器付款成功图。verify-potos-buyer.cjs 使用 ethers 解码，
可通过 ETHERS_MODULE 指向已有安装，使用生成的 build ABI。一次只读查询遇到 TLS 错误后，
已使用 TLS 1.2 和有限重试完成读取，未关闭证书校验。

卖家dispute(1)已由张俊仁完成：区块2823067，gasUsed=36385；seller/保存3张原图。
seller-verified-receipts.json为独立RPC原始回执，seller-dispute-verification.json确认块2823202时Paid/Disputed、余额10/30/60。
paid-disputed-http-verification.json保存正式GET原始响应；4A-6-paid-disputed-http-records.jpg为保存响应的浏览器展示截图，明确标识来源。

尚缺：成员3袁嘉骏亲自执行失败resolve、退款resolve，买家confirm，以及退款交易From/Logs截图。
不能将部署成功等同于完成整组验收。
