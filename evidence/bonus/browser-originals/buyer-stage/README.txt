POTOS 路线 A 接续批次：买家创建与付款阶段核验完成
全部时间为北京时间（UTC+8）。
增强合约：0x917dc833eadba6544f1fd80498715da39ac739f4
买家：0x5a435c43ca006be1688d0d7b95194327528eab26
Token：0xb7da837f93a5448ca0762b80862d053daf9715b3
本阶段仅发起已授权的 6 笔交易，全部 status=0x1：3 笔创建、3 笔 deposit，各 1 WQL，原生币 Value 均为 0。没有重部署、重 approve、重复点击付款或额外交易。
余额：买家 37 WQL，卖家 60 WQL，增强合约 3 WQL；剩余 allowance=0。

最新状态：
订单 1：Shipped (5)；用途 正常发货确认；截止 2026-11-08 01:25:56
订单 2：Disputed (4)；用途 双人仲裁退款；实际为未发货即争议；截止 2026-11-08 01:33:08
订单 3：Paid (1)；用途 未发货超期退款；截止 2026-11-08 00:39:46

卖家顺序偏差（必须保留）：
订单 2 在 2026-11-07 23:50:53 先 dispute 成功，随后 23:52:12 的 markShipped 失败（status=0x0）；只读 eth_call 返回 Escrow: wrong status。因此它不是发货后争议，不得记录为该路径已通过。当前两位仲裁投票均为 false。

后续交接（本批次未执行）：
1. 订单 1 保持 Shipped，待 2026-11-08 01:25:56 后，先验证 refund(1) 失败，再 confirm(1)。
2. 订单 2 现为 Disputed。主对话需要记录上述偏差并协调仲裁：第一仲裁 resolve(2,false)，第二仲裁 resolve(2,true) 证明冲突不结算，最后第二仲裁 resolve(2,false) 退款。不得把未完成的发货后争议场景写为已完成；本批次未自动补单。
3. 订单 3 已 Paid，卖家不要发货或争议。待 2026-11-08 00:39:46 后再 refund(3)。
4. 失败订单 0 保留历史事实：Created 且过期，旧 deposit 失败，没有托管款项，无需退款；没有重试。

完整交易时间线：
1 | createOrder | 2026-11-07 23:27:54 | block 2835321 | status 0x1
0x64c24b30d7afbc3f423da80358ece8ac1771f5761fcb00508907b8b7d7d97fe0
https://scan-testnet.potos.hk/tx/0x64c24b30d7afbc3f423da80358ece8ac1771f5761fcb00508907b8b7d7d97fe0
1 | deposit | 2026-11-07 23:31:20 | block 2835351 | status 0x1
0xf2559557e9ba952abbf2456ccea321cb7cdfef53e23c9db81f871cad6ef816ee
https://scan-testnet.potos.hk/tx/0xf2559557e9ba952abbf2456ccea321cb7cdfef53e23c9db81f871cad6ef816ee
2 | createOrder | 2026-11-07 23:35:53 | block 2835388 | status 0x1
0xe9398d162f065e68ed3a2c109bfb2ca88183f1a3d077ed35f3fea148a27e8bb9
https://scan-testnet.potos.hk/tx/0xe9398d162f065e68ed3a2c109bfb2ca88183f1a3d077ed35f3fea148a27e8bb9
1 | markShipped | 2026-11-07 23:37:06 | block 2835401 | status 0x1
0x68fd629d80512648ab23d7ac2ef3c7319ec445b84df6bd28e1d84ad6f14c3df0
https://scan-testnet.potos.hk/tx/0x68fd629d80512648ab23d7ac2ef3c7319ec445b84df6bd28e1d84ad6f14c3df0
2 | deposit | 2026-11-07 23:38:46 | block 2835418 | status 0x1
0xdd782dc5b16477873ca573808de496e647b2dd2e8ad44b231590086f5f384e5d
https://scan-testnet.potos.hk/tx/0xdd782dc5b16477873ca573808de496e647b2dd2e8ad44b231590086f5f384e5d
3 | createOrder | 2026-11-07 23:41:26 | block 2835440 | status 0x1
0x7d882fcb8b7b656ceda390bee1b3e666bf970478877ecff42264ccf9e8a1d9e6
https://scan-testnet.potos.hk/tx/0x7d882fcb8b7b656ceda390bee1b3e666bf970478877ecff42264ccf9e8a1d9e6
3 | deposit | 2026-11-07 23:50:04 | block 2835507 | status 0x1
0x0140fb9818401908c054a9f9cb19ebf4cf5fa52d47f07c37ae87b941f74be9c0
https://scan-testnet.potos.hk/tx/0x0140fb9818401908c054a9f9cb19ebf4cf5fa52d47f07c37ae87b941f74be9c0
2 | dispute | 2026-11-07 23:50:53 | block 2835515 | status 0x1
0xfe41250933e363cd280e8c77f221bc88a1a2d4453097433134e95ee352ed3e23
https://scan-testnet.potos.hk/tx/0xfe41250933e363cd280e8c77f221bc88a1a2d4453097433134e95ee352ed3e23
2 | markShipped FAILED | 2026-11-07 23:52:12 | block 2835525 | status 0x0
0xc6ee3ac58363428cd89bfe9285f7fc557c6a8ed293cc2dd44ee4add86bf8dcb9
https://scan-testnet.potos.hk/tx/0xc6ee3ac58363428cd89bfe9285f7fc557c6a8ed293cc2dd44ee4add86bf8dcb9

取证说明：
Remix 对 deposit(3) 报 _context7.t3.error.indexOf is not a function，但 RPC 真实回执为成功，OrderPaid(3)、1 WQL Transfer 和 getOrder(3)=Paid 均已核验，不应重复付款。
部分 eth_getLogs 区间查询未返回最新日志；采用实际交易回执 logs 作为事件证据。
seller_provided/ 为用户转交的卖家原始截图；其他 PNG 为本对话直接截取的 Remix 界面。JSON 保存公开 RPC 请求、响应及状态。
旧 pending 状态 JSON 是历史快照；当前状态以 status.json 和此 README 为准。