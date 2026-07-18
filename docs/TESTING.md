# Ponder 1.12.2 发布与运行验收

所有结果都绑定到一次具体构建的 SHA-256。重新构建会改变 manifest 时间戳和 jar 哈希，之前的
客户端、专服或 CatServer 结果不能自动转移到新 jar。服务端单独启动成功也不能代替客户端渲染验收。

## 固定环境

| 项目 | 要求 |
| --- | --- |
| Minecraft | 1.12.2 |
| Java | 64 位 Java 8，源码与 class major version 均为 52 |
| Forge 编译/最低运行基线 | 14.23.5.2847；声明兼容 2847 及以上 |
| MixinBooter | 运行范围 9.1 及以上；默认认证基线 11.2，SHA-256 `48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E` |
| CraftTweaker | 强制依赖 4.1.20 及以上；构建基线 4.1.20.698 |
| CatServer | 仅限 SHA-256 `EAF575310ACBB48D535212CFB88D93DE69F90F2A81879A26F88457713A25952E` |

MixinBooter 必须作为独立 jar 安装，不能嵌入 Ponder。每个要正式声明兼容的 MixinBooter 版本都应
单独执行本文件的服务端与客户端门槛。Forge、MixinBooter 和 CatServer 的普通签名
警告不是通过依据；任何 mixin 应用失败、缺类、客户端类在专服加载或模组生命周期异常都必须判失败。

## 自动化构建门槛

在项目根目录使用 Java 8：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
.\gradlew.bat clean test build compileClientHarnessJava :ponder-mmce:test :ponder-mmce:build --no-daemon --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\verify-release.ps1
```

`build` 已依赖 reobf 主包、reobf 示例 addon、API jar、sources jar 和发布内容检查。不要用
`jar` 任务的 `build/devlibs/*-dev.jar` 代替发布成品。成功后应存在：

- `build/libs/Ponder-1.12.2-1.2.0.jar`
- `build/libs/Ponder-Example-Addon-1.12.2-1.2.0.jar`
- `build/libs/Ponder-1.12.2-1.2.0-api.jar`
- `build/libs/Ponder-1.12.2-1.2.0-sources.jar`
- `build/distributions/Ponder-ZenScript-Examples-1.2.0.zip`
- `build/distributions/Ponder-Example-Addon-Smoke-1.2.0.zip`
- `ponder-mmce/build/libs/Ponder-MMCE-1.12.2-0.1.0-alpha.jar`
- `ponder-mmce/build/distributions/Ponder-MMCE-Smoke-Pack-0.1.0-alpha.zip`
- `ponder-mmce/build/verification/server-harness/Ponder-MMCE-Server-Harness-1.12.2-0.1.0-alpha.jar`
- `ponder-mmce/build/verification/client-harness/Ponder-MMCE-Client-Harness-1.12.2-0.1.0-alpha.jar`

Gradle 门槛检查以下内容：

- 主包、示例包和 API 包内全部 class 为 Java 8 major 52；
- 主包有有效 `mixins.ponder.refmap.json`，四个必要客户端 mixin 都有 SRG 映射；
- manifest 使用 `PonderMixinLoader`、`ForceLoadAsMod=true` 和 `FMLCorePluginContainsFMLMod=true`；
- 主包和示例包抽样类不含 MCP 字段/方法名，证明发布包经过 reobf；
- 不嵌入 MixinBooter、CraftTweaker、ZenScript、Mixin、MixinExtras、Cleanroom、Flywheel、JOML、GLFW、Fabric 或 NeoForge；
- 不把示例 addon 或 `assets/ponder/ponder/debug` 九个开发结构打进主包；
- 主包必须包含 `assets/ponder/ponder/demo` 下八个正式演示结构，并按发布清单逐项检查；
- 主包必须包含 `assets/ponder/scripts/builtin` 下八个内置 ZenScript，并按发布清单逐项检查；
- API classifier 只包含 Ponder API、Catnip 适配 API 及明确公开的入口类；
- 1.2.0 精确 API 签名与仓库快照一致，相对 1.1.3 基线不存在二进制破坏；
- API 门禁 fixture 放行新增类型、重载、default/static 方法，拒绝删除、改描述符、改继承和新增抽象成员；
- Ponder-MMCE 子项目测试和构建通过，运行 jar 内 class 为 Java 8 major 52。

`verify-release.ps1` 会再次遍历主包、API、sources 和示例包：对两个可运行成品解析 class 常量池，
检查 Minecraft 成员是否仍是 MCP 名；API classifier 是供开发编译的反混淆包，不作为运行 jar 检查。
脚本还验证 sources 不含 class 或开发 debug NBT，主包包含八个正式演示结构和八个内置 ZenScript，
并复核 refmap、manifest、禁用引用、语言数和 `pack_format=3`。它把四个 Ponder 成品、
Ponder-MMCE 运行 jar 与 CatServer 的 SHA-256 写入
`build/reports/release-verification.md`。单元测试报告位于 `build/reports/tests/test/index.html`。
Ponder 的作者诊断报告和导出文件则写入 `logs/ponder/diagnostics`。

## 1.2.0 当前发布记录

当前 1.2.0 成品的 SHA-256 不在本文硬编码。请到 GitHub Actions 的 build job summary
和上传的 Ponder/Ponder-MMCE artifact bundle 中读取同一次构建的成品哈希。

同一次构建应同时产出：

- `Ponder-1.12.2-1.2.0.jar`
- `Ponder-Example-Addon-1.12.2-1.2.0.jar`
- `Ponder-1.12.2-1.2.0-api.jar`
- `Ponder-1.12.2-1.2.0-sources.jar`
- `Ponder-ZenScript-Examples-1.2.0.zip`
- `Ponder-Example-Addon-Smoke-1.2.0.zip`
- `Ponder-MMCE-1.12.2-0.1.0-alpha.jar`

这些成品、`build/reports/release-verification.md`、主项目与 Ponder-MMCE 测试报告
以及标准 Forge 专服报告必须指向同一次 1.2.0 构建。CatServer 服务端回归报告也应记录对应成品，
但 CatServer 客户端支持只算实验线，不阻塞发布。标准 Forge 真实客户端仍是 1.2.0 的发布门槛。

Ponder-MMCE 验收还必须覆盖静态/动态结构解析、稳定结构 ID 与指纹、物品到 component 映射、
PASS/NOT_FOUND、重复注册、刷新失效、单机错误隔离和未安装 addon/MMCE 时的可选依赖边界。
任何兼容声明都必须写明实际测试的 Ponder、Ponder-MMCE 与 MMCE 版本组合。

1.2.0 还必须核对协议 v3：codec ID、精确版本、能力要求、服务器 ZS 标签/共享文本、结构依赖清单和
三层事务回滚。Example Addon Smoke Pack 用于验证有 codec 客户端正常同步，以及无 codec 客户端在
Begin/Chunk 前收到兼容拒绝。API 签名报告位于 `build/reports/api`。

真实 MMCE 专服 fixture 使用同一个可安装 Smoke Pack：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\test-ponder-mmce-forge.ps1 `
  -JavaExecutable "$env:JAVA_HOME\bin\java.exe"
```

脚本第一轮启动加载三台 MMCE 机器、两个作者场景、命名组、preview NBT 和蓝图 Resolver。
随后冻结静态结构 ID、修改本地机器配置并再次启动；Provider 必须明确拒绝旧指纹，动态场景必须保留，
且日志必须包含明确的 `fingerprint mismatch`。第三轮模拟 MMCE ABI 不兼容，附属必须停止注册
Provider/Resolver，Ponder 与专服仍正常启动。客户端 harness 另行确认旧指纹拒绝不会移除其他有效场景。
报告写入
`build/reports/ponder-mmce-forge-verification-*.md`。

客户端安装 Smoke Pack、运行独立 Ponder-MMCE client harness 后，按照
[PONDER-MMCE-ALPHA-ACCEPTANCE.md](PONDER-MMCE-ALPHA-ACCEPTANCE.md) 完成人工项。
最终验收 JSON 必须由 `tools/complete-ponder-mmce-client-acceptance.ps1` 生成；Alpha 发布工作流会
重新核对其中的 Ponder 与 Ponder-MMCE SHA-256。

1.1.2 Alpha/MMCE 发布与验收记录是冻结线，只绑定旧的 1.1.2 成品、旧的报告和旧的验收 JSON，
不随着 1.2.0 的主线构建刷新。

## 历史记录

### 1.1.0 历史结果

`1.1.0` 新增 CraftTweaker/ZenScript 场景 IR、首次生成脚本、外部结构加载和服务器快照同步。
历史版本的哈希和报告不转移到当前版本；以下结果仅作为 1.1.0 的历史记录：

| 门槛 | 状态 | 证据或原因 |
| --- | --- | --- |
| 单元测试 | PASS | 40 个测试套件，共 121 项；0 failures、0 errors、0 skipped |
| 完整构建与发布报告 | PASS | `clean test build compileClientHarnessJava` 通过；默认 MixinBooter 11.2 + CraftTweaker 4.1.20.698 发布报告为 `PASS` |
| RFG reobf 专服脚本预检 | PASS | 首次生成 8 个 ZS；CraftTweaker 在 218 ms 内无错误编译并注册全部场景；Forge 加载 8 个 storyboard、生成世界后使用 `stop` 正常保存退出 |
| 标准 Forge 2847 专服 | PASS | 空服对照、Ponder + CraftTweaker 首启、同世界重启和专服类审计通过；报告 `standard-forge-verification-20260712-182019243-1e247d13.md` |
| RFG/验收 harness 客户端 | 编译通过，运行受阻 | harness 已覆盖四阶段逐块截图、70 tick 书本 Y 采样及八场景自动播放；宿主机仍报 `LWJGLException: Pixel format not accelerated`，未取得运行时深度读回或截图 |
| 标准 Forge reobf 客户端视觉门槛 | 环境阻塞/未执行 | 仍须在可用 OpenGL 客户端验证八个场景、真实输入、全屏、资源重载和 GUI scale 1-4 |
| CatServer 四层服务端预检 | PASS_SERVER_ONLY | 指定 SHA-256 的 CatServer 完成空服、MixinBooter、Ponder + CraftTweaker、示例 addon 和重启；报告 `catserver-verification-20260712-182119289-92d095bb.md` |

当前完整客户端验收仍未满足：标准 Forge 客户端画面/交互没有证据。CatServer 真实客户端连接未执行，
因此 `PASS_SERVER_ONLY` 仍不能改写成客户端通过。

本次最终 1.1.0 成品如下；任何后续重建都会改变哈希：

| 文件 | SHA-256 |
| --- | --- |
| `Ponder-1.12.2-1.1.0.jar` | `C76797E091DB2EE0E4157D4FD7D358D3482A5EE4B676FFAF9FFC6B5DF27C8E1F` |
| `Ponder-1.12.2-1.1.0-api.jar` | `3F00CC619A3D91FDE2CBF9DB59FFBF576802160FF54DA6827202C5F8565F9FC1` |
| `Ponder-1.12.2-1.1.0-sources.jar` | `47B3FB4FCE02CD708B0606DD02E7CF19493A88653EE553B5E50A6538767AA06C` |
| `Ponder-Example-Addon-1.12.2-1.1.0.jar` | `435E46A90804DF3406CBA1D328112898B6BEB7C0AFC5CABDC97F2A94422797D0` |

### 1.0.2 历史结果

最近一次完整服务端验收属于 1.0.2，仅作为历史记录：

| 文件 | SHA-256 |
| --- | --- |
| `Ponder-1.12.2-1.0.2.jar` | `28C787F41B99BC469893A43EEA107EF98AD48CC36CAEC2E2DA3C7368B0A94EF2` |
| `Ponder-1.12.2-1.0.2-api.jar` | `22FCC512A0622991285BC1DE193F4FC71A40C1CCB22F005B503089213A0F8749` |
| `Ponder-1.12.2-1.0.2-sources.jar` | `80D9373829D1281910F2201A1560B10A62C4B75638238ADE294F117B908B9CB2` |
| `Ponder-Example-Addon-1.12.2-1.0.2.jar` | `90C13EC3D0B3152C3C7AE529069EC1A9778213BC1F87CED33184A936F69C728C` |
| `mixinbooter-11.2.jar` | `48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E` |

## 标准 Forge 专服

使用 Mojang/Forge 安装器建立隔离的 14.23.5.2847 专服，只安装最终 reobf Ponder、CraftTweaker 与选定的外置
MixinBooter。脚本默认使用 11.2，也可通过 `-MixinBooterVersion` 和可选
`-ExpectedMixinBooterHash` 指定其他版本。至少执行两次启动：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\test-standard-forge.ps1 `
  -TimeoutSeconds 600
```

1. 首次启动生成世界，到达 `Done` 后执行 `save-all`，确认保存完成，再执行 `stop`。
2. 复用同一世界重启，再次到达 `Done`，保存并正常关闭。
3. 检查 `level.dat`，并扫描完整日志中的 `Mixin apply failed`、`InvalidMixinException`、
   `InjectionError`、`NoClassDefFoundError`、`ClassNotFoundException` 和 FML fatal。
4. 使用类加载跟踪或等价证据确认没有加载 Ponder/Catnip 的 `client`、`render`、`gui`、
   `outliner`、`ghostblock` 和 `foundation.ui` 客户端类。若 Forge 自身动态定义原版客户端匿名
   内部类，必须和相同 Forge 空服基线比较；不允许新增集合、原版客户端外层类或其他客户端类。

仅 `runServer` 开发启动不能代替 reobf 成品在隔离标准 Forge 专服中的结果。
使用 `-verbose:class` 时，`[Loaded java.lang.ClassNotFoundException ...]` 或加载 mixin 异常类型本身
不是异常被抛出；fatal 扫描必须匹配真实日志事件/堆栈，不能把类定义加载行当作失败。

本次模组服与空 Forge 基线的类跟踪都只出现同一组 34 个 Forge 注解缓存动态定义的原版客户端匿名
内部类，集合差异为 0；Ponder/Catnip 客户端包加载数为 0。报告位于
`build/reports/standard-forge-verification-20260712-042042984-4e31850b.md`，空服基线、两次模组服、
类列表和 `level.dat` 快照均位于
`build/standard-forge-smoke/20260712-042042984-4e31850b`。两次模组服日志均记录 1 个插件、8 个
storyboard、`Done`、`Saved the world` 和正常关闭，且致命 mixin/缺类日志为 0。

## 标准 Forge 客户端

客户端必须使用 Java 8、Forge 2847+、外置 MixinBooter 和同一 SHA-256 的 Ponder 主包。
当前完整验收基线使用 11.2；其他允许版本需要分别保留对应客户端证据。
八个内置入口为 `minecraft:crafting_table`、`minecraft:chest`、`minecraft:furnace`、
`minecraft:piston`、`minecraft:redstone_lamp`、`minecraft:glass`、`minecraft:water_bucket` 和
`minecraft:rail`。示例 addon 的场景还需安装同一构建生成的 reobf 示例包。

逐项截图或录屏，并在客户端日志中保留资源重载与退出过程：

| 检查项 | 操作与通过条件 |
| --- | --- |
| 四种方块层 | 独立 `render_layers` 场景以 stone 验证 SOLID、普通 glass 验证 CUTOUT、grass 验证 CUTOUT_MIPPED、stained glass 验证 TRANSLUCENT；基础场景另以 water 验证 TRANSLUCENT。不得笼统宣称普通玻璃使用透明混合层，四层都应显示正确，移动 section 后新暴露面也必须存在 |
| 结构与流体 | `demo/basics.nbt` 必须从资源加载；玻璃透明，水面方向正确，未知状态不得静默变空气 |
| TileEntity 与实体 | 箱子 TESR、结构中的盔甲架及脚本创建的物品实体可见，位置和旋转随场景正确更新 |
| 粒子与破坏纹理 | 普通/发光粒子可见且重播后状态一致；破坏阶段纹理处于正确 section 变换下 |
| 独立 section | 显示/隐藏、淡入、平移、三轴旋转、中心、稳定锚、暴露面与透明排序正确；无跳变或重复 anchor 偏移 |
| 命中与识别 | `Q` 或识别按钮进入识别模式，旋转/移动后的最近方块命中正确，提示 ItemStack/方块和命中面方向正确 |
| 播放控制 | 空格和按钮暂停/继续，`R` 和按钮重播，拖动进度条及 keyframe 前后跳转得到一致世界、实体、元素和动画状态 |
| 视角与导航 | 鼠标拖动旋转、滚轮缩放、左右场景切换、索引/标签/返回/关闭均可用，按钮文字和提示不越界 |
| 物品入口 | 在容器悬停已注册物品并按住前进键可以打开场景，未注册物品不应打开 |
| 资源与窗口 | 执行资源重载后所有 section 缓存重建；F11 全屏往返后投影和射线仍一致 |
| GUI scale | 分别使用 1、2、3、4，确认标题、进度条、按钮、提示与场景不重叠、不被裁切 |
| GL 状态 | 关闭 Ponder 后普通世界、物品、GUI、粒子和 TESR 渲染正常；无纹理、深度、混合、光照、矩阵或共享 Tessellator 污染 |
| 异常隔离 | 若第三方方块/TESR/实体 renderer 抛错，只跳过该对象并记录日志，后续对象和普通世界仍能渲染 |

推荐把截图保存在 `build/reports/client-screenshots`，文件名包含 GUI scale 和功能，例如
`scale-2-translucent-water.png`、`scale-4-keyframe-replay.png`。宿主机没有可用 OpenGL 驱动、窗口无法
创建或只能远程基本显示时，结果应记为“环境阻塞/未执行”，不能记为通过。
Mesa/llvmpipe 仅是当前无硬件 OpenGL 驱动宿主机的验收手段，不是 Ponder 的运行前置，也不得进入发布包。

## CatServer 实验性分层验收

先确保项目上一级目录中的 CatServer jar 哈希与固定值一致，再对最终发布 jar 执行服务端四层预检：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\test-catserver.ps1 `
  -CatServerJar ..\CatServer-4168d848-universal.jar `
  -LibrariesDirectory .\verification\catserver-runtime-libraries `
  -TimeoutSeconds 180
```

脚本按隔离目录依次启动：空服、仅 MixinBooter、MixinBooter + Ponder、再加 reobf 示例 addon。
上述命令不等待客户端，最多只能得到 `PASS_SERVER_ONLY`。

若要额外声明 CatServer 客户端兼容，只能在具有可用桌面和 OpenGL 的机器上执行相同命令并追加
`-WaitForClient`。这项客户端兼容证据不阻塞 1.2.0 的标准 Forge 发布。
最后一层监听 `127.0.0.1:25567`，必须用真实 1.12.2 客户端连接，打开内置演示并完成上述视觉/交互
检查。完成后创建脚本打印出的唯一 `client-demo-ok.flag` 路径；不要预先创建或复用旧标记。脚本随后
验证保存、关闭、同一世界重启、mixin 致命错误和专服客户端类加载。

不带 `-WaitForClient` 的运行最多生成 `PASS_SERVER_ONLY`；这是本轮要求的服务端回归结果，不是客户端
画面证据。若执行完整 CatServer 客户端门槛，报告必须在
`build/reports/catserver-verification-<run-id>.md` 中显示 `Status: PASS`、
`Release gate satisfied: True`、真实客户端已观察、演示已确认，并保留报告列出的唯一证据目录。

本次服务端预检报告为
`build/reports/catserver-verification-20260712-042228873-f2d8f415.md`：四层均为 `PASS`，最后一层
示例 addon 首次启动和同世界重启均完成保存与正常关闭，汇总状态为 `PASS_SERVER_ONLY`，因此
`Release gate satisfied` 正确保持为 `False`。

## 最终交付证据

- `build/reports/tests/test/index.html`
- `build/reports/release-verification.md`
- 标准 Forge 报告 `build/reports/standard-forge-verification-20260712-042042984-4e31850b.md`
- 标准 Forge 空服基线、初启/重启日志、类列表和 `level.dat`：
  `build/standard-forge-smoke/20260712-042042984-4e31850b`
- 标准 Forge 客户端 `latest.log`、退出日志和各 GUI scale 截图（当前未取得）
- CatServer 四层报告 `build/reports/catserver-verification-20260712-042228873-f2d8f415.md` 及证据目录
  `build/catserver-smoke/20260712-042228873-f2d8f415`
- 主包、API、sources、示例包和指定 CatServer 的最终 SHA-256 清单

以下任一情况阻断对应验收：required mixin 失败、超出空 Forge 基线的原版客户端类或任何
Ponder/Catnip 客户端包在专服加载、非 Java 8 class、refmap 无效、主包或示例包未 reobf、禁用依赖/
调试 NBT 被打包、场景回放不一致、section 射线与渲染矩阵不一致或 GL 状态污染。完整客户端验收还
必须取得标准 Forge 真实客户端证据；若要声明 CatServer 客户端兼容，则另需真实连接并人工确认演示。
