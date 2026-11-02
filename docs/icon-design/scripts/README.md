# docs/icon-design/scripts — 一次性图标生成/审计脚本（DEV-ONLY）

> 本目录是 2026-09 图标设计过程中的**一次性脚本**（audit*.js / patch*.js / gen*.py 等），
> 用于在当时的 AI 工作区（`C:/Users/Doro/.qwenworkcn/...`）生成、修补与审计图标资源。

## 2026-11-02 归档调整

- G34R 专用 `gen_export.js` / `gen_pngs.py` 已移入 `../archive/g34r/scripts/`，不在本目录。
- `apply_export.js` 已改为仅应用 G34O：源为 `../icon-export-g34o/res`，目标为当前仓库 `app/src/main/res`；不要再指向归档的 G34R。
- `gen8.js` 仍是历史评审板生成器：模板保留 v9/v10 历史字样、输出到旧外部路径；仅作留档，不生成当前 G34O 资产。

## 说明

- **含本机绝对路径**：多数历史脚本内硬编码了生成时的 AI 工作区与项目绝对路径，**克隆到其他机器后不可直接运行**；`apply_export.js` 已改为当前仓库相对路径。
- **不参与构建**：这些脚本不属于应用代码，不会出现在任何构建产物中。
- **仅作历史留档**：如需复用其中的逻辑，请先参数化路径（改为相对路径 + 环境变量）再入库。

## 规范提醒

按 `docs/CODING_STANDARDS.md` 第 1 节：**新增入库脚本禁止硬编码本机绝对路径**。
一次性脚本也应参数化或标注 DEV-ONLY；本目录整体按 DEV-ONLY 留档处理。
