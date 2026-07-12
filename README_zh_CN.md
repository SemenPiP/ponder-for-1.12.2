# Ponder Minecraft 1.12.2 独立重写版

这是面向 Minecraft 1.12.2、Forge 和 Java 8 的 Ponder/Catnip 独立实现。
官方 1.21.1 项目仅作为功能、公开 API、翻译与视觉资源规格；项目没有复制或改造任何第三方 1.12.2 回移源码。

源码实现和自动化检查已经建立，但某个 jar 只有在同一成品完成
[发布验收门槛](docs/TESTING.md) 后才能称为已验证版本。仅通过专服启动不代表客户端渲染或
CatServer 完整兼容已经通过。

## 当前验证状态

`1.0.3` 保留了 1.0.2 的 GUI、场景和回放修复，并取消 MixinBooter 11.2 精确运行锁定。
运行元数据现在接受 MixinBooter 9.1 及以上版本，八个内置场景仍均为精确 32 秒。该版本的构建、
标准 Forge、CatServer 和客户端视觉证据必须绑定到新的成品 SHA-256；历史报告只作为历史记录，
不能证明当前 1.0.3 成品已经通过。

当前工作区是远程服务器环境，不能提供可可靠判断 Minecraft 画面的桌面，也无法完成真实鼠标、
全屏切换和 GUI scale 1-4 的人工观感验收。因此，标准 Forge 客户端视觉验证和真实客户端连接
CatServer 后打开演示仍未完成，完整发布门槛尚未满足。服务端单独验证结果记录在
[TESTING](docs/TESTING.md)，不得把它解释为客户端验证。

## 安装

必须同时安装：

- Forge 14.23.5.2847 或更高版本
- `Ponder-1.12.2-1.0.3.jar`
- MixinBooter 9.1 或更高版本，并作为单独模组安装

Ponder 不会把 MixinBooter、CleanMix 或 MixinExtras 打进自身 jar。
当前默认构建和完整服务端验收基线仍使用 MixinBooter 11.2，其已核对 SHA-256 为
`48667BC07D4F9D54A5C0F808DAA02DEB956128664DB24269EB34460F4CA2462E`。
默认构建使用 [CleanroomMC Maven 上的 11.2 成品](https://maven.cleanroommc.com/zone/rong/mixinbooter/11.2/mixinbooter-11.2.jar)；
手工下载后应先核对上述哈希。

将 Ponder 成品和 MixinBooter 放入同一实例的 `mods` 目录；专服安装 Ponder 时也必须安装
MixinBooter。不要把 `-api`、`-sources` 或 `-dev` jar 当作运行模组安装：API jar 只供 addon
在开发环境编译，sources jar 只供阅读，`build/devlibs` 下的 jar 只供反混淆开发环境使用。

## 构建

```bat
set JAVA_HOME=C:\Program Files\Java\jdk-1.8
gradlew.bat clean test build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\verify-release.ps1
```

开发客户端和服务端分别使用 `runClient`、`runServer`。编译基线是 Forge
14.23.5.2847，构建工具为 RFG 1.4.9、Gradle 8.14.3 和 MCP `stable_39`。`build` 已包含
测试、发布内容检查以及主包和示例包的 reobf；无需再用普通 `jar` 作为发布成品。任何运行兼容声明发布前，
都必须完成 [TESTING](docs/TESTING.md) 中的标准 Forge 与指定 CatServer 启动、连接和场景播放门槛。

默认使用 MixinBooter 11.2 作为运行目标；可通过
`gradlew.bat build -PmixinBooterVersion=10.7` 切换运行 API 编译依赖。生成 refmap 的注解处理器
固定使用 11.2，因为较旧版本没有携带完整的构建期 ASM 类路径。运行元数据接受 9.1 及以上版本，
但发布兼容声明仍应以实际执行过 Forge、CatServer 和客户端验收的版本为准。

可安装成品是 `build/libs/Ponder-1.12.2-1.0.3.jar`。`build/devlibs` 中带 `-dev`
classifier 的文件只用于开发环境，不得安装到正式服务端。执行 `reobfExampleAddonJar`
会生成独立示例模组 `build/libs/Ponder-Example-Addon-1.12.2-1.0.3.jar`。另外还会生成
`-api.jar` 和 `-sources.jar`，二者都不是运行模组。

## 内置演示

可使用 `/ponder <组件 ID>` 打开八个内置演示：`minecraft:crafting_table`、`minecraft:chest`、
`minecraft:furnace`、`minecraft:piston`、`minecraft:redstone_lamp`、`minecraft:glass`、
`minecraft:water_bucket` 和 `minecraft:rail`。`/ponder index` 打开组件索引，`/ponder tags` 打开分类索引。
内置场景会先逐块显示 5x5 地板，再逐块显示上层结构；脚本书本坐标保持固定，同时保留缓慢旋转。
也可以在容器界面悬停已注册物品，并按住提示中的“思索”按键打开场景；默认按键为 `W`，
可在控制设置中重新绑定。

## 供其他模组调用

其他模组通过 `PonderPlugin` 注册组件、标签、共享文本和 Java 场景脚本。
现代 Minecraft 类型已经换成 1.12.2 MCP 类型，详细映射及示例见
[API 兼容表](docs/API-COMPATIBILITY.md) 与 [开发接入说明](docs/DEVELOPMENT.md)。这里保证的是
包名、API 用途与运行行为可迁移，不是与 1.21.1 jar 的二进制兼容，也不承诺现代场景源码零修改编译。

本项目只支持 Forge 1.12.2，不提供 Fabric、NeoForge、Cleanroom 专用行为、第三方 JSON 场景格式或
第三方回移项目源码兼容层。CatServer 兼容承诺只限 [TESTING](docs/TESTING.md) 记录的指定 SHA-256 构建。
