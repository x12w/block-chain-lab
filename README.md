# 区块链大作业：路线 A，成员 1 交付

成员 1：魏琪霖，学号 202430332012，第 15 组。日期：2026-11-06。

本仓库完成合约 Java 封装、EscrowService、环境变量配置和本地自动化验证。
EscrowController 已由成员 2 接入，合并后联调通过；已在 POTOS 部署新 Escrow 并完成
SimpleStorage 验证。买家两笔创建、授权 60 个与两笔付款均已独立核验，当前两笔 Paid。
下一步由成员 2 卖家执行 dispute(1)，再完成后端仲裁与买家确认。
原实验项目不受影响。本项目基于课程 spring-boot-web3j 示例，合约复制自组长实验 3 源码。

## 已完成

- 生成 Escrow、IERC20、WQLToken、SimpleStorage 的 Java 封装。
- EscrowService 支持部署、加载、查询地址/仲裁人、订单详情、事件订单列表、仲裁。
- OrderCreated 按闭区间每批最多 10 个区块查询，RPC 失败会报错，不伪装为空列表。
- 以文字返回订单状态；仲裁的角色与状态检查由合约执行；失败回执保留给 Controller。
- 私钥由 WEB3J_PRIVATE_KEY 注入，配置类无硬编码后备私钥，链 ID 默认 60600。
- 12 个单元测试和 2 个本地链集成测试已通过；其中 HTTP 集成测试实际启动 Spring 服务，
  验证 25 次请求、六个接口、参数转换、失败回执、退款、结算和重新加载。
  本次合并联调证据见 [evidence/pr1-integration](evidence/pr1-integration)，早期成员 1 材料见 [evidence/member1](evidence/member1)。

## 环境与运行

需要 JDK 21。Gradle 8.13 使用仓库内包装器；首次运行需要下载依赖。

```bash
bash gradlew generateContractWrappers
bash gradlew test bootJar
```

组员先克隆：`git clone git@github.com:x12w/block-chain-lab.git`，进入 `block-chain-lab` 后执行上面的命令。

组长本机已有课程容器：先在宿主机执行 `distrobox enter blockchain-course`，
再进入 `/home/x12w/projects/block-chain/大作业/route-a-member1`。

正式运行时使用专用测试网仲裁账户。可用下面脚本在终端隐藏输入私钥：

```bash
bash scripts/run-backend.sh
```

脚本不保存或输出私钥；不要把私钥写入代码、报告、截图或可提交的 .env 文件。
不要使用原示例的公共默认账户，也不要把个人买家钱包作为“新后端账户”混用。

| 环境变量 | 默认/要求 |
|---|---|
| WEB3J_PRIVATE_KEY | 必填，后端专用账户私钥；仅运行时提供 |
| WEB3J_RPC_URL | https://rpc-testnet.potos.hk |
| WEB3J_CHAIN_ID | 60600 |
| WEB3J_GAS_PRICE | 1000000 wei |
| WEB3J_GAS_LIMIT | 6721975 |
| SERVER_ADDRESS | 127.0.0.1 |
| SERVER_PORT | 8080 |

在课程容器中运行时，可直接在容器终端 curl localhost:8080。
需要宿主机浏览器连接时，在启动前设置 SERVER_ADDRESS=0.0.0.0；
该课程容器的 8080 端口仅映射到宿主机 127.0.0.1。不要把无认证接口公开到互联网。

当前保留示例 SimpleStorageController。六个 `/api/escrow` 接口已由成员 2 在
`EscrowController.java` 中实现，并有本地单元测试覆盖状态码与失败回执；仍需在测试网完成
curl 验收并保存 4A-4～4A-8 截图。接口对接见 [docs/成员2接口对接.md](docs/成员2接口对接.md)。

## 本地集成测试

仅连接 127.0.0.1:18545，强制校验 chainId=31337。随机生成三组临时账户、
用 Hardhat RPC 设置本地余额，密钥只在测试进程内存中存在，不写文件。
这些数据不可作为 POTOS 或三人独立操作的验收数据。

```bash
# 终端 1：HARDHAT_PACKAGE_DIR 指向已安装的 Hardhat 2 软件包目录
export HARDHAT_PACKAGE_DIR="$HOME/blockchain-course/my-first-dapp/packages/hardhat/node_modules/hardhat"
bash scripts/start-local-chain.sh
# 终端 2
bash gradlew integrationTest
```

无需重新安装已有前端项目。本地链不会加载旧项目的部署脚本或钱包配置。
结果见 build/reports/tests/、build/local-evidence/local-integration.json 和 http-integration.json。
执行 `python3 scripts/summarize-validation.py` 汇总四个测试类与真实回执。
如本机 Java 下载依赖偶发 TLS 错误，可重试时使用 `-Dhttps.protocols=TLSv1.2`；不要关闭证书校验。

## 合约资料与交易记录

旧实验 3 的地址、部署区块和交易哈希在 [docs/lab3-reference.json](docs/lab3-reference.json)。
组长代币 WQLToken：`0xB7dA837F93A5448ca0762B80862d053daf9715B3`，18 位精度。
旧 Escrow：`0x249F6c83F28864e7c863306CC6DE7f936B339724`，只用于历史参考。
路线 A 必须由专用后端账户重新部署，不能把旧 Escrow 当成本次新部署。
只读核验命令：`python3 scripts/potos-preflight.py`（不读取密钥、不发送交易）。

路线 A 新 Escrow：`0xd7320d80b687766bA73997B97dAE61Ff1f9ed737`，部署区块 `2821876`。
后端/仲裁账户：`0x4c688b060912faf95324a43C0190C59E7cE8aB3c`。
正式部署信息见 [docs/route-a-deployment.json](docs/route-a-deployment.json)，
响应、链上回执、截图见 [evidence/potos](evidence/potos)。

后端已由用户在本机隐藏输入私钥并启动；本仓库不保存密钥。
应用重启后必须加载现有合约，避免误部署新的订单空间：

```bash
curl -X POST 'http://127.0.0.1:8080/api/escrow/load?address=0xd7320d80b687766bA73997B97dAE61Ff1f9ed737&fromBlock=2821876'
```

| 操作 | 交易哈希 | 区块浏览器链接 | 发起账户 |
|---|---|---|---|
| 部署新 Escrow | 0xdd1e95f4a34a0298e2051c47e049cc49a3f654585510465950dd9a444cc034aa | [交易详情](https://scan-testnet.potos.hk/tx/0xdd1e95f4a34a0298e2051c47e049cc49a3f654585510465950dd9a444cc034aa) | 后端 BE |
| createOrder 订单 0 | 0x4f4ebf253000b7ad52e6da3d50fc98b65d9431f2f1f69946fe1d94d768920099 | [交易详情](https://scan-testnet.potos.hk/tx/0x4f4ebf253000b7ad52e6da3d50fc98b65d9431f2f1f69946fe1d94d768920099) | 买家 A |
| approve | 0xc8769e7a8fe9ad68c7c3730bd0c5d0238c9410a9d0415dca7e7a90a6cee53d08 | [交易详情](https://scan-testnet.potos.hk/tx/0xc8769e7a8fe9ad68c7c3730bd0c5d0238c9410a9d0415dca7e7a90a6cee53d08) | 买家 A |
| deposit 订单 0 | 0x1801f6112ae79e1987a59dd2c7799e7ed6c6a1ade52ef2e8e3ecdf60f0ffbed5 | [交易详情](https://scan-testnet.potos.hk/tx/0x1801f6112ae79e1987a59dd2c7799e7ed6c6a1ade52ef2e8e3ecdf60f0ffbed5) | 买家 A |
| dispute 订单 1 | 待协作 | 待协作 | 卖家 B |
| resolve 订单 1 | 待协作 | 待协作 | 后端 BE |
| confirm 订单 0 | 待协作 | 待协作 | 买家 A |
| resolve 订单 0 失败 | 待协作 | 待协作 | 后端 BE |

额外保存订单 1 的创建、付款交易：

| 操作 | 交易哈希/浏览器 | 发起账户 |
|---|---|---|
| createOrder 订单 1 | [0x6965468ab5104bf9c202d3f8c0e8c59b2fa6da24eb224c225c4b5b120d15ec6e](https://scan-testnet.potos.hk/tx/0x6965468ab5104bf9c202d3f8c0e8c59b2fa6da24eb224c225c4b5b120d15ec6e) | 买家 A |
| deposit 订单 1 | [0x24dc8f81f69ff0c3183fb79169d951a93b30e7c2e9cf7d08f124e95ab5c949c0](https://scan-testnet.potos.hk/tx/0x24dc8f81f69ff0c3183fb79169d951a93b30e7c2e9cf7d08f124e95ab5c949c0) | 买家 A |

五笔买家交易的回执/参数/事件已独立核验。付款后买家 10、卖家 30、Escrow 60，allowance=0；
卖家原有的 30 个应与本次收入区分。原始 Remix 截图在另一对话，尚未导入本地仓库。

成员 2 可直接照 [卖家争议操作说明](docs/成员2卖家争议操作说明.md) 执行 dispute(1)。

正式交易应附区块浏览器中的交易页；本地哈希不能填入本表充数。

## 分工（成员 2、3 的具体姓名和领取任务仍需组内确认）

| 成员 | 章节 | 文件/工作 |
|---|---|---|
| 魏琪霖（成员 1） | 4.2、4.3、4.5 Service；后端及买家操作 | contracts/、EscrowService.java、Web3jConfig.java、application.properties、测试及运行脚本 |
| 成员 2 | 4.5 Controller；4.6 REST 验收及卖家操作 | 已实现 EscrowController.java，参数校验、异常映射，接入事件列表，配合 curl 记录 |
| 成员 3 | 4.6 买卖家操作、7 提交整理 | 配合两位真实钱包持有人完成 Remix 操作，收集截图与交易记录，汇总报告 |

开发分工与链上角色独立。路线 A 的仲裁人是后端账户 BE，不默认是成员 3 的个人钱包。
协作步骤见 [docs/协作验收清单.md](docs/协作验收清单.md)。
当前买卖家分配、新合约地址、可复制的操作参数见 [docs/当前协作交接.md](docs/当前协作交接.md)。

## AI 使用说明（准备阶段）

使用 Codex 辅助整理任务、实现 Service、编写测试与文档、运行命令并保存真实终端截图。
排错记录：首次测试依赖下载出现 TLS 错误；缩减为实际需要的 JUnit 依赖后，
使用 TLS 1.2 参数完成依赖下载，保持证书校验开启。首次复用 Hardhat 安装触发 HH12，
检查本机版本源码后使用其支持的非本地安装开关，并采用独立空账户配置。
正式报告需由各成员补充自己的使用情况，并结合截图解释代码；测试/截图不代表个人已掌握。

## 提交前

等待卖家争议时可先整理 [报告草稿](docs/报告草稿.md)、[解释题与答辩准备](docs/成员1解释题准备.md)
及 [28张已有截图索引](docs/截图索引.md)。已验证数据与待补项均已区分，不能作为全流程已完成报告直接提交。

运行 `python3 scripts/check-secrets.py`；确认没有运行时密钥或 .env 入库。
协作仓库：https://github.com/x12w/block-chain-lab 。报告模板和截止日期仍需按教师通知补齐。
