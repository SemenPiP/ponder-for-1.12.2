# Ponder Legacy 1.3.0 Alpha 1 客户端验收

本验收只适用于同一次 `Build and Verify` 主分支运行产生的
`Ponder-1.12.2-1.3.0-alpha.1.jar` 和
`Ponder-Client-Harness-1.12.2-1.3.0-alpha.1.jar`。重新构建后必须重新验收。

## 环境

- Minecraft 1.12.2
- Forge 14.23.5.2847
- 64 位 Java 8
- MixinBooter 11.2
- CraftTweaker 4.1.20.698
- 独立、可丢弃的标准 Forge 客户端实例

从 `Ponder-Client-Acceptance-Kit-1.3.0-alpha.1.zip` 合并 `scripts` 目录，并把
`mods/Ponder-Client-Harness-1.12.2-1.3.0-alpha.1.jar` 安装到实例。另行安装同一次 Actions
运行下载的 Ponder 主包，以及上表中的 CraftTweaker 和 MixinBooter。

## 自动阶段

为客户端 JVM 增加：

```text
-Dponder.clientHarness.output=<绝对证据目录>
```

启动客户端后 harness 会自动执行并退出，输出：

- `report.json`
- `client-demo-ok.flag` 或 `client-demo-failed.flag`
- `screenshots/*.png`
- `json-runtime-fixture`

自动阶段覆盖：

- 八个内置场景完整播放和目标内容断言；
- JSON 场景实际打开、渲染和 SNBT 应用；
- `/ponder reload` 使用的 JSON 重载链、last-known-good、删除包和 ZS 冲突隔离；
- 本地/服务器/effective 覆盖和清除服务器层；
- 程序化拖动、缩放、GUI Scale 1-4、全屏往返和资源重载；
- OpenGL 状态恢复、深度读回、截图非空像素和关闭场景后的 UI 状态。

`report.json` 必须为 `PASS`。自动输入注入不能替代真实键鼠和视觉判断。

## 人工阶段

从实例移除客户端 harness jar，保留同一个 Ponder jar 和 JSON 示例。重新启动并保存
`latest.log`、截图或录屏。至少完成：

1. 分别完整播放八个内置场景，确认结构、实体、流体、粒子、Overlay 和关键帧正常。
2. 在已注册物品上真实悬停并按住 `W` 打开场景，未注册物品不能误触发。
3. 使用真实鼠标拖动旋转、滚轮缩放、时间轴跳转、暂停和重播。
4. 将 GUI Scale 依次设为 1、2、3、4，确认标题、进度条、按钮和提示不重叠或裁切。
5. 执行 F11 全屏往返，确认投影、命中和控制保持正常。
6. 执行资源重载，确认场景 section、材质和 TileEntity 重新渲染。
7. 修改 JSON 示例标题或指令后执行 `/ponder reload`，确认更新立即生效；再制造坏 JSON，
   确认 last-known-good 保留；删除文件后确认场景移除。
8. 关闭 Ponder 后检查普通世界、物品、GUI、粒子和 TileEntity 渲染没有状态污染。

人工证据目录至少保留八张 PNG 和一个 `latest.log`。建议文件名包含场景、GUI Scale
和操作，例如 `scale-4-rail.png`、`json-last-known-good.png`。

## 生成最终报告

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\complete-ponder-client-acceptance.ps1 `
  -HarnessReport <自动证据目录>\report.json `
  -EvidenceDirectory <人工证据目录> `
  -PonderJar <Actions成品>\Ponder-1.12.2-1.3.0-alpha.1.jar `
  -ClientHarnessJar <Actions成品>\Ponder-Client-Harness-1.12.2-1.3.0-alpha.1.jar `
  -ActionsRunId <主分支Build-and-Verify运行ID> `
  -SourceCommit <完整40位提交SHA> `
  -ConfirmedEightBuiltinScenes `
  -ConfirmedJsonReloadCommand `
  -ConfirmedWEntry `
  -ConfirmedRealMouse `
  -ConfirmedGuiScales1To4 `
  -ConfirmedFullscreenRoundTrip `
  -ConfirmedResourceReload `
  -ConfirmedOrdinaryWorldAfterClose
```

输出 `build/reports/ponder-1.3.0-alpha.1-client-acceptance.json`。任何确认项缺失、自动检查失败、
版本不符、证据不足或 jar 文件名错误都会拒绝生成 `PASS` 报告。

Alpha 1 可以在该报告尚未完成时作为 GitHub Pre-release 发布，但 Release 必须附带
`NOT_RUN` 客户端证据文件并明确说明只通过自动化和专服验证。Beta、RC 和稳定版必须完成本报告。
CatServer 客户端兼容仍属于实验线。
