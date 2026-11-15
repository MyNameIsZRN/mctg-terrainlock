# MCTG Terrain Lock

Single-JAR, server-side reference mod for **AsyncYouer 1.21.1-3f16f9b7 / NeoForge 21.1.264 / Java 21**. Disables new Overworld terrain generation while retaining normal loading of existing chunks. No companion plugin or client mod is needed. This is build-specific and is not validated against current official 1.21.1 or 26.3 builds.

[Download the v1.1.0 JAR](downloads/mctg-terrainlock-1.1.0.jar) · [Checksums](downloads/SHA256SUMS) · [Validation and limitations](VERIFICATION.md)

关闭主世界的新地形生成，同时保留磁盘上已有完整区块的正常加载。

这是一个**单 JAR 服务端模组**。客户端不用安装，`plugins/` 中也不需要配套插件。原先插件提供的移动限制、传送限制、状态查询和验证命令已合入模组。

## 支持的服务端

- Minecraft 1.21.1，Java 21。
- 当前验证核心：AsyncYouer `1.21.1-3f16f9b7`，NeoForge `21.1.264`。
- 模组使用该核心的 Moonrise 和 CraftBukkit 内部接口，**不适用于原版 NeoForge、普通 Paper 或任意其他版本**。
- 更新核心前必须重新验证；必要的 Mixin 无法匹配时会阻止启动，避免限制静默失效。

## 行为

- 只锁定 `minecraft:overworld`。下界、末地和其他维度保持原有行为。
- 已保存为 `minecraft:full` 的区块照常从磁盘加载，包括当前未加载到内存的区块。
- 禁止玩家视距、同步与异步区块请求、Bukkit 强制加载及直接生成票据触发新区块生成。
- 玩家步行和载具移动进入未生成区块时回退；玩家和实体传送到未生成区域时拦截。
- 不更换地形生成器，不设置方形世界边界，不将未生成区域保存成完整虚空地形。
- 原生同步读取接口必须返回非空对象时，返回未加入区块持有者的只读 `EmptyLevelChunk`，不会为它增加票据或保存它。区块系统自身仍可能维护加载阶段的 EMPTY 元数据；这不代表生成了地形。
- 不提供绕过限制或动态开启生成的命令。回退时移出本模组并重启。

## 安装和更新

1. 保存世界并停服。
2. 从 `mods/` 移出旧版 `mctg-terrainlock-mod-*.jar`；从 `plugins/` 移出旧版 `mctg-terrainlock-plugin-*.jar`。
3. 将发布页的 `mctg-terrainlock-1.1.0.jar` 放入 `mods/`。
4. 启动服务端，执行 `terrainlock status`，确认 `mode=single-mod`、主世界 `locked=true`，其他维度 `locked=false`。

不要同时加载新旧版本。原插件的数据目录可以保留，不参与新版本运行。

## 命令

命令需要原生命令权限等级 2 或以上，可由控制台执行。

```text
/terrainlock
/terrainlock status
/terrainlock audit
/terrainlock test <existingChunkX> <existingChunkZ> <absentChunkX> <absentChunkZ>
```

`status` 显示版本、运行形式、拦截计数、意外新增区块计数及各维度限制状态。

`audit` 检查八个目标类中的 13 个拦截方法是否真正注入；启动完成时也会自动执行，确保首次玩家登录前已验证移动和传送处理器。

`test` 是实际服务端集成验证：已有区块必须完整生成、当前尚未加载；另一个坐标必须没有生成地形。命令会短暂加载已有区块，然后验证同步、异步、插件加载、直接票据以及移动和传送策略。新增坐标不会被主动预生成。异步验证结果稍后输出。移动和传送验证检查共享拦截策略；不模拟真实客户端的数据包。

## 编译

本项目不包含 Minecraft、Youer、NeoForge 或 Bukkit 的 JAR。使用已安装服务端的依赖进行编译，避免混淆映射和核心 API 版本不一致。

```sh
python3 build.py --server-root /path/to/AsyncYouer/server
```

需要 Python 3.9+ 和 JDK 21+。指定目录下须存在 `libraries/com/mohistmc/installation/data/paper-remap.jar` 及其依赖。输出：

```text
dist/mctg-terrainlock-1.1.0.jar
dist/SHA256SUMS
dist/build-info.json
```

`build-info.json` 记录编译用核心 JAR 的 SHA-256。产物仅包含本模组的类和资源，不嵌入服务器依赖，也不包含 `plugin.yml`。

## 实现结构

- `TerrainLock`：区块存在性判定、维度范围和 NeoForge 生命周期事件。
- `TerrainLockCommands`：原生 Brigadier 命令、诊断和集成验证。
- `PlayerLoaderMixin`：保留加载队列，关闭主世界玩家生成权限。
- `SchedulerMixin`、`ChunkCacheMixin`、`TicketMixin`：限制异步、同步和直接票据入口。
- `CraftWorldMixin`：约束混合核心的 Bukkit 强制加载入口，无需独立插件。
- `MovementMixin`：在主线程处理移动数据包、载具移动和最终同世界传送。
- `PlayerTeleportMixin`、`EntityTeleportMixin`：限制跨世界玩家传送及实体传送。

## 运维注意

已有区块的判定结果会缓存至本次运行结束。在停服导入额外区块后重启，模组即可识别这些区块。运行中直接替换世界文件不属于支持的操作。

当前部署的验证结果见 [VERIFICATION.md](VERIFICATION.md)。

## 许可证

[MIT](LICENSE)。
