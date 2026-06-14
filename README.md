[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/u1xW62gh)

# Campus Dungeon

> 一款以校园答辩为主题的 Java 图形化随机地牢游戏。
> 在迷雾中探索未知地图，收集答辩材料，与 NPC 推进任务，解开课程谜题，最终进入答辩大厅完成软件工程实践答辩。

![Java](https://img.shields.io/badge/Java-8+-blue)
![Maven](https://img.shields.io/badge/Maven-Build-orange)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2ea44f)
![JUnit](https://img.shields.io/badge/Tests-JUnit4-brightgreen)
![Save](https://img.shields.io/badge/Save-JSON-lightgrey)

Campus Dungeon 最初来自一个 Zuul 风格的命令行文字冒险原型。原型验证了房间、道具、背包、NPC、任务门禁、评分和结局等基础机制；随后项目升级为 BYOG-inspired 的图形化随机地牢，让游戏拥有更强的可玩性、展示效果和工程测试价值。

## 游戏亮点

- **随机校园地牢**：输入 seed 后生成 `80 x 40` 的房间与走廊地图，同一 seed 可复现。
- **图形化探索**：使用 Java Swing 自研瓦片渲染，绘制墙体、地板、玩家、道具、NPC、答辩大厅和 HUD。
- **三态迷雾系统**：未探索区域全黑，已探索区域暗色显示，当前视野正常显示。
- **NPC 任务链**：`librarian`、`assistant`、`teacher` 会根据背包与任务状态给出不同反馈。
- **课程谜题机制**：通过 Maven 配置文件 `pom.xml` 谜题获得关键材料。
- **JSON 存档**：保存完整 `GameState`，包括玩家、地图、背包、道具、NPC、任务与探索状态。
- **输入回放测试**：保留 BYOG 风格 `playWithInputString`，核心玩法无需打开 GUI 即可自动化验证。
- **答辩通关闭环**：收集 `report`、`laptop`、`slides`、`pass` 后进入 defense hall 完成答辩。

## 游戏预览

当前版本已经具备可运行的 Swing 窗口和完整核心玩法。后续可在这里补充游戏窗口截图或演示 GIF，用于项目主页、课程报告和答辩视频。

```text
┌──────────────────────── Campus Dungeon ────────────────────────┐
│  黑暗迷雾覆盖未知区域，玩家从入口出发，逐步点亮校园地牢。       │
│  绿色道具、紫色 NPC、金色答辩大厅会在可见范围内呈现。           │
│  底部 HUD 显示 seed、步数、玩家位置、背包和最近提示。           │
└────────────────────────────────────────────────────────────────┘
```

## 快速开始

### 环境要求

- JDK：代码保持 Java 8 兼容。
- Maven：用于编译、测试和构建运行 classpath。

### 运行测试

```bash
mvn test
```

### 启动图形界面

项目使用 Gson 进行 JSON 存档，因此直接 `java -cp target/classes ...` 会缺少运行依赖。请先生成 Maven 依赖 classpath：

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes:$(cat target/runtime-classpath.txt)" cn.edu.whut.sept.dungeon.Main
```

使用指定 seed 启动：

```bash
java -cp "target/classes:$(cat target/runtime-classpath.txt)" cn.edu.whut.sept.dungeon.Main 20260614
```

后续打包任务可以把这一步替换为可执行 jar。

## 操作说明

| 操作 | 说明 |
|---|---|
| `W` / `A` / `S` / `D` | 上、左、下、右移动 |
| `E` | 与当前位置的道具、NPC 或答辩大厅交互 |
| `I` | 查看背包和状态提示 |
| `:Q` | 回放模式中保存并退出 |
| `L` | 回放模式中读取存档 |

回放输入支持：

```text
n<seed>s         开始新游戏
l                读取存档
w/a/s/d          移动
e                交互
i                查看背包/状态
:q               保存并退出
!answer(pom.xml) 回答 Maven 课程谜题
```

示例：

```java
GameResult result = new GameEngine().playWithInputString("n20260614sddae:q");
```

## 玩法目标

你的目标不是简单地跑图捡物品，而是完成一条校园答辩准备任务链：

```text
进入地牢
  -> 探索迷雾
  -> 找到 student-card 和 usb
  -> 与 librarian 交互获得 report
  -> 与 assistant 交互并回答 Maven 谜题
  -> 获得 laptop 和 slides
  -> 与 teacher 交互获得 pass
  -> 前往 defense hall 完成答辩
```

关键规则：

- `student-card` 是借出 `report` 的前置条件。
- `usb` 是导出演示材料的前置条件。
- `assistant` 会询问 Maven 配置文件名，正确答案是 `pom.xml`。
- `teacher` 会检查 `report + laptop + slides`，通过后发放 `pass`。
- `defense hall` 需要 `report + laptop + slides + pass` 才能通关。

通关后会显示完成软件工程实践答辩的提示和步数评价。

## 技术架构

项目将输入、规则、地图、实体、任务、渲染和存档拆分为清晰模块：

```text
GUI KeyListener / Replay Input
              |
          GameEngine
              |
          GameState
     ┌────────┼────────┬────────┐
   World    Entity    Quest    IO
     |        |         |       |
 Renderer  Items/NPC  Progress JSON Save
```

| 包 | 责任 |
|---|---|
| `cn.edu.whut.sept.dungeon.core` | 游戏引擎、输入解析、核心状态、移动规则、回放 API |
| `cn.edu.whut.sept.dungeon.world` | 瓦片、坐标、房间、走廊、seed 随机地图生成 |
| `cn.edu.whut.sept.dungeon.entity` | 道具、背包、NPC |
| `cn.edu.whut.sept.dungeon.quest` | 任务进度与通关状态 |
| `cn.edu.whut.sept.dungeon.render` | Swing 窗口、地图面板、瓦片渲染、HUD |
| `cn.edu.whut.sept.dungeon.io` | JSON 存档与读取 |

核心边界：

- GUI 只把键盘输入交给 `GameEngine.handleInput`。
- 自动化测试通过 `GameEngine.playWithInputString` 驱动游戏。
- 渲染层只读取 `GameState`，不直接修改游戏规则。
- 存档保存完整状态，而不是只保存 seed。

## 测试

运行：

```bash
mvn test
```

当前自动化测试覆盖：

- 同一 seed 生成相同世界。
- 不同 seed 生成不同地图。
- 房间中心与关键位置可达。
- 玩家移动、墙体碰撞、方向和步数。
- 迷雾 `UNSEEN / SEEN / VISIBLE` 三态。
- JSON 存档恢复玩家、地图、背包、道具、NPC、任务和探索状态。
- 道具拾取与背包展示。
- NPC 对话、任务推进和 Maven 谜题。
- 答辩大厅缺材料拒绝与材料齐全通关。
- 渲染器对玩家、未探索区域、已探索区域的颜色选择。

## 存档

默认存档路径：

```text
save/campus-dungeon-save.json
```

`save/` 已加入 `.gitignore`，本地游玩进度不会进入 Git。

## 项目演进

Campus Dungeon 并不是一开始就直接写成图形地牢。项目先完成了 `Campus Defense Zuul` 命令行原型，用来验证玩法方向和领域模型；随后从固定房间文字冒险升级为随机地牢。

这次升级保留了校园答辩主题，但把核心体验改成：

- 从固定地图升级为 seed 随机世界。
- 从命令行输出升级为 Swing 瓦片渲染。
- 从已知路线收集升级为迷雾探索与 NPC 任务。
- 从人工试玩升级为可回放输入和 JUnit 自动化验证。

当前主实现位于：

```text
src/main/java/cn/edu/whut/sept/dungeon
src/test/java/cn/edu/whut/sept/dungeon
```

## 开发记录

Campus Dungeon 功能主线：

| Issue | PR | 内容 |
|---|---|---|
| #19 | - | 项目从 Zuul 转向 Campus Dungeon 的决策记录 |
| #20 | #31 | 核心引擎与输入回放接口 |
| #21 | #32 | seed 随机世界生成 |
| #22 | #33 | 玩家移动、碰撞与基础视野 |
| #23 | #34 | Swing 瓦片渲染与 HUD |
| #24 | #35 | JSON 存档读取与 `:q` 保存退出 |
| #25 | #36 | 迷雾探索显示与探索状态持久化 |
| #26 | #37 | 道具、背包、门禁与答辩大厅通关 |
| #27 | #38 | NPC 线索、任务检查与课程谜题 |
| #30 | #39 | README 与项目说明更新 |

协作分工：

| 成员 | GitHub | 主线职责 |
|---|---|---|
| 成员 A | `siolyn` | 功能开发、核心架构、游戏规则实现 |
| 成员 B | `sand8-ui` | 测试、CI、打包、报告证据和 Review |

仓库仍保留课程协作痕迹：Issue 拆分、独立分支、Pull Request、测试证据和项目演进记录。这些记录既服务课程验收，也展示了项目从原型到图形化游戏的迭代过程。

## AI 辅助说明

本项目开发过程中使用 AI 工具辅助进行了课程要求解析、项目方向讨论、Issue 规划、部分文档整理、代码结构建议和验证命令设计。核心代码与文档均经过人工确认，并通过 Maven 测试、GitHub Issue/PR 流程和本地运行检查进行验证。
