# Ponder 1.12.2 开发接入

本项目的公开 Ponder API 位于 `net.createmod.ponder.api`，脚本支持和运行时适配类位于
`net.createmod.ponder.script` 与 `net.createmod.catnip`。addon 编译时可依赖
`Ponder-1.12.2-1.1.1-api.jar`，但不要把这个反混淆 API jar 安装到游戏。运行环境应安装 reobf 的
Ponder 主包、MixinBooter 9.1 或更高版本以及 CraftTweaker 4.1.20 或更高版本；addon 自身也必须经过
Forge 1.12.2 reobf 后才能发布。更完整的 ZenScript 参考见 [README](../README.md) 与
[docs/ZENSCRIPT-API.md](ZENSCRIPT-API.md)。

## 当前构建基线

当前开发版本为 `1.1.1-mc1.12.2`。默认构建与当前服务端验收基线使用 MixinBooter 11.2，运行元数据接受
9.1 及以上版本。可通过 `-PmixinBooterVersion=<版本>` 切换编译和发布校验所用版本。
Mixin refmap 的注解处理器固定使用 11.2；较旧 MixinBooter 版本仍可作为运行 API 编译目标，但它们自身没有
携带当前构建链所需的完整 ASM 类路径。

当客户端和服务端都安装 Ponder 时，必须使用完全相同的 Ponder 版本；安装了 Ponder 的客户端也可以连接
没有 Ponder 的普通服务器。历史 1.0.x/1.1.0 的构建哈希和报告不适用于当前版本；当前 1.1.1
成品的 SHA-256 由 GitHub Actions build job summary 和上传的 release artifact bundle 发布，不在这里手写静态值。

旧版标准 Forge 与 CatServer 报告不能转移到 1.1.1。当前版本必须重新生成发布报告；针对要声明兼容的
MixinBooter 和 CraftTweaker 版本还应分别执行服务端和客户端门槛。开发或发版时不得把专服启动、自动化
测试或 `PASS_SERVER_ONLY` 当成标准 Forge 客户端门槛已经通过。标准 Forge 真实客户端仍然是发布门槛，
CatServer 客户端支持只是实验线，不阻塞 1.1.1。实时证据和剩余门槛见 [TESTING.md](TESTING.md)。

## 注册插件

实现 `PonderPlugin`。最小的 Java 场景注册如下，其中结构文件位于
`assets/examplemod/ponder/machine/basic.nbt`：

```java
package com.example.examplemod;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public final class ExamplePonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return "examplemod";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(
            new ResourceLocation("examplemod", "machine"),
            "machine/basic",
            ExamplePonderPlugin::basicScene
        );
    }

    private static void basicScene(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("machine_basic", "Machine Basics");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), EnumFacing.DOWN);
        scene.addKeyframe();
        scene.markAsFinished();
    }
}
```

推荐在 addon 的 `META-INF/services/net.createmod.ponder.api.registration.PonderPlugin` 中写入实现类全名：

```text
com.example.examplemod.ExamplePonderPlugin
```

Ponder 会在 load-complete 阶段通过 `ServiceLoader` 发现插件。也可在 load-complete 之前直接调用
`PonderIndex.addPlugin(new ExamplePonderPlugin())`；相同实现类只注册一次，注册表冻结后的迟到调用会抛出
`IllegalStateException`，因此不要在世界加载或客户端 GUI 打开时注册。

无法使用 services 文件时，也可以在 Forge IMC 阶段向 `ponder_legacy` 发送键 `register_ponder_plugin`，
消息值为插件实现类全名。实现类必须具有无参数构造方法。

```java
FMLInterModComms.sendMessage(
    "ponder_legacy",
    "register_ponder_plugin",
    "com.example.examplemod.ExamplePonderPlugin"
);
```

三种入口最终合并到同一注册队列。Ponder 自带插件优先，其余插件按 `getModId()` 和实现类全名稳定排序；
随后依次注册场景、标签、共享文本与索引排除，并冻结结果。不要同时用不同包装类重复注册同一内容来
依赖未定义的覆盖顺序。

## 内置脚本

首次启动时会在 CraftTweaker 扫描前生成八个可编辑脚本到 `scripts/ponder/builtin`：
`basics.zs`、`storage.zs`、`smelting.zs`、`piston.zs`、`redstone.zs`、`render_layers.zs`、
`fluids.zs` 和 `rail.zs`。

生成标记位于 `config/ponder/builtin-zs-generated.properties`。标记存在时，安装器不会覆盖这八个脚本。
如果标记被删除，安装器会先把现有 builtin 目录备份到带时间戳的目录，再从随包资源原子恢复全部八个脚本，
最后写入新的标记。若复制或写标记失败，旧的 builtin 目录会被恢复回去。

## 打开与操作场景

- `/ponder examplemod:machine` 打开指定组件的场景；`/ponder index` 和 `/ponder tags` 打开索引视图。
- `/ponder reload` 要求权限等级 2，用于重新构建客户端 Ponder 注册表并刷新结构缓存；它不会重新执行
  ZenScript，脚本变更仍然需要重启。
- 容器中悬停已注册物品并按住“思索”按键可打开对应场景；默认是 `W`，可在控制设置中重新绑定。
- 场景内拖动鼠标旋转，滚轮缩放，空格暂停/继续，`R` 重播，左右方向键切换场景，`Q` 切换识别模式。
- 底部进度条可以拖动；跳转会从最近快照恢复后确定性重放到目标 tick。

## ZenScript 接入

脚本目录为 `scripts/ponder`。静态入口包括：

- `mods.ponder.SceneRegistry`
- `mods.ponder.TagRegistry`
- `mods.ponder.SharedText`
- `mods.ponder.Index`
- `mods.ponder.Selection`
- `mods.ponder.Position`
- `mods.ponder.Vector`

`SceneBuilder` 由 `SceneRegistry.create()` 返回；`World`、`Overlay` 和 `Effects` 分别通过
`scene.world`、`scene.overlay` 和 `scene.effects` 访问；`TagBuilder` 由 `TagRegistry.create()`
返回。它们是 builder/返回类型，不是需要独立调用的静态入口。详细签名、句柄生命周期、结构目录、
同步和限制语义见 [docs/ZENSCRIPT-API.md](ZENSCRIPT-API.md)。
一个最小脚本片段如下：

```zenscript
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create("minecraft:paper", "example:paper", "Paper", "example:paper");
scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(20);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.markAsFinished();
scene.register();
```

ZS 在 CraftTweaker 初始化阶段执行并生成不可变场景指令，不保留播放期任意回调。脚本可以使用循环、
函数和条件生成指令，但修改脚本后必须重启。`/ponder reload` 不重新执行 ZS，只会重建已编译的注册表和
结构缓存。

外部结构放在 `scripts/ponder/structures/<namespace>/<path>.nbt`，结构 ID 写成 `<namespace>:<path>`。
服务器登录时同步的是验证后的场景 IR，服务器不发送 ZS 文件或结构 NBT。Java addon 可通过
`ScriptInstructionCodecs.register(...)` 注册确定性、可序列化的扩展指令。

## 结构文件

场景结构可以放在 `scripts/ponder/structures/<namespace>/<path>.nbt`，也可以放在
`assets/<namespace>/ponder/<path>.nbt`。注册结构 ID 时不要添加 `ponder/` 前缀或 `.nbt` 后缀。

加载器优先使用外部结构，然后回退到资源包，再回退到模组 jar。外部根目录会拒绝路径逃逸、符号链接和
junction；单个外部结构最多 16 MiB，palette 最多 65536 项，block 列表最多 16 MiB 条目。
现代方块名会经过显式 1.12 映射；无法映射的状态显示为 barrier，并在日志中汇总，不会静默替换为空气。

内置 `ponder:demo/basics` 及 storage、smelting、piston、redstone、render_layers、fluids、rail 都是独立的
压缩 structure NBT，不存在代码兜底。可用
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate-demo-structure.ps1`
一次性确定性重建全部资源。脚本只写入 1.12 原版内容；每个结构都由测试核对目标方块、方块实体、实体、
渲染层和解析诊断。

场景坐标永远是结构局部坐标。公开虚拟世界类型是 `PonderLevel`，其实现继承内部 `PonderWorld`；
`localToWorld` 与 `worldToLocal` 才处理 anchor，调用方不要提前重复叠加 anchor。

## 时间轴规则

`SceneBuilder` 使用绝对 tick：指令在当前 build cursor 上安排，`idle(ticks)` 只推进 cursor，不会暂停
已经开始的动画。相同 tick 的指令按注册顺序执行。

`addKeyframe()` 标记当前 tick，`addLazyKeyframe()` 标记当前 tick 后 6 tick。播放回退会恢复虚拟世界、
元素链接、指令状态和场景状态，再确定性重放到目标 tick。

普通 `showBasePlate()` 和 `world().showSection()` 仍占用 15 tick，但会过滤空气后，从选择中心向外按方块
错峰平滑滑入。`showIndependentSection`、`showSectionAndMerge`、`glueBlockOnto` 和隐藏接口保持原有整区段
行为；公开方法签名没有变化。

所有网络或真实世界修改必须由 addon 自己在服务端完成。Ponder 的场景 world 是无存盘、无网络副作用的
客户端虚拟世界，不能用来替代真实玩法逻辑。

## 同步与限制

- `SceneRegistry` 最多 2048 个场景。
- 单个场景最多 4096 条指令。
- 单条自定义指令数据最大 256 KiB。
- 场景同步会先做能力协商，再分块发送已验证的场景快照；协议号必须一致，缺失 codec 会直接拒绝。
- 默认分块大小是 256 KiB，传输超时为 30 秒。
- 服务器只同步已验证的场景 IR，不同步脚本或外部结构文件。

## 1.21 到 1.12 类型映射

| 新版类型 | 1.12.2 API |
| --- | --- |
| `Direction` | `EnumFacing` |
| `BlockState` | `IBlockState` |
| `BlockEntity` | `TileEntity` |
| `Level` | `World` / `PonderLevel` |
| `Vec3` | `Vec3d` |
| `AABB` | `AxisAlignedBB` |
| `Component` | `ITextComponent` |
| `Property` | `IProperty` |
| particle data | `EnumParticleTypes` 与可选整型参数 |

现代 `Codec`、`StreamCodec` 和 DataComponent 没有直接移植；对应功能分别使用 Catnip NBT/Gson codec、
`PacketBuffer` codec 和 `ItemStack` NBT。命令使用 `CommandBase`，配置使用 Forge `Configuration`。

逐类状态和现代 accessor/mixin 的去向见 [API-COMPATIBILITY.md](API-COMPATIBILITY.md)。这些映射保留用途和
DSL 语义，但方法签名使用 1.12.2 类型，不与 1.21.1 二进制兼容。

## 兼容性约束

- Java 源码和字节码目标均为 Java 8。
- 不要在公共或专服会加载的类中引用 `net.minecraft.client`。
- 不要把 MixinBooter、MixinExtras 或其他 loader 嵌入 addon jar。
- UI 与固定渲染管线只在客户端加载；注册、结构解析与语言收集可在专服安全执行。
- 网络包必须限制字符串、集合和枚举范围，并在主线程处理；客户端包不得修改真实服务端世界。
- 不要从虚拟 `PonderLevel` 获取真实存档、区块票或服务端网络状态；它只为场景提供隔离内存世界。
