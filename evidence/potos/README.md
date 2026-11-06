# POTOS 正式测试网：成员 1 已完成部分

2026-11-06，链 ID 60600。用户在本机终端隐藏输入后端专用私钥并启动服务；
通过 localhost REST 接口发送测试网交易，原始响应在 records/。
这部分是真实 POTOS 数据，与 evidence/pr1-integration 的本地模拟数据分开。

- SimpleStorage：0x2EB79Bf8A18EBF0518501eFa0b6DB9D48075D786，set(100) 成功，get 返回 100。
- 新 Escrow：0xd7320d80b687766bA73997B97dAE61Ff1f9ed737，部署区块 2821876。
- GET address 与链上 arbiter 均匹配 0x4c688b060912faf95324a43C0190C59E7cE8aB3c。
- /load 已验证；部署后的订单列表为空，尚未开展买家/卖家协作。
- verified-receipts.json 来自 RPC eth_getTransactionReceipt，验证 status、from、目标合约和部署区块。
- verification-summary.json 为上述只读核验摘要；08-token-preflight.json 为组长代币只读核验摘要。
  本次合约部署信息以 docs/route-a-deployment.json 为准。

截图包括 4A-3 三条响应、set 交易浏览器页，以及 4A-5 的部署/仲裁人信息。
终端响应截图使用 show-potos-evidence.py 展示已保存的实际 curl 输出，并明确标注为已保存记录；
没有为截图重复部署或修改响应。Controller 的九张完整代码截图在 evidence/pr1-integration/screenshots/。

尚缺：两笔订单创建、approve、deposit、卖家 dispute、三条订单 GET、
失败 resolve、退款 resolve、买家 confirm，以及退款交易 From/Logs 截图。
不能将部署成功等同于完成整组验收。
