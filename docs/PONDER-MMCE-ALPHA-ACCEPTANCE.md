# Ponder-MMCE 0.1 Alpha 客户端验收

本清单只适用于同一次 GitHub Actions 构建生成的 Ponder 1.1.2、Ponder-MMCE
0.1.0-alpha 和 Smoke Pack。重新构建后必须重新验收。

## 安装

安装 Forge 14.23.5.2847、Java 8、MixinBooter 11.2、CraftTweaker 4.1.20.698、
MMCE 2.3.2、Ponder 1.1.2、Ponder-MMCE 0.1.0-alpha，以及同次构建生成的
Ponder-MMCE client harness。将 Smoke Pack 中的 `config` 和 `scripts` 合并到实例根目录。

为 client harness 设置输出目录：

```text
-Dponder.mmce.clientHarness.output=<绝对证据目录>
```

启动后等待生成 `client-mmce-ok.flag` 和 `ponder-mmce-client-report.json`。自动报告必须为
`PASS`，并包含静态、动态两张非空截图、旧指纹隔离检查及两个运行 jar 的 SHA-256。

## 人工检查

- 为 `ponder_mmce_static_demo` 蓝图悬停并按住 `W`，确认静态场景打开。
- 为 `ponder_mmce_dynamic_demo` 蓝图悬停并按住 `W`，确认三段重复结构和末端输出出现。
- 为 `ponder_mmce_unconfigured` 蓝图悬停，确认显示“未配置 Ponder 场景”且不会打开空场景。
- 使用真实鼠标拖动旋转并用滚轮缩放，关闭后普通世界输入正常。
- 分别在 GUI Scale 1、2、3、4 检查标题、结构、提示、进度条和按钮无重叠或裁切。
- 执行 F11 全屏往返，确认结构投影和鼠标命中保持正确。
- 执行资源重载，重新打开两个场景，确认结构、透明方块和命名组仍正常。
- 保留至少四张人工截图和客户端 `latest.log`。

## 生成最终报告

```powershell
.\tools\complete-ponder-mmce-client-acceptance.ps1 `
  -HarnessReport "<证据目录>\ponder-mmce-client-report.json" `
  -EvidenceDirectory "<人工截图和 latest.log 目录>" `
  -ConfirmedBlueprintWEntry `
  -ConfirmedRealMouse `
  -ConfirmedGuiScales1To4 `
  -ConfirmedFullscreenRoundTrip `
  -ConfirmedResourceReload `
  -Notes "Standard Forge 14.23.5.2847 manual acceptance"
```

输出的 `build/reports/ponder-mmce-client-acceptance.json` 是
`Publish 1.1.2 Alpha` 工作流的 `client_report_url` 输入。该 URL 必须能直接下载 JSON。
