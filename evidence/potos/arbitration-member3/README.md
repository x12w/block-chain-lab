# 成员3仲裁证据

2026-11-07由组长转交。操作人按用户说明为袁嘉骏；成员1提供后端，链上签名者为BE。五张PNG保持上传原样，摘要见source-manifest.json。

| 文件 | 来源与内容 |
|---|---|
| 01-failed-resolve-terminal.png | 成员3提交的终端记录图，显示HTTP409及失败响应 |
| 02-refund-terminal.png | 成员3提交的终端记录图，显示HTTP200及退款后状态 |
| 03-failed-resolve-explorer.png | 浏览器失败交易详情，块2823579 |
| 04-refund-explorer.png | 浏览器退款成功详情，块2823583 |
| 05-refund-logs.png | Transfer和OrderResolved日志 |

失败交易：0x6b6157111ef72bcacf69ab031d15ccaebd11cde978fa73637eebde039117f4c3。成功退款：0x41346bdd3a5ce73aa5bf86cb2436fd0c149338f94d1a00cf23d161313e130a7f。

公开回执、交易输入和日志由scripts/verify-potos-arbitration.cjs独立查询，保存到../records/arbitration-verified-receipts.json及arbitration-verification.json。
链上只能核验BE签名，实际操作人的归属依据用户提交说明。

终端图所示HTTP状态及响应文本按提交材料记录；目前未收到原始01-failed-resolve.json、02-refund.json及headers文件，不将截图转录冒充原始响应。

浏览器使用了refundToBuyer参数标签，本项目源码/ABI实际定义OrderResolved(uint256 indexed id, bool sellerWins)。参数名不进入事件签名；false表示sellerWins=false。应结合Transfer向买家支付30个及订单1=Refunded理解，不据浏览器标签反推业务含义。

RPC历史状态读取有局限：指定退款前块2823578也返回Refunded，不能将这次请求标成有效历史快照。成员3所报块2823657与本次独立当前查询分别保留来源。
