// 文档导出依赖的最小冒烟：证明 image-size 被钉到 2.x 之后，
// pptxgenjs 仍能加载、仍能生成可解开的 .pptx，且 image-size 自己的解析路径还活着。
//
// 为什么要有这个文件：package.json 用 overrides 把 image-size 从 pptxgenjs 声明的
// `^1.2.1` 强提到 `^2.0.4`（两条 high 级通告的首个修复版是 2.0.3，而 ^1.2.1 到不了 2.x，
// pptxgenjs@latest 又仍是 4.0.1/仍声明 ^1.2.1）⇒ 这是一次"绕过上游范围"的提版，
// 光看 npm ls 不足以说明没坏。本文件由 CI 的 Node 冒烟 job 真跑。
//
// 它**不**测 pptx 的视觉效果，也**不**替真机验收；只守三件事：
//   1. 版本闸门：解析到的 image-size 必须 >= 2.0.3，否则说明 override 被人摘了 ⇒ 红。
//   2. image-size 仍能把一张真 PNG 解析出宽高（通告影响的正是它的各格式解析器）。
//   3. pptxgenjs 走 addText + addImage 写出 .pptx → 文件非空、以 PK\x03\x04 开头
//      （.pptx 是 zip）。zip 条目由 CI 用 `unzip -l` 复核。
//
// 第 3 件事的定位要说清楚（本机实测，别当成"它在用 image-size"）：
// pptxgenjs@4.0.1 的 dist 里 `image-size` / `imageSize` 命中数都是 **0**，且它的
// `browser` 映射写着 `"image-size": false` ⇒ **运行期根本不经过这个包**，
// 它只是被声明在 dependencies 里。所以这一步是"防上游哪天改成用它"的回归护栏，
// 不是"验证它已被正确调用"。真正的替换风险由第 1、2 条承担。

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);

// 1x1 PNG，够用且无外部夹具依赖。
const PNG_1X1_B64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==';
const PNG_1X1 = Buffer.from(PNG_1X1_B64, 'base64');

function fail(msg) {
  console.error(`PPTX_SMOKE FAIL ${msg}`);
  process.exit(1);
}

function cmpSemver(a, b) {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < 3; i++) {
    if ((pa[i] || 0) !== (pb[i] || 0)) return (pa[i] || 0) > (pb[i] || 0) ? 1 : -1;
  }
  return 0;
}

// --- 1. 版本闸门：image-size 必须落在已修复的 2.0.3+ ---------------------------
// 注意不能用 require('image-size/package.json')：2.x 的 exports 图没暴露 package.json
// 子路径，会抛 ERR_PACKAGE_PATH_NOT_EXPORTED（本机实测）。⇒ 从入口文件往上找同名 package.json。
function pkgJsonOf(name) {
  let dir = path.dirname(require.resolve(name));
  for (let depth = 0; depth < 6; depth++) {
    const candidate = path.join(dir, 'package.json');
    if (fs.existsSync(candidate)) {
      const json = JSON.parse(fs.readFileSync(candidate, 'utf8'));
      if (json.name === name) return json;
    }
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  fail(`找不到 ${name} 的 package.json`);
}

const sizePkg = pkgJsonOf('image-size');
const MIN_PATCHED = '2.0.3'; // GHSA-5p2g-fcmc-qvqq / GHSA-w3rx-r6r6-pgpr 的 first_patched_version
if (cmpSemver(sizePkg.version, MIN_PATCHED) < 0) {
  fail(`image-size=${sizePkg.version} 低于修复版 ${MIN_PATCHED}（override 失效或被摘）`);
}
console.log(`PPTX_SMOKE image-size=${sizePkg.version} (>= ${MIN_PATCHED} OK)`);

// --- 2. image-size 解析路径仍可用 -------------------------------------------
const sizeMod = require('image-size');
const imageSize = sizeMod.imageSize ?? sizeMod.default ?? sizeMod;
if (typeof imageSize !== 'function') fail(`image-size 导出形态变了：${Object.keys(sizeMod)}`);
const dims = imageSize(PNG_1X1);
if (!dims || dims.width !== 1 || dims.height !== 1) {
  fail(`image-size 解析 1x1 PNG 结果不对：${JSON.stringify(dims)}`);
}
console.log(`PPTX_SMOKE imageSize(png) => width=${dims.width} height=${dims.height}`);

// --- 3. pptxgenjs 加载 + addImage + 写出 --------------------------------------
const pptxMod = require('pptxgenjs');
const PptxGenJS = pptxMod.default ?? pptxMod.PptxGenJS ?? pptxMod;
if (typeof PptxGenJS !== 'function') fail('pptxgenjs 默认导出不是构造器');
const pptx = new PptxGenJS();
const slide = pptx.addSlide();
slide.addText('DraftPeek dependency smoke', { x: 0.5, y: 0.5, w: 6, h: 1, fontSize: 18 });
// addImage 是本次提版唯一可能踩到的面（尺寸探测）。
slide.addImage({ data: `image/png;base64,${PNG_1X1_B64}`, x: 1, y: 2, w: 1, h: 1 });

const outArg = process.argv.indexOf('--out');
const outPath =
    outArg >= 0 && process.argv[outArg + 1]
      ? path.resolve(process.argv[outArg + 1])
      : path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'pptx-smoke-')), 'smoke.pptx');

// writeFile 不会替你建目录（本机实测：目录不存在时报错被 catch 吞掉，只留下二次崩的栈）。
fs.mkdirSync(path.dirname(outPath), { recursive: true });

try {
  await pptx.writeFile({ fileName: outPath });
} catch (e) {
  // 不再走"换个 API 重试"的路：pptxgenjs v4 的写出参数是 outputType（jszip 类型名），
  // 传错键会让 jszip 拿到 undefined 再抛 `type.toLowerCase is not a function`，
  // 把真实成因盖掉。红就红，把原文打出来。
  fail(`pptxgenjs writeFile 失败：${e && e.stack ? e.stack : e}`);
}

// 用 try/catch 包住 statSync，避免 existsSync+statSync 的 TOCTOU 竞态（CodeQL 门禁）。
let bytes;
try {
  bytes = fs.statSync(outPath).size;
} catch (e) {
  fail(`没写出文件：${outPath}`);
}
if (bytes <= 0) fail(`产出为空文件：${outPath}`);
const head = fs.readFileSync(outPath).subarray(0, 4);
if (!(head[0] === 0x50 && head[1] === 0x4b)) {
  fail(`产出不像 zip（前 4 字节 ${head.toString('hex')}）：${outPath}`);
}
console.log(`PPTX_SMOKE OK pptx=${bytes} bytes magic=${head.toString('hex')} file=${outPath}`);
