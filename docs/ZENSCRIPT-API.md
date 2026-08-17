# Ponder Legacy ZenScript API

本文档对应 `1.3.0-alpha.1-mc1.12.2` 的实际 `@ZenClass` 和 `@ZenMethod`。脚本由
CraftTweaker 4.1.20+ 在启动阶段执行，推荐放在 `scripts/ponder/scenes`。修改脚本后必须重启；
`/ponder reload` 会重新读取 JSON 场景包，但只重新应用当前进程中已经编译的 ZS 定义。

诊断命令不属于 ZenScript API 本身，但它们和脚本作者的工作流绑定很紧：

- `/ponder list [local|server|effective]`
- `/ponder inspect <scene> [local|server|effective]`
- `/ponder validate [local|server|effective]`
- `/ponder export <scene> [ir|timeline|all] [local|server|effective]`
- `/ponder dependencies [local|server|effective]`
- `/ponder sync status [player]`

`local` 表示客户端本地注册表，来源包括 Java 插件、builtin ZS、local ZS 和 local JSON；`server`
表示通过同步并验证后的服务端快照；`effective` 表示合并后的生效视图，同 Scene ID 的服务端
脚本场景覆盖本地脚本场景，但 Java 插件场景保持本地来源。
在专服控制台中，`local` 表示全部本地注册，`server` 表示可同步且非 `clientOnly` 的 ZS
集合，`effective` 等同当前服务端注册结果。

玩家在游戏内发起的 `list`、`inspect`、`validate` 和 `export` 会转发到客户端诊断服务。
非玩家发送 `validate` 或 `export` 仍要走权限检查；`/ponder reload` 和 `/ponder sync status`
在服务端执行时需要权限等级 2。ZS 变更仍然必须重启，`reload` 不会重新执行脚本。
诊断报告和导出文件写入 `logs/ponder/diagnostics`。
Java storyboard 场景没有可导出的脚本 IR，所以 `export ... ir` 只适用于脚本场景；`timeline`
对 Java 场景和脚本场景都可导出。ZenScript 示例包打包为
`build/distributions/Ponder-ZenScript-Examples-1.3.0-alpha.1.zip`。
JSON 前端使用相同 IR，但其格式与工具单独记录在 [JSON-PACKS.md](JSON-PACKS.md)。

1.3.0 快照协议为 v3。服务器可以同步经过验证的 ZS 标签、标签 component 关联和共享文本；同 ID
服务器 ZS 元数据覆盖本地 ZS 元数据，断线后恢复本地层。Java 插件正式标签不能被服务器覆盖，冲突会
拒绝整个候选快照并保留旧层。自定义 `scene.custom(...)` 指令还会协商 codec 精确协议版本与实际能力，
缺少或不兼容时在发送快照正文前拒绝。

MMCE 2.3.2+ 兼容实现已经包含在 Ponder 运行 jar 中。检测到 `modularmachinery` 且 ABI 检查通过后，
Ponder 会自动注册 Java 结构 Provider、物品 Subject Resolver，以及
`mods.ponder.mmce.MMCEStructures` 和 `mods.ponder.mmce.MMCEStructureRef`：

```zenscript
MMCEStructures.machine(machineId)
MMCEStructures.staticStructure(machineId, includePreviewNbt)
MMCEStructures.dynamic(machineId, dynamicPattern, repetitions, patternOffset, facing)
MMCEStructures.dynamic(machineId, dynamicPattern, repetitions, patternOffset, facing, includePreviewNbt)
```

返回对象公开 `component`、`structure`、尺寸、控制器坐标、建议底板大小和 `fingerprint`；
`structure` 是可传给场景结构参数的稳定 `ponder_mmce:` 结构 ID。未安装 MMCE 时兼容实现不会启用；
脚本不应在没有 MMCE 的整合包中调用这些入口。不需要安装独立 Ponder-MMCE 运行 jar。

## 最小场景

```zenscript
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;

val scene = SceneRegistry.create(
    "minecraft:chest",
    "mypack:chest_storage",
    "箱子存储",
    "ponder:demo/storage"
);

scene.configureBasePlate(0, 0, 5);
scene.showBasePlate();
scene.idle(10);
scene.world.showSection(Selection.layersFrom(1), "down");
scene.markAsFinished();
scene.register();
```

`SceneRegistry.create()` 返回 `mods.ponder.SceneBuilder`。`scene.world`、`scene.overlay` 和
`scene.effects` 分别返回对应 builder；这些类型不是静态入口。每个场景必须调用一次
`register()`，未注册 builder 会在脚本加载结束后报告错误。

## 静态入口

### `mods.ponder.SceneRegistry`

```zenscript
SceneRegistry.create(componentId, sceneId, title, structureId)
SceneRegistry.removeScene(sceneId)
SceneRegistry.removeComponent(componentId)
```

Scene ID 在本地脚本层全局唯一。服务器同步层使用相同 Scene ID 时覆盖本地版本，断开后恢复本地场景。

### `mods.ponder.Selection`

```zenscript
Selection.position(x, y, z)
Selection.fromTo(x1, y1, z1, x2, y2, z2)
Selection.column(x, z)
Selection.layer(y)
Selection.layersFrom(y)
Selection.layers(y, height)
Selection.cuboid(x, y, z, offsetX, offsetY, offsetZ)
Selection.everywhere()
```

`layers` 要求 `height > 0`。`cuboid` 的后三个参数沿用 Java DSL 的包含式偏移语义。

### `mods.ponder.Position` 与 `mods.ponder.Vector`

```zenscript
Position.of(x, y, z)
Vector.of(x, y, z)
```

`Position` 保存整数方块坐标；`Vector` 保存有限的浮点坐标或偏移。两者公开只读的 `x`、`y`、`z`
属性。保留旧的分离坐标重载，现有脚本无需迁移。

### 标签、共享文本和索引

```zenscript
val tag = TagRegistry.create(id, iconItemId, title, description);
tag.addComponent(componentId);
tag.indexed(true);
tag.register();

SharedText.register(key, defaultText);
Index.exclude(itemId);
```

`TagRegistry.create()` 返回 `mods.ponder.TagBuilder`。同一脚本注册表中重复 tag ID 或共享文本 key
会被拒绝。

## `mods.ponder.SceneBuilder`

```zenscript
scene.tag(tagId)
scene.clientOnly()
scene.configureBasePlate(x, z, size)
scene.showBasePlate()
scene.removeShadow()
scene.scaleSceneView(scale)
scene.setSceneOffsetY(offset)
scene.idle(ticks)
scene.idleSeconds(seconds)
scene.rotateCameraY(degrees)
scene.addKeyframe()
scene.addLazyKeyframe()
scene.markAsFinished()
scene.setNextUpEnabled(enabled)
scene.movePointOfInterest(x, y, z)
scene.movePointOfInterest(vector)
scene.custom(codecId, data)
scene.register()
```

`clientOnly()` 排除服务器快照同步。`custom()` 只接受 Java addon 已注册的确定性
`ScriptInstructionCodec` 和 map 形式 `IData`，不会开放播放期 callback。

## `mods.ponder.World`

### 区段

```zenscript
world.showSection(selection, direction)
world.hideSection(selection, direction)
world.restoreBlocks(selection)
world.showIndependentSection(handle, selection, direction)
world.showIndependentSectionImmediately(handle, selection)
world.makeSectionIndependent(handle, selection)
world.showSectionAndMerge(selection, direction, handle)
world.glueBlockOnto(x, y, z, direction, handle)
world.glueBlockOnto(position, direction, handle)
world.hideIndependentSection(handle, direction)
world.moveSection(handle, x, y, z, duration)
world.moveSection(handle, vector, duration)
world.rotateSection(handle, x, y, z, duration)
world.rotateSection(handle, vector, duration)
world.configureCenterOfRotation(handle, x, y, z)
world.configureCenterOfRotation(handle, vector)
world.configureStabilization(handle, x, y, z)
world.configureStabilization(handle, vector)
```

### 方块与 TileEntity

```zenscript
world.setBlock(x, y, z, state, particles)
world.setBlock(position, state, particles)
world.setBlocks(selection, state, particles)
world.replaceBlocks(selection, state, particles)
world.destroyBlock(x, y, z)
world.destroyBlock(position)
world.incrementBlockBreakingProgress(x, y, z)
world.incrementBlockBreakingProgress(position)
world.cycleBlockProperty(x, y, z, property)
world.cycleBlockProperty(position, property)
world.toggleRedstonePower(selection)
world.modifyTileNBT(selection, data, replace, redraw)
```

方块状态使用资源 ID 和 1.12 属性字符串。`modifyTileNBT` 要求 map 形式 `IData`，只做声明式替换或
合并，不允许调用 TileEntity 方法。

### 物品实体

```zenscript
world.createItemEntity(handle, x, y, z, motionX, motionY, motionZ, itemId, count, meta)
world.createItemEntity(handle, positionVector, motionVector, itemId, count, meta)
world.moveItem(handle, x, y, z, duration)
world.moveItem(handle, offsetVector, duration)
world.setItemVisible(handle, visible)
world.hideItem(handle)
world.showItem(handle)
world.removeItem(handle)
```

`removeItem` 会终止句柄；之后继续引用或重新定义同名句柄会在注册阶段失败。

### 矿车与鹦鹉

```zenscript
world.createMinecart(handle, x, y, z, angle, type)
world.createMinecart(handle, position, angle, type)
world.createCart(handle, x, y, z, angle, type)
world.createCart(handle, position, angle, type)
world.moveMinecart(handle, x, y, z, duration)
world.moveMinecart(handle, offset, duration)
world.moveCart(handle, x, y, z, duration)
world.moveCart(handle, offset, duration)
world.rotateMinecart(handle, angle, duration)
world.rotateCart(handle, angle, duration)
world.hideMinecart(handle, direction)

world.createParrot(handle, x, y, z, pose)
world.createParrot(handle, position, pose)
world.createBirb(handle, x, y, z, pose)
world.createBirb(handle, position, pose)
world.changeParrotPose(handle, pose)
world.changeBirbPose(handle, pose)
world.moveParrot(handle, x, y, z, duration)
world.moveParrot(handle, offset, duration)
world.rotateParrot(handle, x, y, z, duration)
world.rotateParrot(handle, rotation, duration)
world.hideParrot(handle, direction)
```

矿车类型：`empty`、`chest`、`furnace`、`hopper`、`tnt`。鹦鹉姿态：`dance`、`flappy`、
`face_poi`、`face_cursor`；`face_point_of_interest` 是 `face_poi` 的别名。

## `mods.ponder.Overlay`

```zenscript
overlay.showText(duration, text, x, y, z, color, nearTarget, keyframe)
overlay.showText(duration, text, target, color, nearTarget, keyframe)
overlay.showSharedText(duration, key, params, x, y, z, color, nearTarget, keyframe)
overlay.showSharedText(duration, key, params, target, color, nearTarget, keyframe)
overlay.showIndependentText(duration, text, y, color, keyframe)
overlay.showControls(duration, x, y, z, pointing, action, itemId)
overlay.showControls(duration, target, pointing, action, itemId)
overlay.showLine(color, x1, y1, z1, x2, y2, z2, duration, big)
overlay.showLine(color, from, to, duration, big)
overlay.showOutline(color, slot, selection, duration)
overlay.showOutlineWithText(text, color, selection, duration, keyframe)
overlay.showBoundingBox(color, slot, minX, minY, minZ, maxX, maxY, maxZ, duration)
overlay.showBoundingBox(color, slot, minimum, maximum, duration)
overlay.showScrollInput(location, side, duration)
overlay.showCenteredScrollInput(position, side, duration)
overlay.showRepeaterScrollInput(position, duration)
overlay.showFilterSlotInput(location, duration)
overlay.showFilterSlotInput(location, side, duration)
```

`params` 是可序列化 `String[]`。不带命名空间的共享文本 key 使用当前 Scene ID 的命名空间。
`action` 支持 `right_click`、`left_click`、`scroll`。颜色名称必须存在于 `PonderPalette`。

## `mods.ponder.Effects`

```zenscript
effects.indicateRedstone(x, y, z)
effects.indicateRedstone(position)
effects.indicateSuccess(x, y, z)
effects.indicateSuccess(position)
effects.createRedstoneParticles(x, y, z, color, amount)
effects.createRedstoneParticles(position, color, amount)
effects.emitParticles(type, x, y, z, motionX, motionY, motionZ, amount, cycles)
effects.emitParticles(type, position, motion, amount, cycles)
effects.emitParticlesWithinBlock(type, x, y, z, motionX, motionY, motionZ, amount, cycles)
effects.emitParticlesWithinBlock(type, position, motion, amount, cycles)
effects.movePointOfInterest(x, y, z)
effects.movePointOfInterest(vector)
```

粒子类型使用 1.12.2 `EnumParticleTypes` 名称，不支持任意粒子 callback。

## 字符串值与句柄

- 方向：`up`、`down`、`north`、`south`、`west`、`east`。
- 句柄：`[A-Za-z0-9_.-]{1,64}`。
- 句柄类型：section、item、minecart、parrot；跨类型引用会被拒绝。
- 同名句柄不能重复定义；被终止的物品句柄不能再次使用。
- duration 和 idle 范围为 `0..72000` tick。
- Resource ID 最长 256 字符，普通文本和共享文本参数最长 8192 字符。

## 结构目录

```text
scripts/ponder/
├─ builtin/
├─ scenes/
└─ structures/
   └─ <namespace>/<path>.nbt
```

`scripts/ponder/structures/mypack/machine/basic.nbt` 映射为 `mypack:machine/basic`。加载优先级：

1. 外部 `scripts/ponder/structures`
2. 当前资源包
3. 模组 jar/ClassLoader 资源

外部路径拒绝绝对路径、`..`、符号链接、Windows junction、非普通文件和超过 16 MiB 的文件。服务器只
同步 IR，不同步 `.zs` 或结构 NBT，因此客户端整合包必须预装服务器场景引用的结构。

## 快照限制

- 协议：v3，双方都安装 Ponder 时必须使用完全相同的模组版本。
- 每个快照最多 2048 个场景。
- 每个场景最多 4096 条指令。
- 每个场景未压缩数据最多 1 MiB。
- 整体压缩和未压缩数据分别最多 16 MiB。
- 单条指令数据最多 256 KiB。
- 每个快照最多声明 256 个扩展 codec。
- 网络分块最大 256 KiB，传输超时 30 秒。

客户端完成协议、codec、长度、哈希、字段、句柄和结构检查后才原子启用服务器层。失败时保留原本地
场景和旧服务器层；缺少结构只过滤受影响场景。
