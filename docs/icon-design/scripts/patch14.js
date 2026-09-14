// v9.3: four new math-system candidates, coordinates generated programmatically
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// --- G26 Sierpinski triangle: depth-2 recursion, 9 filled triangles, apex red ---
const T = [];
function sierp(ax, ay, bx, by, cx, cy, depth) {
  if (depth === 0) { T.push(`M${ax.toFixed(1)} ${ay.toFixed(1)}L${bx.toFixed(1)} ${by.toFixed(1)}L${cx.toFixed(1)} ${cy.toFixed(1)}Z`); return; }
  const m01 = [(ax + bx) / 2, (ay + by) / 2], m12 = [(bx + cx) / 2, (by + cy) / 2], m02 = [(ax + cx) / 2, (ay + cy) / 2];
  sierp(ax, ay, m01[0], m01[1], m02[0], m02[1], depth - 1);
  sierp(m01[0], m01[1], bx, by, m12[0], m12[1], depth - 1);
  sierp(m02[0], m02[1], m12[0], m12[1], cx, cy, depth - 1);
}
sierp(50, 20, 76, 65, 24, 65, 2);
// first path (apex subtree) red, rest ink
const g26paths = T.map((d, i) => `<path d="${d}" fill="${i === 0 ? 'var(--s3)' : 'var(--s1)'}"/>`).join('\n    ');

// --- G28 Lissajous 3:2 curve ---
let pts = [];
for (let i = 0; i <= 120; i++) {
  const t = (i / 120) * 2 * Math.PI;
  const x = 50 + 23 * Math.sin(3 * t + Math.PI / 2);
  const y = 50 + 26 * Math.sin(2 * t);
  pts.push(`${x.toFixed(1)} ${y.toFixed(1)}`);
}
const g28path = 'M' + pts.join('L') + 'Z';

// --- G29 golden-angle fan: 8 blades at 137.507° increments, lengths 18.2→26.6 ---
const fan = [];
for (let k = 1; k <= 8; k++) {
  const a = (90 + k * 137.507) * Math.PI / 180;
  const ro = 17 + 1.2 * k, ri = 9;
  const x1 = (50 + ri * Math.cos(a)).toFixed(1), y1 = (50 + ri * Math.sin(a)).toFixed(1);
  const x2 = (50 + ro * Math.cos(a)).toFixed(1), y2 = (50 + ro * Math.sin(a)).toFixed(1);
  let col = 'var(--s1)';
  if (k === 8) col = 'var(--s3)'; else if (k === 5) col = 'var(--s2)';
  fan.push(`<path d="M${x1} ${y1}L${x2} ${y2}" stroke="${col}" stroke-width="7" stroke-linecap="round"/>`);
}

const newSymbols = `
  <!-- G26 谢尔宾斯基三角:深度 2 递归,9 枚正三角,顶枚红 -->
  <symbol id="mk-g26" viewBox="0 0 100 100">
    ${g26paths}
  </symbol>

  <!-- G27 角无限环:双三角对顶,中缝 8u,红点悬于交叉点 -->
  <symbol id="mk-g27" viewBox="0 0 100 100">
    <path d="M26 36L46 50L26 64Z" fill="var(--s1)"/>
    <path d="M74 36L54 50L74 64Z" fill="var(--s2)"/>
    <circle cx="50" cy="50" r="4.2" fill="var(--s3)"/>
  </symbol>

  <!-- G28 利萨茹曲线:x=3:2 频率比闭合曲线,A23/B26,起终点红标 -->
  <symbol id="mk-g28" viewBox="0 0 100 100">
    <path d="${g28path}" fill="none" stroke="var(--s1)" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="73" cy="50" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G29 黄金角扇:8 枚放射刃按 137.507° 递增,外刃 17→26.6 渐长 -->
  <symbol id="mk-g29" viewBox="0 0 100 100">
    ${fan.join('\n    ')}
  </symbol>`;
h = h.replace('  <symbol id="mk-old"', newSymbols + '\n\n  <symbol id="mk-old"');

const r2 = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };
r2("{id:'g25', code:'G25', name:'分屏位移', gene:'SPLIT SHIFT · 双板错位 ±4u · 游标居间', c:'两块圆角板错位排布(左墨右红),中间一枚蓝色游标——左写右览的分屏对照。构成元素最少的一个候选。'},",
`{id:'g25', code:'G25', name:'分屏位移', gene:'SPLIT SHIFT · 双板错位 ±4u · 游标居间', c:'两块圆角板错位排布(左墨右红),中间一枚蓝色游标——左写右览的分屏对照。构成元素最少的一个候选。'},
  {id:'g26', code:'G26', name:'谢尔宾斯基三角', gene:'SIERPINSKI · 深度 2 递归 · 9 枚正三角', c:'递归分形的图标化:大正三角按谢尔宾斯基规则挖两轮,剩 9 枚小三角,顶枚红——「递归」是程序员一眼认出的数学,缺口本身构成倒三角负形。'},
  {id:'g27', code:'G27', name:'角无限环', gene:'ANGULAR LEMNISCATE · 双三角对顶 · 缝 8u', c:'莫比乌斯/无限环的角量化:两枚对顶三角中缝 8u,红点悬于交叉点——两条边、一个面、没有尽头的循环。'},
  {id:'g28', code:'G28', name:'利萨茹曲线', gene:'LISSAJOUS · 频率比 3:2 · 120 段折线逼近', c:'参数数学之美:x 与 y 以 3:2 频率振动,闭合曲线三次自交却一气呵成;红点标在起终点——所有振荡最终回到原点。'},
  {id:'g29', code:'G29', name:'黄金角扇', gene:'GOLDEN ANGLE FAN · 137.507° × 8 · 外刃渐长', c:'G9R 叶序点阵的放射版:8 枚圆头刃按黄金角递增排布,由于 137.507° 不可通约,任何两刃都不对齐——自然界的排布策略,红刃是最新的一片叶。'},`);

r2("  ['G25','分屏位移','分屏对照',[4,5,3,4,5,4],0],",
`  ['G25','分屏位移','分屏对照',[4,5,3,4,5,4],0],
  ['G26','谢尔宾斯基','递归分形',[4,4,5,3,4,5],0],
  ['G27','角无限环','拓扑循环',[4,4,5,5,4,4],0],
  ['G28','利萨茹曲线','参数数学',[3,3,5,4,3,4],0],
  ['G29','黄金角扇','黄金角放射',[4,4,5,4,4,5],0],`);

fs.writeFileSync(P, h);
console.log('v9.3 four math candidates added');
