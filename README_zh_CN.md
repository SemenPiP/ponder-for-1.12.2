# Ponder Minecraft 1.12.2 独立重写版

[![构建与验证](https://github.com/SemenPiP/ponder-for-1.12.2/actions/workflows/build.yml/badge.svg)](https://github.com/SemenPiP/ponder-for-1.12.2/actions/workflows/build.yml)

这是面向 Minecraft 1.12.2、Forge 和 Java 8 的 Ponder/Catnip 独立实现。
官方 1.21.1 项目仅作为功能、公开 API、翻译与视觉资源规格；项目没有复制或改造任何第三方
1.12.2 回移源码。

项目后续版本目标、当前低完善度功能和发布优先级见
[Ponder Legacy 项目路线图](docs/ROADMAP.md)。

源码实现和自动化检查已经建立，但某个 jar 只有在同一成品完成
[发布验收门槛](docs/TESTING.md) 后才能称为已验证版本。仅通过专服启动不代表客户端渲染或
CatServer 完整兼容已经通过。

## 当前验证状态

`1.3.0-alpha.1-mc1.12.2` 新增可热重载的 JSON 场景包、Draft-07 Schema、离线校验/迁移工具和可安装示例包。
JSON 与 ZenScript 会生成相同的确定性 IR，并继续使用协议 v3、codec 能力协商、服务端脚本元数据、
统一诊断、结构依赖清单和公开 API 签名门禁。
运行元数据接受 MixinBooter 9.1 及以上版本，并要求 CraftTweaker 4.1.20 及以上版本。
八个内置场景在首次启动时生成，全部保持精确 32 秒。MMCE 2.3.2+ 兼容层已经合入 Ponder
运行 jar，仅在检测到 Modular Machinery 时启用。当前发布为 Alpha：自动构建、打包检查和标准 Forge
专服证据属于发布门槛，真实客户端人工验收仍明确标记为待完成。成品 SHA-256 由 GitHub Actions
的 build job summary 和上传的 release artifact bundle 发布。

当前工作区无法提供可可靠判断 Minecraft 画面的桌面，也无法完成真实鼠标、全屏切换和 GUI scale 1-4 的
人工观感验收。因此，标准 Forge 客户端视觉验证仍是发布门槛。CatServer 客户端支持为实验性，不阻塞
1.3.0 发布线；服务端单独验证结果只记录在 [TESTING](docs/TESTING.md) 中，不能解释为客户端通过。

## 运行要求

- Forge 模组 ID：`ponder_legacy`
- 内容、资源和 ZenScript 命名空间：`ponder`
- Minecraft 1.12.2
- Forge 14.23.5.2847 或更新
- Java 8
- MixinBooter 9.1 或更新
- CraftTweaker 4.1.20 或更新

当客户端和服务端都安装 Ponder 时，必须使用完全相同的 Ponder 版本。安装了 Ponder 的客户端仍可连接到
未安装 Ponder 的普通服务器。运行时依赖不会打进主 jar。

不要把 `-api`、`-sources` 或 `-dev` jar 当作运行模组安装。`-api` jar 只供 addon 编译，
`-sources` 只供阅读，`build/devlibs` 下的 jar 只供反混淆开发环境使用。当前成品下载后如需核对版本，
请查看 GitHub Actions job summary 与 release artifact bundle 中记录的 1.3.0 Alpha 1 构建结果。

## 构建

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-1.8
gradlew.bat clean test build compileClientHarnessJava :ponder-mmce:test :ponder-mmce:build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\verify-release.ps1
```

开发客户端和服务端分别使用 `runClient`、`runServer`。构建使用 RetroFuturaGradle 1.4.9、
Gradle 8.14.3、MCP stable_39 和 Forge 14.23.5.2847。`build` 已包含测试和发布内容检查，
并输出 reobf 成品。任何兼容声明发布前，同一组成品还必须通过 [TESTING](docs/TESTING.md) 中的
标准 Forge 专服检查和真实标准 Forge 客户端门槛。CatServer 服务端回归继续保留，但属于实验性证据，
不阻塞 1.3.0 Alpha 1 发布。

`build/libs/Ponder-1.12.2-1.3.0-alpha.1.jar` 是 reobf 运行时成品。开发 jar 只放在 `build/devlibs`，
不得安装到正式服务器。`reobfExampleAddonJar` 会生成可单独安装的示例 addon：
`build/libs/Ponder-Example-Addon-1.12.2-1.3.0-alpha.1.jar`。

## 内置演示

可使用 `/ponder <component id>` 打开 `minecraft:crafting_table`、`minecraft:chest`、
`minecraft:furnace`、`minecraft:piston`、`minecraft:redstone_lamp`、`minecraft:glass`、
`minecraft:water_bucket` 和 `minecraft:rail`。`/ponder index` 打开组件索引，`/ponder tags`
打开分类索引。内置场景会先显示 5x5 地板，再显示上层结构。脚本书本保持固定位置并保留缓慢旋转。
在容器界面悬停已注册物品并按住显示的 Ponder 键，即可打开对应场景；默认键位是 `W`，可在控制设置中
重新绑定。

首次启动时，Ponder 会在 CraftTweaker 扫描前生成八个可编辑脚本到 `scripts/ponder/builtin`。
生成标记位于 `config/ponder/builtin-zs-generated.properties`。只要标记存在，就不会覆盖这八个脚本；
如果标记被删除，Ponder 会先把现有 builtin 目录备份到时间戳目录，再从随包资源原子恢复全部八个脚本，
最后写入新的标记。若复制或写标记失败，旧的 builtin 目录会被恢复回去。

## ZenScript 与 JSON 场景

自定义脚本放在 `scripts/ponder/scenes`，使用 `mods.ponder.SceneRegistry`、`TagRegistry`、
`SharedText`、`Index`、`Selection`、`Position` 和 `Vector` 静态入口。`SceneBuilder` 由
`SceneRegistry.create()` 返回，`World`、`Overlay` 和 `Effects` 分别通过场景的 `world`、
`overlay` 和 `effects` 属性访问，`TagBuilder` 由 `TagRegistry.create()` 返回。完整签名、句柄规则、
结构目录和同步语义见
[docs/ZENSCRIPT-API.md](docs/ZENSCRIPT-API.md) 与 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

修改 ZS 后必须重启；`/ponder reload` 只会重新应用已编译的场景定义并刷新结构缓存，不会重新执行 ZS。
服务器只同步已经验证过的场景指令，不同步脚本文件或结构 NBT。

JSON 场景包放在 `scripts/ponder/packs/**/*.ponder.json`，可通过 `/ponder reload` 重新读取。
每个文件独立保留 last-known-good：某个文件编辑失败时继续使用该文件上一次有效内容，其他有效包仍会
更新；删除文件则移除其贡献。格式、SNBT 字符串和离线工具说明见
[docs/JSON-PACKS.md](docs/JSON-PACKS.md)。

自定义结构放在 `scripts/ponder/structures/<namespace>/<path>.nbt`，结构 ID 写成
`<namespace>:<path>`。外部结构优先于资源包和模组 jar。

## 诊断命令

`/ponder list [local|server|effective]`、`/ponder inspect <scene>
[local|server|effective]`、`/ponder validate [local|server|effective]`、
`/ponder export <scene> [ir|timeline|all] [local|server|effective]` 和
`/ponder sync status [player]` 提供同一套诊断面。
`/ponder dependencies [local|server|effective]` 会输出结构依赖摘要，并在诊断目录生成带格式版本的
JSON 清单，记录 Provider、指纹、引用场景和兼容状态，不泄露外部结构绝对路径。

`local` 指客户端本地注册表，来源包括 Java 插件、builtin ZS、local ZS 和 local JSON。
`server` 指同步后经验证的服务端快照。`effective` 指合并后的生效视图：
同 Scene ID 的服务端脚本场景会覆盖本地脚本场景，而 Java 插件场景仍保持本地。
在专服控制台中，`local` 表示全部本地注册，`server` 表示可同步且非
`clientOnly` 的 ZS 集合，`effective` 等同当前服务端注册结果。

`/ponder reload` 和 `/ponder sync status` 在服务端执行时需要权限等级 2。
游戏内玩家发起的 `list`、`inspect`、`validate`、`export` 会转发到客户端诊断服务；
如果非玩家发送 `validate` 或 `export`，同样要走权限检查。

校验报告和导出文件写入 `logs/ponder/diagnostics`。Java 场景没有可导出的脚本 IR，
所以 `export ... ir` 只适用于脚本场景；`timeline` 对 Java 场景和脚本场景都可导出。

ZenScript 示例包构建为 `build/distributions/Ponder-ZenScript-Examples-1.3.0-alpha.1.zip`。
JSON 示例和作者工具分别构建为 `Ponder-JSON-Examples-1.3.0-alpha.1.zip` 与
`Ponder-JSON-Tools-1.3.0-alpha.1.zip`。

## 供其他模组调用

其他模组通过 `PonderPlugin` 注册组件、标签、共享文本和 Java 场景脚本。开发接入说明见
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)，示例插件位于 `examples/addon`。

## 内置 MMCE 兼容

Ponder 运行 jar 内置可选的 MMCE 2.3.2+ 兼容层。未安装 `modularmachinery` 时，不会加载任何直接引用
MMCE 类型的实现类；安装 MMCE 且 ABI 检查通过后，会自动注册结构 Provider、蓝图 Subject Resolver
以及 `mods.ponder.mmce.MMCEStructures` ZenScript 命名空间。

用户不再需要安装独立 Ponder-MMCE 运行 jar。`ponder-mmce` Gradle 子项目继续作为隔离的源码、
fixture 和 harness 边界，用于测试兼容层而不让 MMCE 类型进入播放器核心。如果同时安装旧
`ponder_mmce` 外部附属，本体会把兼容层所有权交给旧附属，避免 Provider 和 Resolver 重复注册。

当前支持静态结构、Dynamic Pattern、负坐标归一化、preview NBT、命名结构组、结构指纹和蓝图
component 映射。不会自动生成通用场景，暂不提供材料清单、配方 Overlay 和已放置控制器入口。

示例 Java addon 另行生成
`build/distributions/Ponder-Example-Addon-Smoke-1.3.0-alpha.1.zip`，其中包含 reobf addon、结构资源和调用
`ponder_example:pulse` 自定义 codec 的 ZS 场景，用于验证 ServiceLoader、IMC、协议 v3 协商以及
缺少 codec 时在发送快照正文前拒绝。

## 兼容性政策

公开包名和 DSL 名称会尽量跟随当前 Ponder 的语义，只在 Minecraft 1.12.2 有等价类型时才保留。
Minecraft 和 Forge 类型映射到 MCP 1.12.2 对应类型。现代专有系统用可运行的 1.12.2 适配层替代，
而不是空壳兼容实现。这是源码迁移兼容，不是与 1.21.1 jar 的二进制兼容，也不是“所有场景源码都能
零修改编译”的承诺。详细映射见 [docs/API-COMPATIBILITY.md](docs/API-COMPATIBILITY.md)。
仓库同时固定 `api-signatures/ponder-api-1.2.0.sig` 基线和 1.3.0 精确签名，CI 使用 JDK 8
`javap` 检查二进制破坏与未审阅的签名漂移。

本项目只支持 Forge 1.12.2，不提供 Fabric、NeoForge、Cleanroom 专用行为或其他第三方场景格式。
CatServer 路径属于实验性支持，不是 1.3.0 发布门槛；标准 Forge 真实客户端仍然是发布要求。
