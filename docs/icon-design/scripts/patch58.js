// v11.4: swallow versions OF THE G34 FAMILY ITSELF — chord rose with swallow-shaped hole + red eye dot with gap.
// G34SW 居中燕孔 / G34SB 咬边燕孔 / G34SO 偏心燕孔. Chords tangent to the swallow hull; eye floats in the head lobe.
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const fx = v => v.toFixed(1);

// swallow convex hull (5 key points: head top, wingtips, tail prong corners)
const HULL = [[50, 35], [64, 45], [58, 62], [42, 62], [36, 45]];

function chordsFor(hull, C) {
  const out = [];
  for (let k = 0; k < 9; k++) {
    const a = k * 20, nx = Math.cos(rad(a)), ny = Math.sin(rad(a));
    let d = -99;
    for (const [vx, vy] of hull) d = Math.max(d, (vx - C[0]) * nx + (vy - C[1]) * ny);
    d = Math.max(d, 4);
    const tx = -ny, ty = nx;
    const half = Math.sqrt(Math.max(26.5 * 26.5 - d * d, 4));
    const fxx = C[0] + d * nx, fxy = C[1] + d * ny;
    out.push({ x1: fxx + half * tx, y1: fxy + half * ty, x2: fxx - half * tx, y2: fxy - half * ty, k });
  }
  return out;
}
function render(chs, redK) {
  return chs.map(c => {
    const col = c.k === redK ? 'var(--s3)' : 'var(--s1)';
    return `<path d="M${fx(c.x1)} ${fx(c.y1)}L${fx(c.x2)} ${fx(c.y2)}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`;
  }).join('\n    ');
}
const shift = (dx, dy) => HULL.map(([x, y]) => [x + dx, y + dy]);

// SW: centered hole, eye at head lobe (50,42)
const sw = render(chordsFor(HULL, [50, 50]), 4);
// SB: hole shifted down 14 → tail prongs bite the rim (like G34B), eye at (50,56)
const sbHull = shift(0, 14);
const sb = render(chordsFor(sbHull, [50, 50]), 4);
// SO: hole shifted (-8,-6) eccentric (like G34O), eye at (42,36)
const soHull = shift(-8, -6);
const so = render(chordsFor(soHull, [50, 50]), 4);

const newSymbols = `
  <!-- G34SW 燕孔弦窗:G34 九弦构造,内包络=燕形凸壳(头/双翼尖/尾角),红点=眼睛悬浮头叶 -->
  <symbol id="mk-g34sw" viewBox="0 0 100 100">
    ${sw}
    <circle cx="50" cy="42" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G34SB 燕孔咬边:燕孔下移,尾尖咬破外圆(G34B 惯例),红眼悬浮 -->
  <symbol id="mk-g34sb" viewBox="0 0 100 100">
    ${sb}
    <circle cx="50" cy="56" r="2.2" fill="var(--s3)"/>
  </symbol>

  <!-- G34SO 燕孔偏心:燕孔偏左上(G34O 惯例),窥视有方位,红眼在头叶 -->
  <symbol id="mk-g34so" viewBox="0 0 100 100">
    ${so}
    <circle cx="42" cy="36" r="2.2" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// marks entries — append after g34o entry (G34 family)
r("{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},",
`{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},
  {id:'g34sw', code:'G34SW', name:'燕孔弦窗', gene:'G34 FAMILY · 内包络=燕形凸壳 · 红点=眼', c:'G34 家族本尊的燕形化:九弦构造原样(20° 扇/端点落外圆 r26.5/八墨一红),只把内包络从圆换成燕形凸壳——弦线切于头、双翼尖、尾角;红点=眼睛悬浮头叶,四周留白即眼白。'},
  {id:'g34sb', code:'G34SB', name:'燕孔咬边', gene:'G34 FAMILY · 燕孔下移 · 尾尖咬破外圆', c:'G34B 惯例的燕孔版:孔下移 14u,尾尖咬破外圆——剪叉尾在边界上完成;红眼悬浮孔心。'},
  {id:'g34so', code:'G34SO', name:'燕孔偏心', gene:'G34 FAMILY · 燕孔偏左上 · 窥视有方位', c:'G34O 惯例的燕孔版:孔偏左上,燕向巢外张望;红眼在头叶,偏心让「窥」有了方向。'},`);

// matrix rows
r("  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],",
`  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],
  ['G34SW','燕孔弦窗','燕形点睛',[5,5,5,4,4,5],0],
  ['G34SB','燕孔咬边','尾咬边界',[4,5,4,4,4,4],0],
  ['G34SO','燕孔偏心','方位窥视',[4,5,4,4,4,4],0],`);

// VARIANTS: append to G34 family list
r("'g34d', 'g34e', 'g34f', 'g34h', 'g34l', 'g34w', 'g34b']", "'g34d', 'g34e', 'g34f', 'g34h', 'g34l', 'g34w', 'g34b', 'g34sw', 'g34sb', 'g34so']");

fs.writeFileSync(P, h);
console.log('v11.4 three G34-family swallow-hole marks added');
