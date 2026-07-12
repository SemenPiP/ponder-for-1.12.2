# Ponder 1.12.2 开发接入

本项目的公开 Ponder API 位于 `net.createmod.ponder.api`；场景所需的 Catnip 适配类位于
`net.createmod.catnip`。addon 编译时可依赖 `Ponder-1.12.2-1.0.2-api.jar`，但不要把这个
反混淆 API jar 安装到游戏。运行环境应安装 reobf 的 Ponder 主包与独立的 MixinBooter 11.2；
addon 自身也必须经过 Forge 1.12.2 reobf 后才能发布。

## 当前构建基线

当前开发版本为 `1.0.2-mc1.12.2`，依赖精确的 MixinBooter 11.2。历史 1.0.0/1.0.1 的构建哈希与
服务端报告不适用于当前版本；每次重新构建后都必须使用新 SHA-256 重新执行发布内容、标准 Forge
专服、CatServer 与客户端门槛。

当前工作区的干净构建、92 项单元测试、标准 Forge 2847 专服初启/同世界重启及 CatServer 四层
服务端预检，均已绑定到主包 SHA-256
`28C787F41B99BC469893A43EEA107EF98AD48CC36CAEC2E2DA3C7368B0A94EF2`。标准 Forge 与 CatServer 报告分别为
`build/reports/standard-forge-verification-20260712-042042984-4e31850b.md` 和
`build/reports/catserver-verification-20260712-042228873-f2d8f415.md`。这些结果不覆盖客户端画面
与交互。当前工作区无法创建硬件 OpenGL 上下文，标准 Forge 客户端视觉、真实鼠标、全屏和 GUI
scale 1-4 验收仍未执行；真实客户端连接 CatServer 也未执行，但不属于本轮必要门槛。开发或发版时
不得把专服启动、自动化测试或 `PASS_SERVER_ONLY` 当成标准 Forge 客户端门槛已经通过。实时证据和剩余门槛见
[TESTING.md](TESTING.md)。

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

推荐在 addon 的
`META-INF/services/net.createmod.ponder.api.registration.PonderPlugin` 中写入实现类全名：

```text
com.example.examplemod.ExamplePonderPlugin
```

Ponder 会在 load-complete 阶段通过 `ServiceLoader` 发现插件。也可在 load-complete 之前直接调用
`PonderIndex.addPlugin(new ExamplePonderPlugin())`；相同实现类只注册一次，注册表冻结后的迟到调用会
抛出 `IllegalStateException`，因此不要在世界加载或客户端 GUI 打开时注册。

无法使用 services 文件时，也可以在 Forge IMC 阶段向 `ponder` 发送键 `register_ponder_plugin`，消息值为插件实现类全名。实现类必须具有无参数构造方法。

```java
FMLInterModComms.sendMessage(
    "ponder",
    "register_ponder_plugin",
    "com.example.examplemod.ExamplePonderPlugin"
);
```

三种入口最终合并到同一注册队列。Ponder 自带插件优先，其余插件按 `getModId()` 和实现类全名稳定
排序；随后依次注册场景、标签、共享文本与索引排除，并冻结结果。不要同时用不同包装类重复注册同一
内容来依赖未定义的覆盖顺序。

完整、不会进入 Ponder 发布 jar 的示例位于 `examples/addon`。它演示了：

- 场景、标签、共享文本和索引排除注册；
- base plate、普通 section、独立 section、移动、旋转、稳定锚点；
- 文本、输入提示、线条、轮廓和 keyframe；
- 方块修改、实体创建、粒子、摄像机旋转和结束标记。

在项目根目录执行 `gradlew.bat reobfExampleAddonJar` 可单独构建示例。开发环境产物是
`build/devlibs/Ponder-Example-Addon-1.12.2-1.0.2-dev.jar`；可安装到标准 Forge/CatServer
的 SRG 成品是 `build/libs/Ponder-Example-Addon-1.12.2-1.0.2.jar`。不要发布或安装带
`-dev` classifier 的示例 jar。示例只依赖 Ponder 的公开 API，且不会被打进 Ponder 主 jar。

## 打开与操作场景

- `/ponder examplemod:machine` 打开指定组件的场景；`/ponder index` 和 `/ponder tags` 打开索引视图。
- `/ponder reload` 要求权限等级 2，用于重新构建客户端 Ponder 注册表；材质/模型资源仍使用原版资源重载入口。
- 容器中悬停已注册物品并按住“前进”键可打开对应场景。
- 场景内拖动鼠标旋转，滚轮缩放，空格暂停/继续，`R` 重播，左右方向键切换场景，`Q` 切换识别模式。
- 底部进度条可以拖动；跳转会从最近快照恢复后确定性重放到目标 tick。

## 结构文件

场景结构放在 `assets/<namespace>/ponder/<path>.nbt`。注册 `new ResourceLocation(namespace, path)` 时不要添加 `ponder/` 前缀或 `.nbt` 后缀。

加载器接受原版 structure NBT，严格验证 `size`、`palette`、`blocks`、`entities` 和 palette 索引。现代方块名会经过显式 1.12 映射；无法映射的状态显示为 barrier，并在日志中汇总，不会静默替换为空气。

内置 `ponder:demo/basics` 及 storage、smelting、piston、redstone、render_layers、fluids、rail
都是独立的压缩 structure NBT，不存在代码兜底。可用
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/generate-demo-structure.ps1`
一次性确定性重建全部资源。脚本只写入 1.12 原版内容；每个结构都由测试核对目标方块、方块实体、
实体、渲染层和解析诊断。

场景坐标永远是结构局部坐标。公开虚拟世界类型是 `PonderLevel`，其实现继承内部
`PonderWorld`；`localToWorld` 与 `worldToLocal` 才处理 anchor，调用方不要提前重复叠加 anchor。

## 时间轴规则

SceneBuilder 使用绝对 tick：指令在当前 build cursor 上安排，`idle(ticks)` 只推进 cursor，不会暂停已经开始的动画。相同 tick 的指令按注册顺序执行。

`addKeyframe()` 标记当前 tick，`addLazyKeyframe()` 标记当前 tick 后 6 tick。播放回退会恢复虚拟世界、元素链接、指令状态和场景状态，再确定性重放到目标 tick。

普通 `showBasePlate()` 和 `world().showSection()` 仍占用 15 tick，但会过滤空气后，从选择中心向外按方块错峰平滑滑入。
`showIndependentSection`、`showSectionAndMerge`、`glueBlockOnto` 和隐藏接口保持原有整区段行为；公开方法签名没有变化。

所有网络或真实世界修改必须由 addon 自己在服务端完成。Ponder 的场景 world 是无存盘、无网络副作用的客户端虚拟世界，不能用来替代真实玩法逻辑。

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

现代 `Codec`、`StreamCodec` 和 DataComponent 没有直接移植；对应功能分别使用 Catnip NBT/Gson codec、`PacketBuffer` codec 和 `ItemStack` NBT。命令使用 `CommandBase`，配置使用 Forge `Configuration`。

逐类状态和现代 accessor/mixin 的去向见 [API-COMPATIBILITY.md](API-COMPATIBILITY.md)。这些映射
保留用途和 DSL 语义，但方法签名使用 1.12.2 类型，不与 1.21.1 二进制兼容。

## 兼容性约束

- Java 源码和字节码目标均为 Java 8。
- 不要在公共或专服会加载的类中引用 `net.minecraft.client`。
- 不要把 MixinBooter、MixinExtras 或其他 loader 嵌入 addon jar。
- UI 与固定渲染管线只在客户端加载；注册、结构解析与语言收集可在专服安全执行。
- 网络包必须限制字符串、集合和枚举范围，并在主线程处理；客户端包不得修改真实服务端世界。
- 不要从虚拟 `PonderLevel` 获取真实存档、区块票或服务端网络状态；它只为场景提供隔离内存世界。
