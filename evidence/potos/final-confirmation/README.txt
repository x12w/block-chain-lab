路线 A 最终买家确认：原始证据包

本次仅新增 1 笔链上业务交易：confirm(0)。没有重新部署、重新创建订单、追加授权、重复付款或重复仲裁。操作时复用了 Remix 原有 Escrow.sol 与 WQLToken.sol，经 Add Contract / At Address 绑定已有地址。

网络：POTOS Testnet，chainId=60600，原生测试币 POT。
RPC：https://rpc-testnet.potos.hk
交易浏览器链接：https://scan-testnet.potos.hk/tx/0x44db2d985fee8be415c46c35a8422840cb463691aeef9f95d5f12b66ca23ea20
（链接为定位入口，本包依据实际 RPC 回执与 Remix 截图取证，未依赖浏览器页面解释。）

买家A / 签名者：0x5a435c43Ca006bE1688d0D7B95194327528EAB26
卖家B：0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2
新Escrow / To：0xd7320d80b687766bA73997B97dAE61Ff1f9ed737
Token：0xB7dA837F93A5448ca0762B80862d053daf9715B3，18 位小数。
实际 arbiter：0x4c688b060912faf95324a43c0190c59e7ce8ab3c

最终交易
交易哈希：0x44db2d985fee8be415c46c35a8422840cb463691aeef9f95d5f12b66ca23ea20
调用：confirm(uint256)，id=0
交易 input：0xba0179b5 + 32 字节的 0；原生币 Value=0
回执 status：0x1
blockNumber：2828331（0x2b282b）
交易回执中的 transactionIndex：0x0
区块时间（北京时间）：2026-11-07T09:25:25+08:00
gasUsed：62541（0xf44d）
effectiveGasPrice：0x0，即 0 wei/gas
实际执行手续费：62541 × 0 = 0 wei = 0 POT
maxFeePerGas 与 maxPriorityFeePerGas 是交易字段，不应代替回执中的 effectiveGasPrice 计算实际执行手续费。

事件解码（依据原始 topics/data）
1. WQLToken Transfer(address indexed from,address indexed to,uint256 value)
   from=0xd7320d80b687766bA73997B97dAE61Ff1f9ed737
   to=0xE0F0970f35FFE50dB6a6AD25ba775674D24682D2
   value=30000000000000000000，即 30 个代币。
   topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
2. 新Escrow OrderCompleted(uint256 indexed id)
   id=0，data=0x。
   topic0=0x02a7d4d9472af7118644884b9a0ee443540c0027e938dd0aa35be8ecbe946c0a
   准确事件名称是 OrderCompleted，不是 OrderConfirmed。

前后实查
订单0：Paid(1) -> Completed(2)，description=route A normal order。
订单1：Refunded(3) -> Refunded(3)，description=route A disputed order。
两笔订单的 buyer、seller、token、amount、deadline 均与交接匹配，完整字段见 before_state.json / after_state.json。
买家A：40 -> 40。
卖家B：30 -> 60（原有30个不计作本次新增收入）。
Escrow：30 -> 0。
买家对新Escrow的 allowance：0 -> 0。
交易前读取北京时间：2026-11-07T09:21:54.829381+08:00 至 2026-11-07T09:22:38.199123+08:00，期间块高 [2828302, 2828307]。
交易后读取北京时间：2026-11-07T09:26:25.555269+08:00 至 2026-11-07T09:27:04.353072+08:00，期间块高 [2828341, 2828346]。
以上是多次 latest 查询，不能表述成单个历史块的原子快照。交接已提示该 RPC 的历史 blockTag 不可靠，本次未用历史读数推断旧状态。

操作经过
1. 恢复 Browser Extension / MetaMask；通过页面账户列表 DOM 核对完整买家地址，Remix 显示 Account 16。
2. At Address 绑定指定的新Escrow；独立只读 RPC 查询两笔订单、arbiter、decimals、三方 balanceOf 和 allowance；并在 Remix 查询两笔订单。
3. 将 confirm 的 id 设为0，核对 Value=0，在 Remix 点击 Transact 一次。等待时未再次点击或重新发送。
4. 等待期间 Remix 尚未提供哈希；一次只读 OrderCompleted 日志查询返回空数组。随后 Remix 显示本次确认已成功。空数组仅代表那次中间查询结果，不表示最终没有交易。
5. 从 Remix 展开详情取得完整交易哈希；查询回执、交易字段和区块信息，核验成功状态、From/To、input、Value、链ID和两条事件。
6. 再次独立查询最终订单/余额/allowance；在 Remix 补拍两笔最终订单和三方 balanceOf。
7. 成功后未再进行任何链上写操作。全部截图是连接的 Distrobox Chrome 中 app.remix.live 页面原始截图，没有重绘终端、拼接或修改像素。

取证说明
- Remix 终端摘要的 hash: 0x3fc...c3f85 实际对应 blockHash；完整 transaction hash 以展开详情、receipt.json 和 transaction.json 为准。
- Remix 解码栏 logs 显示 []，但 raw logs 和 RPC 回执有2条日志；事件名称与参数依据原始 topics/data 解码。
- RPC 原始日志的 transactionIndex 返回 0x307830，Remix 显示为3176496；回执和交易对象的 transactionIndex 为0x0。本包原样保留差异，未篡改字段。
- 此包来自本操作对话的工作环境；未声称写入主对话本机路径。

文件说明
receipt.json：原始 eth_getTransactionReceipt 结果。
transaction.json：原始 eth_getTransactionByHash 结果（包含公开的交易签名字段，无私钥/助记词）。
confirmation_block.json：RPC 区块结果。
receipt_rpc_raw.json：上述查询的请求、原始响应、UTC起止时间。
before_state.json / after_state.json：可读解码状态及读取期间块高范围。
before_rpc_raw.json / after_rpc_raw.json：对应只读查询的逐条原始响应。
verified_summary.json：经过断言核验的交易、事件解码及最终状态汇总。
existing_completed_logs.json / completed_logs_rpc_raw.json：等待期间的中间只读日志查询，最终结果以成功回执为准。
read_chain.py：只读证据采集脚本，不包含发送交易或签名方法。
screenshots_manifest.json：截图来源、内容、路径及SHA256。
SHA256SUMS.txt：包内文件校验值（该校验清单本身除外）。

截图清单（9张）
1. screenshots/01_before_order0.jpg — 交易前 Remix getOrder(0)：Paid(1)，30 个代币。
2. screenshots/02_before_order1.jpg — 交易前 Remix getOrder(1)：Refunded(3)。
3. screenshots/03_confirm_success.jpg — Remix confirm(0) 输入、买家账户、POTOS Testnet 和成功终端记录；块 2828331。
4. screenshots/04_receipt_and_raw_logs.jpg — Remix 展开交易详情：status=1、完整交易哈希、From/To、62541 gas 及两条 raw logs。
5. screenshots/05_final_order0_completed.jpg — 交易后 Remix getOrder(0)：Completed(2)。
6. screenshots/06_final_order1_refunded.jpg — 交易后 Remix getOrder(1)：Refunded(3)。
7. screenshots/07_final_buyer_balance_40.jpg — Remix WQLToken.balanceOf(买家A)：40000000000000000000。
8. screenshots/08_final_seller_balance_60.jpg — Remix WQLToken.balanceOf(卖家B)：60000000000000000000。
9. screenshots/09_final_escrow_balance_0.jpg — Remix WQLToken.balanceOf(新Escrow)：0。
