# Ponder Legacy 项目路线图

最后更新：2026-07-16

本路线图用于记录 Ponder Legacy 的当前完成度、已知缺口、版本目标和发布门槛。
它描述的是项目计划，不表示列出的功能已经完成。实际兼容声明仍以
[TESTING.md](TESTING.md) 中绑定具体成品 SHA-256 的验证结果为准。

## 项目方向

Ponder Legacy 面向 Minecraft 1.12.2、Forge 和 Java 8，Forge Mod ID 为
`ponder_legacy`。内容、资源、场景、命令和 ZenScript 命名空间继续使用
`ponder`。

项目采用两条并行开发线：

- A 线：稳定性、同步协议、结构安全、标准 Forge 真实客户端和 CatServer
  实验性回归。
- B 线：ZenScript 作者能力、诊断工具、扩展 API 和后续 JSON 前端。

两条开发线可以同时推进，但任何稳定版发布都必须先满足 A 线发布门槛。

## 当前完成度

### 已较成熟

- 播放器时间轴、关键帧、暂停、跳转、重放和场景状态恢复。
- 世界区段显示、隐藏、移动、旋转、合并、锚点和稳定中心。
- 方块、TileEntity、物品实体、矿车、鹦鹉、粒子和基础 Overlay。
- 组件索引、标签索引、物品悬停和默认 `W` 打开场景。
- Java `PonderPlugin`、ServiceLoader、IMC、标签、共享文本和索引排除 API。
- CraftTweaker/ZenScript 场景 IR、八个首次生成的内置脚本和外部结构目录。
- 服务器场景快照、服务器 Scene ID 优先和断线恢复本地场景。
- Java 8 打包、API/source/example addon 产物和标准 Forge 专服 CI。

### P0：发布阻塞和高风险缺口

- 标准 Forge 真实客户端尚未完成 GUI Scale 1-4、真实键鼠、全屏切换、
  资源重载和硬件 OpenGL 最终验收。

### P1：完成度偏低

- `ScriptInstructionCodec` 只有基础扩展接口，缺少协议版本、能力描述、示例
  addon 和端到端同步测试。

### P2：尚未提供

- `/ponder list`、`inspect`、`validate` 和 `sync status` 诊断命令。
- 场景来源、本地/服务器覆盖、缺失结构和协议拒绝原因的统一诊断模型。
- 错误示例和可直接安装的 ZenScript 示例脚本包。
- IR 导出、指令序号查看和开发模式时间轴定位工具。
- Java API 签名快照、二进制兼容检查和正式弃用周期。
- 网络同步进度和失败原因的客户端界面反馈。
- JSON 场景前端、离线校验器和可视化编辑工具。

## 1.1.1：稳定性与 ZenScript 补全

本版本作为 1.1.x 的稳定性、同步协议与 ZenScript 作者能力基线。历史构建和
验收结果只适用于对应的 1.1.1 成品，不转移到后续版本。

### 已完成的 A 线

- 将同步协议升级为版本化状态机，覆盖能力应答、开始、分块、完成、
  接受或拒绝、超时和断线清理。
- 解码阶段重新校验单场景、单指令、NBT、文本、资源 ID 和总快照限制。
- 所有服务器场景先进入临时注册表，验证和结构检查全部通过后原子切换；
  失败时保留原有本地层和服务器层。
- 建立真实 CraftTweaker 集成 fixture，直接编译和注册八个生成脚本。
- 在 ZS 测试可替代旧测试后，删除 `VanillaPonderScenes` 及其错误目标测试。
- 修正 IMC 目标文档、内置脚本显式恢复语义和 Windows 结构路径测试。

### 已完成的 B 线

- 补齐 `Selection.column`、`layers`、`cuboid`。
- 为常用 Scene、World、Overlay 和 Effects 方法增加 `Position`、`Vector`
  重载，同时保留现有坐标参数 API。
- 增加共享文本、参数化文本、独立文本、带文本轮廓、AABB 和 ValueBox
  类 Overlay 指令。
- 为物品实体增加受类型检查的移动、隐藏和移除指令。
- 所有新增能力继续生成确定性、可序列化 IR，不开放播放期间 ZenScript
  callback。

### 发布前剩余

- 最终版本提交通过 `clean test build compileClientHarnessJava`、发布内容检查、
  真实 CraftTweaker fixture 和标准 Forge 专服启动/重启。
- 对同一 SHA-256 成品完成标准 Forge 真实客户端八场景、GUI Scale 1-4、
  真实键鼠、全屏和资源重载验收。

## 1.1.2：通用 Provider/Subject SPI 与 MMCE 支撑

- 提供不依赖特定内容模组的 Provider/Subject SPI：`PonderStructureProvider`
  按结构 ID 提供 NBT、指纹、命名组和诊断，`ItemSubjectResolver` 把
  `ItemStack` 解析为 Ponder 场景 component ID。
- 明确 Provider/Resolver 注册、优先级、同 ID 替换、PASS/NOT_FOUND、缓存失效、
  错误隔离和无客户端环境下的生命周期约束。
- 保留现有 Java `PonderPlugin`、ServiceLoader、IMC、ZenScript 注册和普通物品
  场景入口；Provider/Subject SPI 是增量扩展面，不替代已有 API。
- 增加 Provider/Subject 的单元测试、示例接入和发布内容检查，保证第三方 addon
  不需要访问 Ponder 内部注册表。
- 引入独立 `ponder-mmce` 子项目，使用通用 SPI 提供 MMCE 动态结构，并把机器
  代表物品解析到对应 Ponder component；Ponder 主模组不直接依赖 MMCE，未安装
  addon 时行为保持不变。
- 主项目与 `ponder-mmce` 分别构建 jar、测试和计算 SHA-256；MMCE addon 的兼容
  声明必须绑定同一次 CI 构建以及明确的 Ponder/MMCE 版本组合。

## 1.1.3：作者工具与诊断

- 增加 `/ponder list [local|server|effective]`。
- 增加 `/ponder inspect <scene>`、`/ponder validate` 和
  `/ponder sync status`。
- 场景来源统一标记为 `JAVA_PLUGIN`、`BUILTIN_ZS`、`LOCAL_ZS` 或
  `SERVER_SNAPSHOT`，并记录覆盖关系。
- 错误统一包含来源文件、Scene ID、指令序号、结构路径和参数原因。
- 客户端只显示一次错误摘要，完整诊断保留在日志和验证报告中。
- 增加开发用 IR 导出和指令时间轴报告，不改变生产同步格式。
- 发布完整 ZenScript API 参考、外部结构示例、错误示例和可安装示例包。

## Ponder-MMCE 路线

Ponder-MMCE 是独立发布的附属模组，位于 `ponder-mmce` 子项目。它只通过 Ponder
公开 SPI 接入，不把 MMCE 专用逻辑放入 Ponder 主模组。

### Ponder-MMCE 0.1：基础桥接（当前 `0.1.0-alpha`）

- 解析 MMCE 已注册机器，为静态/动态结构请求建立稳定、可重载的结构 ID。
- 将机器控制器/代表物品解析为已有 Ponder component，处理缺失场景、重复注册和
  可选 MMCE 依赖。
- 建立 Java 8 单元测试、独立 jar、发布元数据、CI 上传和 SHA-256 清单。

### Ponder-MMCE 0.2：机器定义与变体

- 支撑机器结构、配方或配置变体对应的 Subject 选择和场景覆盖规则。
- 跟随 MMCE 配置/注册表重载刷新 Provider 缓存，并隔离单台机器定义错误。
- 增加整合包侧覆盖入口、示例机器和 Ponder/MMCE 兼容矩阵。

### Ponder-MMCE 0.3：扩展与诊断

- 向其他 MMCE 附属模组开放稳定扩展点，允许补充 Subject 元数据、场景选择和
  自定义显示组件。
- 与 Ponder 1.1.3 作者诊断对接，报告 Provider 来源、机器 ID、场景覆盖和拒绝
  原因。
- 建立公开 API 签名快照、弃用周期、跨版本回归和可重复发布流程。

## 1.2.0：扩展生态与兼容保障

- 为 `ScriptInstructionCodec` 增加稳定协议版本和能力描述，新增接口使用
  Java 8 default method 保持现有实现的二进制兼容。
- 示例 addon 同时演示 Java 场景、ServiceLoader、IMC、自定义 codec、
  快照同步和未知 codec 拒绝。
- 建立公开 API 签名快照和破坏性变更检查。
- 正式弃用 API 至少跨一个次版本保留，再允许删除。
- 完善服务器自定义标签、场景来源展示和整合包结构依赖清单。

## 1.3.0：多前端与可视化开发

- 在同一不可变 IR 上增加可选 JSON 场景前端。
- 提供 JSON Schema、离线校验器、格式迁移器和场景包清单。
- JSON 与 ZenScript 生成相同的验证结果和播放行为。
- 可视化编辑器只负责生成 JSON、ZS 或 IR 预览，不成为运行时依赖。
- 第一阶段优先提供结构、时间轴、选择器和基础 Overlay 编辑；复杂扩展
  codec 继续通过文本或 Java addon 配置。

## 测试与发布门槛

每次提交默认由 GitHub Actions 执行：

```text
clean test build compileClientHarnessJava :ponder-mmce:test :ponder-mmce:build
```

并执行发布内容检查、Java 8 字节码检查和标准 Forge 专服启动及重启。

新增测试至少覆盖：

- ZenScript：八个内置场景、循环、条件、函数、NBT、重复 ID、未注册
  builder、单文件错误隔离和专服安全加载。
- 网络：服务器覆盖、断线恢复、乱序、重复、缺失、超时、重叠传输、
  哈希错误、未知 codec、超限数据和多客户端。
- 结构：外部文件、资源包、jar 优先级、文件变化、SHA-256、路径逃逸、
  符号链接和缺失结构隔离。
- API：公开签名变化、示例 addon 编译、ServiceLoader、IMC 和 codec
  协议兼容。
- Provider/Subject：注册顺序、稳定 ID、PASS/NOT_FOUND、同 ID 替换、刷新失效、
  错误隔离、服务端安全和 addon 缺失。
- Ponder-MMCE：静态/动态结构解析、物品到 component 映射、可选依赖、重载和
  独立 jar 内容。

稳定发布候选还必须满足：

- 所有报告绑定同一 Ponder 成品 SHA-256。
- 标准 Forge 真实客户端完成八个场景、GUI Scale 1-4、全屏、资源重载、
  真实鼠标和按键验收。
- CatServer 客户端支持只作为实验线，不阻塞 1.1.2；若要额外声明兼容，
  仍需真实连接、同步和场景播放证据。
- 测试数量、产物哈希和报告链接由 CI 生成，不再手工维护易漂移的静态值。

## 固定约束

- Forge Mod ID 保持 `ponder_legacy`，内容和脚本命名空间保持 `ponder`。
- CraftTweaker `4.1.20+` 为强制依赖。
- MixinBooter 运行范围保持 `9.1+`，默认认证基线为 `11.2`。
- 服务器只同步验证后的 IR，不同步 `.zs` 和结构 NBT。
- Java `PonderPlugin`、ServiceLoader 和 IMC API 保留。
- `1.1.x` 只维护 ZenScript 前端；JSON 从 `1.3.0` 开始。
- 编译和标准专服验证优先使用 GitHub Actions。
- GPU/OpenGL 和真实键鼠验收使用预配置 Windows 自托管环境或绑定成品
  哈希的人工报告。
