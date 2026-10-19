# 编码与 Git 规范（Coding & Git Standards）

> 本文件是仓库内所有入库内容的**可移植性、依赖与 Git 卫生**约束。任何修改请在提交前通读并对照检查。
> 最后更新：2026-10-14（新增第 7 节「交付体规范单一来源」，承接原先只写在本地 `AGENTS.md` 里的门禁/测试/混淆规范）。

## 1. 路径可移植性（强制）

### 1.1 禁止

- 在入库文件中硬编码本机绝对路径：`C:\Users\<用户名>\...`、`/home/<用户名>/...`、`/Users/<用户名>/...`
- 在 pip 依赖清单里写 `file:///C:/...` 本机 wheel 路径
- 构建 / 打包 / 安装脚本里写死本机工具路径（如 `C:\Users\...\7za.exe`、`C:\Program Files (x86)\Microsoft Visual Studio\<版本>\...\vcvars64.bat`）

### 1.2 正确姿势

| 场景 | 做法 |
|---|---|
| Python 生产代码 | `Path(__file__).resolve().parents[N]` 推导项目根；外部路径用环境变量 + 项目内默认值 |
| PowerShell 脚本 | `$PSScriptRoot` 推导脚本所在目录 |
| 批处理 | `%~dp0` 推导脚本所在目录 |
| NSIS 安装器 | 相对脚本目录的路径 + `!ifndef/!define` 允许 `-D` 覆盖 |
| pip 依赖 | 官方索引 URL（如 `https://download.pytorch.org/whl/cu130/...`）或普通版本声明，禁止本机文件路径 |
| 数据 / 配置文件 | 不写本机路径；含路径的历史数据需在文档标注，不新增 |

### 1.3 本机专用（DEV-ONLY）运维脚本

仅本机使用的运维 / 开发辅助脚本（检查其他仓库 venv、清理本机数据等）：

- 文件头必须标注：`DEV-ONLY` + 用途 + 是否含本机路径
- 尽量参数化（环境变量 / 命令行参数）；不能参数化的必须明示"克隆后不可直接用"

### 1.4 提交前检查

PowerShell：

```powershell
# 全库搜索本机路径痕迹（排除文档与锁定文件后人工复核）
rg -n -i "C:\\Users\\|/home/|/Users/" --glob '!*.md' --glob '!*.lock' .
```

Python 项目：

```bash
grep -rn --include='*.py' -iE 'C:\\Users|/home/|/Users/' .
```

自动化门禁（已接入 `.githooks/pre-commit`，提交时自动执行）：

```bash
# 提交时拦截新引入的本机绝对路径；--all 全库审计（人工复核用）
python scripts/check_no_hardcoded_paths.py          # 默认：git diff --cached
python scripts/check_no_hardcoded_paths.py --all    # 全库
```

> 规则：占位符（`/home/user`、`C:\Users\me` 等）与豁免清单（`Dockerfile` 容器路径、`locales/` 占位文案、`.env.example`、测试断言文件等）见脚本头部注释；新增豁免必须人工复核后加入 `ALLOWLIST` 并注明理由。

## 2. Git 工作流（强制）

- main 交付闸门（口径以 `docs/ci/branch-protection.json` 的实测记录 `_measured_at` / `_meets_reality` 为准）：**非 admin 账号禁止直推**，流程为 `git fetch` → 从 `origin/main` 建分支 → 修改 → `git commit -s`（DCO）→ push（过 pre-push 门禁）→ GitHub PR + 1 审批 + 三项必需检查（`Build & Test` / `layout` / `Lock Integrity (server lock resolvability)`）。**仓库 owner/admin 账号因 `enforce_admins=false` 免 PR 直推 main，该路径不经过远端必需检查**——所以直推前唯一阻断点是本地门禁：`.githooks` 的 pre-commit / pre-push 与 `docs/README.md`「Server 改动验证门禁」要求的 `cd server && pytest tests -q`，必须全绿才可提交，不得假设「CI 会拦」。本条与 live 是否漂移用只读命令核查：`python scripts/apply_branch_protection.py`（**不加 `--apply`**）报 `[OK] 无漂移` 即一致，报 `[DRIFT]` 即须回填 `branch-protection.json` 并同步本条。
- 提交信息使用 Conventional Commits 风格：`feat: / fix: / chore: / docs: / refactor: / test:`；同一仓库内语言保持一致。
- 钩子存放在 `.githooks/` 并随仓库分发（仓库自包含），克隆后启用：

  ```bash
  git config core.hooksPath .githooks
  ```

- `.gitattributes` 统一 `text eol=lf`；`.mailmap` 用于历史身份归并（新增身份先加映射）。
- 合并 PR 后删除源分支；恢复期保护分支（backup / pre-recovery-* 等）由所有者确认后删除，不擅自清理。

## 3. 仓库自包含（强制）

- 不依赖"家族 / 公用"仓库的内容：工作流、钩子、脚本必须随本仓库分发，保证**克隆后即可工作**。
- 已知例外（需逐步消除）：CI 引用 `ReSerendipity/.github` 的 self-purify.yml / python-quality-baseline.yml。新增工作流禁止再引用外部仓库文件。
- 不使用 git submodule / symlink 传递必要内容。

## 4. 卫生与安全

- 密钥、`.env*`、本地数据库 / 产物不入库（见 `.gitignore`）。
- 大文件（>10MB）不入库，已有大资产走 LFS 或移出仓库。
- 不提交生成物（build/、dist/、target/、__pycache__/、*.log）。
- 修改 `.gitattributes` / `.gitignore` / `.mailmap` 前先确认与上游 `origin/main` 一致，避免重复 / 冲突提交。

## 5. 本仓修复记录（2026-09-18）

第二轮（检查脚本落地 + 首轮遗漏补齐）：

- `scripts/check_no_hardcoded_paths.py`：新增路径可移植性检查脚本，已接入 `.githooks/pre-commit`（提交时 `git diff --cached` 拦截）。
- `scripts/check_spec_refs.py`：docstring 中的本机路径说明改为通用表述（首轮遗漏，本轮补齐）。

首轮（PR #69）：

- 本次扫描**未发现**生产代码 / 构建脚本硬编码执行路径。全库命中均为：
  - 第三方 textmate 语法定义（`*.tmLanguage.json` 正则中的 `\d`、`\w` 等转义，非路径）；
  - 测试用例构造输入（如 `C:/imgs/a.png`、`/etc/passwd` 等安全测试样例）；
  - README 教学示例（含 `YourName` 占位符）。
  以上均不构成可移植性风险，无需处理；后续按第 1 节规则执行即可。

## 6. 关联文档

- `docs/RELEASE_CHECKLIST.md`、`docs/VERSIONING.md`
- `docs/README.md`（目录速览 + 分发边界说明）

## 7. 交付体规范单一来源（fresh clone 权威入口）

> 新增于 2026-10-14。此前下列条目只写在**本地维护、不随仓库分发**的 `AGENTS.md` 中，而入库文件（脚本注释、proguard 规则、测试类 KDoc、基线 JSON）却把它们指向该文件，导致 fresh clone / CI 里取不到规范出处。
> **本节是这些规范在版本库内的唯一权威落点**：入库文件引用规范时只允许指向本节、本节列出的配置文件，或机器可验证的脚本/workflow；**禁止**以 `AGENTS.md` 作为规范出处，也**禁止**在 `.gitignore` 等处声称存在并不存在的豁免。
> 存量范围：`docs/release-version-management-evaluation.md`、`docs/reports/*`、`CHANGELOG.md` 等**已定稿的历史叙述**里提及 `AGENTS.md` 的语句不作改写（改写会 falsify 当时的评估现场）；它们只作归因，不作规范出处。

### 7.1 分发边界（`AGENTS.md` 家族 = 仅本地保留）

- 裁决（2026-09-19，2026-10-14 复核维持）：根 `AGENTS.md`、`feature/editor/AGENTS.md`、`core/common/AGENTS.md`、`docs/agents/**`、`FIX_LOG.md`、`LOCAL_RULES.md` **仅本地保留、有意不入库**。
- 机制与事实一致：`.gitignore` 的 `*.md` 通配（`docs/agents/` 另有目录级规则）覆盖该家族，**没有任何 `!` 豁免指向 AGENTS.md**。可校验：

  ```bash
  git check-ignore -v AGENTS.md          # 期望命中 .gitignore 的 *.md 规则
  git ls-files -- '*AGENTS.md'           # 期望为空
  ```

- 后果：fresh clone 中没有 `AGENTS.md` 属**预期行为**，不得视为缺失，也不得以「补齐 AGENTS.md」为由新建该文件。agent / 新协作者的上下文落点依次为 `docs/README.md` → 本文件 → 根 `README.md` + `.github/**`。
- 短引用约定：入库文件（`build.gradle.kts`、`codecov.yml`、测试 KDoc、基线 JSON notes 等）里出现的 `GOTCHAS #nn` / `#DP-nn` 一律指本地维护、**不随仓库分发**的 `docs/agents/GOTCHAS.md`，仅作历史事故归因；该结论的**可执行部分**必须能在本节或机器可验证的配置里取到。新写引用时推荐直接写成 `docs/agents/GOTCHAS.md #nn（本地文档，未随仓库发布）`。

### 7.2 覆盖率门禁 = 质量门禁

| 规则 | 权威落点（均在版本库内） |
|---|---|
| 阈值单一事实来源，禁止在他处写死数字 | `coverage-baseline.json` → `modules[*].threshold_pct` |
| ratchet：`threshold_pct` 只升不降，下降即 CI 红 | `python scripts/check_coverage_gate.py --ratchet`；CI 步骤见 `.github/workflows/android.yml` |
| 报告缺失即失败，不允许「无数据即绿」 | `scripts/check_coverage_gate.py` 退出码 1 |
| 新增模块必须同时进门禁 | 同一 PR 内写入 `coverage-baseline.json` + `codecov.yml` |
| 实测明显上升须同 PR 上调至新实测 ≈90%（软警：超 `measured_pct` ≥1pt 且超 `threshold_pct` ≥3pt） | `coverage-baseline.json` notes 与 `scripts/check_coverage_gate.py` |
| 禁止为过 CI 拆改报告（改 `classDirectories`/`executionData`、用 `\|\| echo` 吞错） | 各模块 `build.gradle.kts` JaCoCo 配置 + `.github/workflows/android.yml` |
| 确需下调阈值（如移除大模块） | 先写 ADR（`docs/adr/`），再同步 `coverage-baseline.json` 与 workflow |

### 7.3 单元测试平台 / 注解约定

- Robolectric 测试类（`@RunWith(RobolectricTestRunner::class)`）使用 **JUnit4** 风格：扁平 `@Test`，不使用 `@Nested` / `@DisplayName` 等 JUnit5 注解。
- **禁止**同一模块内混用 JUnit5 注解与 JUnit4 Runner（`useJUnitPlatform()` 无 vintage 引擎时，JUnit4 用例会被静默跳过 → CI 假绿、覆盖率失真）。该规则由入库脚本 `scripts/check_junit_platform.py` 静态拦截。
- 任务名：仅 `:app` 有 flavor 维度（`testDevDebugUnitTest` / `lintDevDebug`）；`feature/*`、`core/*` 无 flavor，单变体任务名为 `testDebugUnitTest`。

### 7.4 R8 / ProGuard 模式

- `android.enableR8.fullMode=false`（见 `gradle.properties`）：R8 **Full Mode 会破坏 Apache POI / JGit 的反射链**，本工程固定使用 Standard Mode。
- 仅关闭 `!field/marking/final` 优化（见 `app/proguard-rules.pro`），其余优化（方法内联、死代码删除、控制流简化）正常启用。

---

## 8. Markdown 写作规范

所有 .md 文件遵循以下约定（2026-09-19 家族统一新增）：

- **编码**：UTF-8 **无 BOM**。禁止保存为带 BOM 的 UTF-8、UTF-16 或 GBK。
- **行尾**：统一 LF（.gitattributes 已锁 *.md text eol=lf），禁止 CRLF 入库。
- **行宽**：正文建议 ≤ 120 字符；URL、表格、代码块、长 JSON 不强制折行。
- **标题**：每个文件仅一个 #（一级标题），标题层级不跳级（# → ## → ###）。
- **中英文混排**：中文字符与英文/数字之间加一个半角空格（例：使用 ruff check .、Python 3.12）。
- **文件名**：公开文档优先英文 kebab-case（如 developer-guide.md）；存量中文文件名保留，新文档同一目录内风格保持一致。日期后缀统一用 -YYYYMMDD（如 exec-checklist-20260910.md）。
- **换行**：markdown 行尾两个空格表示硬换行；不要用 \ 转义。
- **链接**：相对路径引用仓库内文件，禁止引用本地绝对路径（C:\...、/home/...）。
