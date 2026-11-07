# 最终确认原始证据选集

2026-11-07从用户提供的route_a_final_confirmation.zip导入。完整原包23个校验项均通过；此目录保留9张未改动的Remix截图、交易前后状态、汇总和原始说明，共13个原文件，摘要见source-manifest.json。

README.txt是原包说明的原样副本，里面列出的完整原始RPC响应、采集脚本等保存在最终交付包的“最终确认原始证据”目录，未全部重复入库。该说明并非声称当前选集包含全部原包文件。

主对话另行直接查询POTOS，确认提交的receipt.json与独立回执逐字段一致，事件、订单和余额均通过核验：

- [独立核验与当前状态](../records/final-confirm-verification.json)
- [完整链上回执](../records/final-confirm-verified-receipts.json)
- [公开交易字段](../records/final-confirm-verified-transactions.json)

确认交易区块2828331；订单Completed/Refunded，最终余额40/60/0。原图取自Remix而非区块浏览器。交易和日志的transactionIndex差异原样保留，以transactionHash、blockHash、topics/data和真实状态共同核验。
