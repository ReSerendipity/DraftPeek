// v11: G57 燕蓄 — SINGLE VARIABLE change: G34R rose kept identical, only the hole profile becomes a swallow.
// Each chord's foot of perpendicular lands exactly on the swallow outline (ray-cast), so the swallow is ENVELOPED by the chords.
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

// swallow outline (8 vertices, centered on (50,50), max radius ~15.6): head up, wings out, forked tail down
const SW = [[50,36],[64,43],[55,48],[57,62],[50,54],[43,62],[45,48],[36,43]];
const C = 50, R = 26.5;

// ray-cast: first polygon intersection from C in direction a
function firstHit(a) {
  const dx = Math.cos(a), dy = Math.sin(a);
  let best = 99;
  for (let i = 0; i < SW.length; i++) {
    const [x1, y1] = SW[i], [x2, y2] = SW[(i + 1) % SW.length];
    const ex = x2 - x1, ey = y2 - y1;
    const den = dx * ey - dy * ex;
    if (Math.abs(den) < 1e-9) continue;
    const t = ((x1 - C) * ey - (y1 - C) * ex) / den;
    const u = ((x1 - C) * dy - (y1 - C) * dx) / den;
    if (t > 0.5 && u >= 0 && u <= 1 && t < best) best = t;
  }
  return Math.max(best, 5); // min 5 so no chord passes through exact center
}

let lines = [];
for (let k = 0; k < 9; k++) {
  const a = k * 20 * Math.PI / 180;
  const d = firstHit(a);
  const nx = Math.cos(a), ny = Math.sin(a);
  const fx = C + d * nx, fy = C + d * ny;           // chord foot = swallow boundary point
  const tx = -ny, ty = nx;                           // chord direction
  const half = Math.sqrt(R * R - d * d);
  const x1 = (fx + half * tx).toFixed(1), y1 = (fy + half * ty).toFixed(1);
  const x2 = (fx - half * tx).toFixed(1), y2 = (fy - half * ty).toFixed(1);
  const col = k === 4 ? 'var(--s3)' : 'var(--s1)';
  lines.push(`<path d="M${x1} ${y1}L${x2} ${y2}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
}

const sym = `
  <!-- G57 燕蓄:九弦玫瑰结原样保留,仅孔轮廓由圆改为燕——每弦垂足落在燕形轮廓上,燕由弦自己蓄出 -->
  <symbol id="mk-g57" viewBox="0 0 100 100">
    ${lines.join('\n    ')}
  </symbol>`;
h = h.replace('  <!-- G43 错位环', sym + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };
r("{id:'g56', code:'G56', name:'燕出于弦', gene:'THE SWALLOW AS ENVELOPE · 8 条边线延长成弦', c:'G34 几何表达燕形的终极形态:燕形轮廓的每条边本身就是一根弦线,向两端延长穿越整个圆盘——燕子作为纯负空间,被自己的边缘线围出来;剪叉尾的三条边全红。没有一个多余的元素:线即燕,燕即线。'},",
`{id:'g56', code:'G56', name:'燕出于弦', gene:'THE SWALLOW AS ENVELOPE · 8 条边线延长成弦', c:'G34 几何表达燕形的终极形态:燕形轮廓的每条边本身就是一根弦线,向两端延长穿越整个圆盘——燕子作为纯负空间,被自己的边缘线围出来;剪叉尾的三条边全红。没有一个多余的元素:线即燕,燕即线。'},
  {id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},`);
r("  ['G56','燕出于弦','负空间极致',[5,5,5,4,4,5],0],",
`  ['G56','燕出于弦','负空间极致',[5,5,5,4,4,5],0],
  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],`);
r("['g50c', 'g50b', 'g50a', 'g50d', 'g50e', 'g56']],", "['g50c', 'g50b', 'g50a', 'g50d', 'g50e', 'g56', 'g57']],");

fs.writeFileSync(P, h);
console.log('v11 G57 added — single variable: hole profile circle→swallow');
