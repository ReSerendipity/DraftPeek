// v9.5: five math/CS challengers — line envelope, git graph, interference, hilbert, morse
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// --- G34 line envelope: 7 chords tangent to r12 circle, chords of r27 disc ---
const env = [];
for (let k = 0; k < 7; k++) {
  const th = (k * 20) * Math.PI / 180;
  const tx = 50 + 12 * Math.cos(th), ty = 50 + 12 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  const half = 24.19;
  const x1 = (tx + half * dx).toFixed(1), y1 = (ty + half * dy).toFixed(1);
  const x2 = (tx - half * dx).toFixed(1), y2 = (ty - half * dy).toFixed(1);
  let col = 'var(--s1)';
  if (k === 3) col = 'var(--s2)'; else if (k === 6) col = 'var(--s3)';
  env.push(`<path d="M${x1} ${y1}L${x2} ${y2}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
}
const g34body = env.join('\n    ');

// --- G37 Hilbert curve order-2 on 4x4 grid ---
const hp = [];
function hilbert(x0, y0, xi, xj, yi, yj, n) {
  if (n <= 0) { hp.push([x0 + (xi + yi) / 2, y0 + (xj + yj) / 2]); return; }
  hilbert(x0, y0, yi / 2, yj / 2, xi / 2, xj / 2, n - 1);
  hilbert(x0 + xi / 2, y0 + xj / 2, xi / 2, xj / 2, yi / 2, yj / 2, n - 1);
  hilbert(x0 + xi / 2 + yi / 2, y0 + xj / 2 + yj / 2, xi / 2, xj / 2, yi / 2, yj / 2, n - 1);
  hilbert(x0 + xi / 2 + yi, y0 + xj / 2 + yj, -yi / 2, -yj / 2, -xi / 2, -xj / 2, n - 1);
}
hilbert(0, 0, 1, 0, 0, 1, 2);
const S = 13, OX = 30.5, OY = 69.5;
const hpts = hp.map(p => [(OX + p[0] * S).toFixed(1), (OY - p[1] * S).toFixed(1)]);
let g37d = 'M' + hpts.map(p => p[0] + ' ' + p[1]).join('L');
const g37first = hpts[0], g37last = hpts[hpts.length - 1];

const newSymbols = `
  <!-- G34 直线包络:7 条弦切于 r12 内圆(角距 20°),直线蓄出圆 -->
  <symbol id="mk-g34" viewBox="0 0 100 100">
    ${g34body}
  </symbol>

  <!-- G35 分支合并:主干 + 右支弧线,三节点(出发蓝/分支墨/合并红) -->
  <symbol id="mk-g35" viewBox="0 0 100 100">
    <path d="M50 76V22" stroke="var(--s1)" stroke-width="6.5" stroke-linecap="round"/>
    <path d="M50 58H60Q68 58 68 50V42Q68 34 60 34H50" fill="none" stroke="var(--s2)" stroke-width="6.5" stroke-linecap="round"/>
    <circle cx="50" cy="58" r="5" fill="var(--s2)"/>
    <circle cx="68" cy="50" r="4" fill="var(--s1)"/>
    <circle cx="50" cy="34" r="5.5" fill="var(--s3)"/>
  </symbol>

  <!-- G36 双缝干涉:左右两组弧波相向推进,中心红点=叠加最亮处 -->
  <symbol id="mk-g36" viewBox="0 0 100 100">
    <g fill="none" stroke="var(--s1)" stroke-width="4">
      <path d="M36.4 42.3A10 10 0 0 1 36.4 57.7"/>
      <path d="M40.9 37A17 17 0 0 1 40.9 63"/>
      <path d="M45.4 31.6A24 24 0 0 1 45.4 68.4"/>
    </g>
    <g fill="none" stroke="var(--s2)" stroke-width="4">
      <path d="M63.6 42.3A10 10 0 0 0 63.6 57.7"/>
      <path d="M59.1 37A17 17 0 0 0 59.1 63"/>
      <path d="M54.6 31.6A24 24 0 0 0 54.6 68.4"/>
    </g>
    <circle cx="50" cy="50" r="4.5" fill="var(--s3)"/>
  </symbol>

  <!-- G37 希尔伯特曲线:order-2 递归一笔遍历 4x4 全格 -->
  <symbol id="mk-g37" viewBox="0 0 100 100">
    <path d="${g37d}" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="${g37first[0]}" cy="${g37first[1]}" r="3.8" fill="var(--s3)"/>
    <circle cx="${g37last[0]}" cy="${g37last[1]}" r="3.8" fill="var(--s2)"/>
  </symbol>

  <!-- G38 摩尔斯节奏:点·点·划·划·点,信号本身的线性韵律 -->
  <symbol id="mk-g38" viewBox="0 0 100 100">
    <circle cx="25" cy="50" r="3.2" fill="var(--s1)"/>
    <circle cx="34" cy="50" r="3.2" fill="var(--s1)"/>
    <rect x="40" y="47" width="11" height="6" rx="3" fill="var(--s1)"/>
    <rect x="54" y="47" width="11" height="6" rx="3" fill="var(--s1)"/>
    <circle cx="71" cy="50" r="3.2" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <symbol id="mk-old"', newSymbols + '\n\n  <symbol id="mk-old"');

const r2 = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };
r2("{id:'g33', code:'G33', name:'阶梯塔', gene:'ZIGGURAT · 三层收窄 · 40/30/20u', c:'三层阶梯逐级收窄(墨/蓝/红)——从草稿到成稿的逐级构建,编译进度的纪念碑形态。'},",
`{id:'g33', code:'G33', name:'阶梯塔', gene:'ZIGGURAT · 三层收窄 · 40/30/20u', c:'三层阶梯逐级收窄(墨/蓝/红)——从草稿到成稿的逐级构建,编译进度的纪念碑形态。'},
  {id:'g34', code:'G34', name:'直线包络', gene:'LINE ENVELOPE · 7 弦切于 r12 · 角距 20°', c:'九条等宽直线只因角度逐根旋变,就蓄出一枚圆——「纯文本渲染出圆滑预览」的产品逻辑被几何直接证明;蓝线破序,红线点睛。'},
  {id:'g35', code:'G35', name:'分支合并', gene:'GIT GRAPH · 主干 + 右支弧线 · 三节点', c:'主干、分支、合并:三枚节点两段线构成 Git 的最小语法树,合并点红。DraftPeek 自带 Git 功能,语义零翻译成本。'},
  {id:'g36', code:'G36', name:'双缝干涉', gene:'INTERFERENCE · 左右弧波相向 · 叠加最亮', c:'物理学的双缝实验抽象:左右两组弧波相向推进,叠加中心红点最亮——两份内容对比之处,正是洞察产生之地。'},
  {id:'g37', code:'G37', name:'希尔伯特曲线', gene:'HILBERT · order-2 递归 · 一笔遍历 4x4', c:'一条线按希尔伯特次序遍历全部 16 格,不重复、不遗漏、一笔画完——「轻览全部」的空间填满;红点起点、蓝点终点。'},
  {id:'g38', code:'G38', name:'摩尔斯节奏', gene:'MORSE RHYTHM · 点点划划点 · 信号原点', c:'点、点、划、划、点——摩尔斯是「编码」一词最古老的形态;删掉字母,只留信号本身的线性韵律。收尾红点=信号到达。'},`);

r2("  ['G33','阶梯塔','逐级构建',[3,4,4,4,4,4],0],",
`  ['G33','阶梯塔','逐级构建',[3,4,4,4,4,4],0],
  ['G34','直线包络','切线成圆',[5,5,5,4,4,5],2],
  ['G35','分支合并','版本图谱',[4,5,4,4,4,5],0],
  ['G36','双缝干涉','对比成亮',[4,4,5,4,3,4],0],
  ['G37','希尔伯特','遍历全文',[4,4,5,4,4,5],0],
  ['G38','摩尔斯节奏','编码原点',[4,4,4,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.5 five math/CS challengers added');
