# 区块链大作业：路线 A，成员 1 交付

成员 1：魏琪霖，学号 202430332012，第 15 组。日期：2026-11-08。

本仓库完成合约 Java 封装、EscrowService、环境变量配置和本地自动化验证。
EscrowController 已由成员 2 接入，合并后联调通过；已在 POTOS 部署新 Escrow 并完成
SimpleStorage 验证。买家两笔创建、授权60个、两笔付款和卖家dispute(1)均已独立核验。
正式GET、成员3仲裁及买家confirm(0)均已完成并独立核验：订单0=Completed、订单1=Refunded，最终余额40/60/0。
原实验项目不受影响。本项目基于课程 spring-boot-web3j 示例，合约复制自组长实验 3 源码。

## 本次新增的三个加分项

最新进度（2026-11-08 01:41，北京时间）：增强订单1=Completed、订单2=Refunded、订单3=Refunded；买家/卖家/增强合约余额39/61/0，allowance=0。两位仲裁先意见冲突，第二仲裁改票后退款，最后四笔交易均已独立核对回执、参数、事件与转账方向。订单0过期未付款，保留Created。无需继续签名或重复交易。独立记录见`evidence/bonus/potos/final-settlement-verification.json`和[当前协作交接](docs/当前协作交接.md)。

订单时间线、卖家发货状态、双人仲裁均已实现，已有本地EVM、真实HTTP及POTOS链上验证。订单1验证成功发货与确认；订单2验证双人投票；订单3验证未发货超期退款。订单2实际先争议、后发货失败，不能写成“发货后争议成功”；订单1超期退款被拒绝仅为模拟，没有失败交易哈希。三项各3分，实际得分由教师评定。

增强合约已于2026-11-07部署至POTOS：`0x917Dc833eadBa6544F1fd80498715da39Ac739f4`，部署块`2829547`。第一仲裁人为袁嘉骏的BE，第二仲裁人为魏琪霖现有买家钱包；链上回执、构造参数、两位仲裁地址和version=2均已独立核对。后端11月8日已恢复并加载该合约；以后重启只需重新加载，不再部署。记录见`evidence/bonus/potos/`。

历史异常：首次订单0的付款比截止时间晚95秒上链而失败，无代币转账。原15分钟期限不足以覆盖人工签名往返，替代订单1、2改为2小时期限，订单3为1小时，现均已结算。详见`evidence/bonus/potos/expired-order0-verification.json`。用户提供的两个浏览器ZIP现已完整归档：104个源文件、38张图片，最终包58项原始摘要全部匹配。Word已补入最终测试网证据、实际仲裁人身份和操作偏差；本次最终交付包取代此前阶段包，贡献比例与成员个人AI说明仍需小组确认。详见`evidence/bonus/browser-originals/import-verification.json`。

- [实现、接口与验收说明](docs/路线A加分项说明.md)
- [测试网协作步骤](docs/加分项测试网操作说明.md)
- [24项测试与源码摘要](evidence/bonus/enhanced/summary.json)
- [运行后端的真实时间线响应](evidence/bonus/timeline/running-backend-acceptance.json)

增强合约 `src/main/resources/contracts/EscrowEnhanced.sol` 已独立部署。它保留原状态值，追加Shipped；两位不同仲裁人意见一致后才结算。原必做合约和已完成的两笔订单继续保留，查询原记录使用 `/load`，查询增强合约使用 `/load-enhanced`。

## 已完成

- 生成 Escrow、IERC20、WQLToken、SimpleStorage 的 Java 封装。
- EscrowService 支持部署、加载、查询地址/仲裁人、订单详情、事件订单列表、仲裁。
- OrderCreated 按闭区间每批最多 10 个区块查询，RPC 失败会报错，不伪装为空列表。
- 以文字返回订单状态；仲裁的角色与状态检查由合约执行；失败回执保留给 Controller。
- 私钥由 WEB3J_PRIVATE_KEY 注入，配置类无硬编码后备私钥，链 ID 默认 60600。
- 12 个单元测试和 2 个本地链集成测试已通过；其中 HTTP 集成测试实际启动 Spring 服务，
  验证 25 次请求、六个接口、参数转换、失败回执、退款、结算和重新加载。
  本次合并联调证据见 [evidence/pr1-integration](evidence/pr1-integration)，早期成员 1 材料见 [evidence/member1](evidence/member1)。
- 加分项扩展后重新执行全部验证：19个单元测试、5个本地集成测试通过，`bootJar`构建成功。
  增强验证覆盖卖家权限、截止时间边界、已发货退款限制、双票冲突与修改、重复投票、成功退款及放款、错误加载不改变绑定。

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
`EscrowController.java` 中实现，并有本地单元测试覆盖状态码与失败回执；正式测试网
curl 验收及 4A-4～4A-8 截图已保存。接口对接见 [docs/成员2接口对接.md](docs/成员2接口对接.md)。

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
加分项验证执行 `bash gradlew test integrationTest bootJar`，再运行
`python3 scripts/summarize-bonus.py`，归档全部六个测试类、增强合约回执和源码摘要。
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
| dispute 订单 1 | 0x38f57adcfd17adba8849eb8e66985bbefb38000ffe221b8ac1105cd991a90494 | [交易详情](https://scan-testnet.potos.hk/tx/0x38f57adcfd17adba8849eb8e66985bbefb38000ffe221b8ac1105cd991a90494) | 卖家 B／张俊仁 |
| resolve 订单 1 | 0x41346bdd3a5ce73aa5bf86cb2436fd0c149338f94d1a00cf23d161313e130a7f | [交易详情](https://scan-testnet.potos.hk/tx/0x41346bdd3a5ce73aa5bf86cb2436fd0c149338f94d1a00cf23d161313e130a7f) | 后端 BE／袁嘉骏操作 |
| confirm 订单 0 | 0x44db2d985fee8be415c46c35a8422840cb463691aeef9f95d5f12b66ca23ea20 | [交易详情](https://scan-testnet.potos.hk/tx/0x44db2d985fee8be415c46c35a8422840cb463691aeef9f95d5f12b66ca23ea20) | 买家 A |
| resolve 订单 0 失败 | 0x6b6157111ef72bcacf69ab031d15ccaebd11cde978fa73637eebde039117f4c3 | [交易详情](https://scan-testnet.potos.hk/tx/0x6b6157111ef72bcacf69ab031d15ccaebd11cde978fa73637eebde039117f4c3) | 后端 BE／袁嘉骏操作 |

额外保存订单 1 的创建、付款交易：

| 操作 | 交易哈希/浏览器 | 发起账户 |
|---|---|---|
| createOrder 订单 1 | [0x6965468ab5104bf9c202d3f8c0e8c59b2fa6da24eb224c225c4b5b120d15ec6e](https://scan-testnet.potos.hk/tx/0x6965468ab5104bf9c202d3f8c0e8c59b2fa6da24eb224c225c4b5b120d15ec6e) | 买家 A |
| deposit 订单 1 | [0x24dc8f81f69ff0c3183fb79169d951a93b30e7c2e9cf7d08f124e95ab5c949c0](https://scan-testnet.potos.hk/tx/0x24dc8f81f69ff0c3183fb79169d951a93b30e7c2e9cf7d08f124e95ab5c949c0) | 买家 A |

五笔买家交易的回执/参数/事件已独立核验。付款后买家 10、卖家 30、Escrow 60，allowance=0；
卖家原有的 30 个应与本次收入区分。原始浏览器证据已从用户提供的 ZIP 校验导入 evidence/potos/buyer/，包含 9 张截图。

成员2卖家争议和成员3仲裁均已完成；原图分别见 evidence/potos/seller/ 与 evidence/potos/arbitration-member3/。失败/退款两笔回执均已核验，最终确认9张原图见 evidence/potos/final-confirmation/；[完成状态及提交检查](docs/当前协作交接.md)。

正式交易应附区块浏览器中的交易页；本地哈希不能填入本表充数。

## 分工

| 成员 | 章节 | 文件/工作 |
|---|---|---|
| 魏琪霖（成员 1） | 4.2、4.3、4.5 Service；后端及买家操作 | contracts/、EscrowService.java、Web3jConfig.java、application.properties、测试及运行脚本 |
| 张俊仁（成员 2） | 4.5 Controller；4.6 REST 验收及卖家操作 | 已实现 EscrowController.java，参数校验、异常映射，接入事件列表，配合 curl 记录 |
| 袁嘉骏（成员 3） | 4.6 仲裁验收、7 提交整理 | 负责仲裁环节及记录，由后端BE签名；收集截图、交易并整理报告 |

开发分工与链上角色独立。根据组长2026-11-07的最新确认，后端BE地址归袁嘉骏；增强合约第一票由其负责，第二票由魏琪霖使用现有买家钱包签署。本次课程演示中魏琪霖兼任买家和第二仲裁人，不能将两个角色描述为两个不同成员。
协作步骤见 [docs/协作验收清单.md](docs/协作验收清单.md)。
当前买卖家分配、新合约地址、可复制的操作参数见 [docs/当前协作交接.md](docs/当前协作交接.md)。

## AI 使用说明（准备阶段）

使用 Codex 辅助整理任务、实现 Service、编写测试与文档、运行命令并保存真实终端截图。
排错记录：首次测试依赖下载出现 TLS 错误；缩减为实际需要的 JUnit 依赖后，
使用 TLS 1.2 参数完成依赖下载，保持证书校验开启。首次复用 Hardhat 安装触发 HH12，
检查本机版本源码后使用其支持的非本地安装开关，并采用独立空账户配置。
正式报告需由各成员补充自己的使用情况，并结合截图解释代码；测试/截图不代表个人已掌握。

## 提交前

链上流程及最终余额已核验；提交前核对 [报告草稿](docs/报告草稿.md)、[解释题与答辩准备](docs/成员1解释题准备.md)
及 [98张已归档图片索引](docs/截图索引.md)。教师模板报告已补齐8行交易、六道解释题及截图；贡献比例和各成员AI说明仍需本人核对。

运行 `python3 scripts/check-secrets.py`；确认没有运行时密钥或 .env 入库。
协作仓库：https://github.com/x12w/block-chain-lab 。教师模板已收到；[Word报告](docs/大作业报告.docx)已填入最终结算证据，贡献比例按要求留空。截止日期待教师通知。
