# v1.1.0 部署验证

验证时间：2026-11-15 21:16（Asia/Shanghai）。

环境：Minecraft 1.21.1，AsyncYouer `1.21.1-3f16f9b7`，NeoForge `21.1.264`，Java 21。

安装目录中只有 `mods/mctg-terrainlock-1.1.0.jar`，没有配套 TerrainLock 插件 JAR。服务已由原有 MCSManager 实例启动并恢复 `running / healthy`。

| 检查 | 结果 |
| --- | --- |
| 八个目标类、13 个 Mixin 方法实际注入 | 13/13 |
| 原生 `/terrainlock status` 和 `/terrainlock audit` | 正常 |
| 已完整生成、原先未加载的区块 `(160,160)` | 可正常加载 |
| 未生成坐标 `(900096,900096)` 的同步 FULL 请求 | 返回未注册只读空对象，实际区块未加载 |
| Bukkit `loadChunk(..., true)` | 拒绝 |
| 直接 FULL 生成票据 | 拒绝 |
| 异步 `generate=true` 请求 | 完成并返回 null，无异常 |
| 移动到未生成区域的共享策略 | 拒绝 |
| 传送到未生成区域的共享策略 | 拒绝 |
| 移动到已有区块的共享策略 | 允许 |
| 下界、末地限制状态 | 均为 false |
| `save-all flush` 后检查新区块测试区域 | 无 region、entities、poi 文件产生 |
| 意外新增主世界完整区块计数 | 0 |

移动、传送和载具的入口已通过实际类注入检查；本次没有真人登录、真实客户端移动/传送或载具操作测试。

发布 JAR 的 SHA-256：

```text
cac9fd6f606bf33866a72f6e1294666ce7e42378fbeb8b8e6f79f9b156ddc231
```

旧版双组件已移出服务端加载目录并保留以便回退。升级没有更换世界生成器、设置世界边界或修改原有区块和数据库。
