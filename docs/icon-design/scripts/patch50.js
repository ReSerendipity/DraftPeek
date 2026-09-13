// v10.9: G50D/E — the faithful G34 construction applied to the swallow:
// chord family between INNER envelope (core circle r9) and OUTER envelope (swallow contour), rays at 15° fan
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;

// swallow silhouette polygons (same as photo-study poses, merged set)
const POLYS = [
  [[44,48],[24,36],[34,44],[48,52]],
  [[50,48],[72,32],[66,42],[52,52]],
  [[34,50],[52,46],[62,52],[56,58],[38,56]],
  [[38,56],[56,58],[52,63],[40,60]],
  [[30,52],[36,52],[34,57],[29,55]],
  [[28,52],[21,54],[29,55]],
  [[62,52],[76,56],[64,60]],
  [[60,56],[70,66],[58,62]],
];
const OX = 46, OY = 52, CORE = 9, MAXR = 28.4;

// ray-cast: farthest polygon intersection along direction a (deg), from O
function castFar(a) {
  const dx = Math.cos(rad(a)), dy = Math.sin(rad(a));
  let best = 0;
  for (const poly of POLYS) {
    for (let i = 0; i < poly.length; i++) {
      const [x1, y1] = poly[i], [x2, y2] = poly[(i + 1) % poly.length];
      const ex = x2 - x1, ey = y2 - y1;
      const den = dx * ey - dy * ex;
      if (Math.abs(den) < 1e-9) continue;
      const t = ((x1 - OX) * ey - (y1 - OY) * ex) / den;       // along ray
      const u = ((x1 - OX) * dy - (y1 - OY) * dx) / den;       // along edge
      if (t > 0 && u >= 0 && u <= 1) best = Math.max(best, t);
    }
  }
  return Math.min(best, MAXR);
}

// ray fan: G34-style uniform 15° over the right/upper sweep + two head rays
const angles = [];
for (let a = -85; a <= 95; a += 15) angles.push(a);
angles.push(140, 158); // head/beak rays

function fanLines(colorFn) {
  const out = [];
  for (const a of angles) {
    const R = Math.max(castFar(a), CORE + 2);
    const col = colorFn(a, R);
    out.push(`<path d="M${(OX + CORE * Math.cos(rad(a))).toFixed(1)} ${(OY + CORE * Math.sin(rad(a))).toFixed(1)}L${(OX + R * Math.cos(rad(a))).toFixed(1)} ${(OY + R * Math.sin(rad(a))).toFixed(1)}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
  }
  return out.join('\n    ');
}

// mono: all ink, wingtip ray (-45) red, tail ray (60) blue? keep: red at wingtip ray
const g50d = fanLines(a => a === -40 ? 'var(--s3)' : 'var(--s1)');
// color: rays colored by region — wing rays blue, tail rays red, head ray rust, body ink
const g50e = fanLines(a => {
  if (a >= -70 && a <= -20) return 'var(--s2)';   // 翼羽行蓝
  if (a >= 20 && a <= 95) return 'var(--s3)';      // 尾羽行红
  if (a >= 130) return 'var(--s5)';                // 头喙行琥珀
  return 'var(--s1)';
});

const newSymbols = `
  <!-- G50D 羽轴包络燕:弦线族介于体芯圆 r9 与燕形轮廓之间,15° 扇 —— G34 构造的燕形移植 -->
  <symbol id="mk-g50d" viewBox="0 0 100 100">
    ${g50d}
  </symbol>

  <!-- G50E 羽轴包络燕·羽色:同构,弦线按翼/尾/头分区配色 -->
  <symbol id="mk-g50e" viewBox="0 0 100 100">
    ${g50e}
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// marks entries after g50a entry
r("{id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},",
`{id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},
  {id:'g50d', code:'G50D', name:'羽轴包络燕', gene:'CHORD FAMILY · 内包络体芯 r9 / 外包络燕形轮廓 · 15° 扇', c:'G34 构造对燕形的忠实移植:弦线族介于体芯圆与燕形轮廓两层包络之间,15° 均匀扇——翼尖、剪叉缺口、腹线全由弦线端点落在轮廓上自然表达,零裁剪零填充;翼尖羽红。'},
  {id:'g50e', code:'G50E', name:'羽轴包络·羽色', gene:'CHORD FAMILY · 同构 · 按翼/尾/头分区配色', c:'同构配色版:翼区弦线蓝、尾区弦线红、头喙弦线琥珀、体弦墨——羽色按弦线所属身体分区映射。'},`);

// matrix rows after G50A row
r("  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],",
`  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],
  ['G50D','羽轴包络燕','双层包络',[5,5,5,4,4,5],2],
  ['G50E','羽轴包络·羽色','分区配色',[5,4,4,5,4,4],0],`);

// VARIANTS: extend swallow lineart family
r("['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 放射', ['g50c', 'g50b', 'g50a']],",
"['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 包络', ['g50c', 'g50b', 'g50a', 'g50d', 'g50e']],");

fs.writeFileSync(P, h);
console.log('v10.9 faithful G34-construction swallow added (ray-cast between two envelopes)');
