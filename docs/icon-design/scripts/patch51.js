// v10.9b: G56 — the swallow AS the envelope: each edge of the angular swallow, extended into a full chord, clipped to the main circle
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// angular swallow outline (from G50), scaled 0.5 about (48,47) then re-centered
const V = [[24,64],[34,53],[66,28],[58,52],[64,58],[78,48],[72,68],[50,66]]
  .map(([x, y]) => [50 + (x - 48) * 0.5, 50 + (y - 47) * 0.5]);

// edge lines → clipped to circle r27 about (50,50)
const R = 27;
let lines = [];
for (let i = 0; i < V.length; i++) {
  const [x1, y1] = V[i], [x2, y2] = V[(i + 1) % V.length];
  const dx = x2 - x1, dy = y2 - y1;
  const len = Math.hypot(dx, dy); const ux = dx / len, uy = dy / len;
  const mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
  // param range where point stays inside circle r=R: solve |M + t·u − C|² = R²
  const ox = mx - 50, oy = my - 50;
  const b = 2 * (ox * ux + oy * uy), c = ox * ox + oy * oy - R * R;
  const disc = b * b - 4 * c;
  if (disc < 0) continue;
  const t1 = (-b - Math.sqrt(disc)) / 2, t2 = (-b + Math.sqrt(disc)) / 2;
  const ax = (mx + t1 * ux).toFixed(1), ay = (my + t1 * uy).toFixed(1);
  const bx = (mx + t2 * ux).toFixed(1), by = (my + t2 * uy).toFixed(1);
  // colors: tail-fork edges (index 4..6) red, wingtip edge (index 2) blue, rest ink
  let col = 'var(--s1)';
  if (i >= 4 && i <= 6) col = 'var(--s3)'; else if (i === 2) col = 'var(--s2)';
  lines.push(`<path d="M${ax} ${ay}L${bx} ${by}" stroke="${col}" stroke-width="3.8" stroke-linecap="round"/>`);
}

const sym = `
  <!-- G56 燕出于弦:燕形轮廓的 8 条边各自延长成整弦,燕子=纯负空间,由自己的边缘线围出 -->
  <symbol id="mk-g56" viewBox="0 0 100 100">
    ${lines.join('\n    ')}
  </symbol>`;
h = h.replace('  <!-- G43 错位环', sym + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

r("{id:'g50e', code:'G50E', name:'羽轴包络·羽色', gene:'CHORD FAMILY · 同构 · 按翼/尾/头分区配色', c:'同构配色版:翼区弦线蓝、尾区弦线红、头喙弦线琥珀、体弦墨——羽色按弦线所属身体分区映射。'},",
`{id:'g50e', code:'G50E', name:'羽轴包络·羽色', gene:'CHORD FAMILY · 同构 · 按翼/尾/头分区配色', c:'同构配色版:翼区弦线蓝、尾区弦线红、头喙弦线琥珀、体弦墨——羽色按弦线所属身体分区映射。'},
  {id:'g56', code:'G56', name:'燕出于弦', gene:'THE SWALLOW AS ENVELOPE · 8 条边线延长成弦', c:'G34 几何表达燕形的终极形态:燕形轮廓的每条边本身就是一根弦线,向两端延长穿越整个圆盘——燕子作为纯负空间,被自己的边缘线围出来;剪叉尾的三条边全红。没有一个多余的元素:线即燕,燕即线。'},`);

r("  ['G50E','羽轴包络·羽色','分区配色',[5,4,4,5,4,4],0],",
`  ['G50E','羽轴包络·羽色','分区配色',[5,4,4,5,4,4],0],
  ['G56','燕出于弦','负空间极致',[5,5,5,4,4,5],0],`);

// VARIANTS: add to swallow lineart family
r("['g50c', 'g50b', 'g50a', 'g50d', 'g50e']],", "['g50c', 'g50b', 'g50a', 'g50d', 'g50e', 'g56']],");

fs.writeFileSync(P, h);
console.log('v10.9b G56 swallow-as-envelope added');
