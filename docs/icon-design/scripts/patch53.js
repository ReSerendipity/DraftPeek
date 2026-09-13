// v11.1: 3 poses × 3 line styles = 9 marks (horizontal rows / flight-axis rows / contour stroke)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');

const POSES = {
  g53: { polys: [
      [[44,48],[24,36],[34,44],[48,52]],
      [[50,48],[72,32],[66,42],[52,52]],
      [[34,50],[52,46],[62,52],[56,58],[38,56]],
      [[38,56],[56,58],[52,63],[40,60]],
      [[30,52],[36,52],[34,57],[29,55]],
      [[28,52],[21,54],[29,55]],
      [[62,52],[76,56],[64,60]],
      [[60,56],[70,66],[58,62]],
    ], axis: -20 },
  g54: { polys: [
      [[60,42],[34,28],[42,40],[50,48]],
      [[44,48],[70,44],[74,48],[70,52],[46,52]],
      [[46,52],[70,52],[68,57],[48,56]],
      [[68,48],[76,48],[74,54],[68,53]],
      [[70,44],[79,47.5],[71,51.5]],
      [[44,48],[24,42],[40,52]],
      [[44,52],[26,58],[42,56]],
    ], axis: -15 },
  g55: { polys: [
      [[42,40],[30,30],[38,38],[48,44]],
      [[52,42],[70,28],[64,38],[50,44]],
      [[36,32],[50,42],[58,52],[44,46],[34,38]],
      [[40,38],[46,40],[44,46],[38,44]],
      [[44,46],[58,52],[52,58],[42,50]],
      [[56,52],[62,58],[58,72],[54,56]],
      [[58,54],[66,56],[66,72],[56,56]],
      [[60,52],[72,54],[72,66],[58,54]],
    ], axis: -35 },
};

function scanSpans(polys, y, mergeGap = 3) {
  const xs = [];
  for (const poly of polys)
    for (let i = 0; i < poly.length; i++) {
      const [x1, y1] = poly[i], [x2, y2] = poly[(i + 1) % poly.length];
      if ((y1 <= y) !== (y2 <= y)) xs.push(x1 + (y - y1) * (x2 - x1) / (y2 - y1));
    }
  xs.sort((a, b) => a - b);
  const spans = [];
  for (let i = 0; i + 1 < xs.length; i += 2) {
    if (spans.length && xs[i] - spans[spans.length - 1][1] < mergeGap) spans[spans.length - 1][1] = xs[i + 1];
    else spans.push([xs[i], xs[i + 1]]);
  }
  return spans.filter(s => s[1] - s[0] > 2.5);
}
function rotatePoly(polys, deg) {
  const c = Math.cos(rad(deg)), s = Math.sin(rad(deg));
  return polys.map(poly => poly.map(([x, y]) => [50 + (x - 50) * c - (y - 50) * s, 50 + (x - 50) * s + (y - 50) * c]));
}
function rad(d) { return d * Math.PI / 180; }
const fx = v => v.toFixed(1);

function seg(x1, y1, x2, y2, col, sw = 4) {
  return `<path d="M${fx(x1)} ${fx(y1)}L${fx(x2)} ${fx(y2)}" stroke="${col}" stroke-width="${sw}" stroke-linecap="round"/>`;
}

// style builders: return {body, redNote}
function styleHorizontal(polys) {
  const ys = polys.flat().map(p => p[1]);
  const y0 = Math.min(...ys) + 2.5, y1 = Math.max(...ys) - 2.5, n = 10;
  let best = 0, redI = 0, rowsArr = [];
  for (let i = 0; i < n; i++) {
    const y = y0 + (y1 - y0) * i / (n - 1);
    const spans = scanSpans(polys, y);
    const tot = spans.reduce((s, sp) => s + sp[1] - sp[0], 0);
    rowsArr.push([y, spans]); if (tot > best) { best = tot; redI = i; }
  }
  const body = rowsArr.map(([y, spans], i) => spans.map(([xa, xb]) => seg(xa, y, xb, y, i === redI ? 'var(--s3)' : 'var(--s1)')).join('')).join('\n    ');
  return { body, note: '最长总跨度的那行红=预览行' };
}
function styleAxis(polys, axisDeg) {
  const rot = rotatePoly(polys, axisDeg);
  const { body } = styleHorizontal(rot);
  return { body, note: `线沿飞行轴 ${axisDeg}° 平行排布,速度感` };
}
function styleContour(polys) {
  // largest polygon = main outline
  const poly = polys.reduce((a, b) => {
    const area = pts => { let s = 0; for (let i = 0; i < pts.length; i++) { const [x1, y1] = pts[i], [x2, y2] = pts[(i + 1) % pts.length]; s += x1 * y2 - x2 * y1; } return Math.abs(s / 2); };
    return area(b) > area(a) ? b : a;
  });
  let worst = 0, redI = 0;
  for (let i = 0; i < poly.length; i++) {
    const [x1, y1] = poly[i], [x2, y2] = poly[(i + 1) % poly.length];
    const d = Math.hypot(x2 - x1, y2 - y1); if (d > worst) { worst = d; redI = i; }
  }
  const outline = poly.map(([x, y], i) => (i === 0 ? 'M' : 'L') + fx(x) + ' ' + fx(y)).join('') + 'Z';
  const body = `<path d="${outline}" fill="none" stroke="var(--s1)" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>` +
    poly.map(([x, y], i) => {
      const [x2, y2] = poly[(i + 1) % poly.length];
      return i === redI ? seg(x, y, x2, y2, 'var(--s3)', 4.5) : '';
    }).join('');
  return { body, note: '最长边红=翼/尾的主笔画' };
}

// build 9 symbols
let symbols = '';
const ids = [];
for (const key of ['g53', 'g54', 'g55']) {
  const { polys, axis } = POSES[key];
  const up = key.toUpperCase();
  const s1 = styleHorizontal(polys);
  const s2 = styleAxis(polys, axis);
  const s3 = styleContour(polys);
  const defs = [
    [`${key}h1`, `S1 · 水平行`, s1.body],
    [`${key}h2`, `S2 · 斜行束 ${axis}°`, s2.body],
    [`${key}h3`, `S3 · 轮廓线描`, s3.body],
  ];
  for (const [sid, label, body] of defs) {
    symbols += `
  <symbol id="mk-${sid}" viewBox="0 0 100 100">
    ${body}
  </symbol>`;
    ids.push(sid);
  }
}
const symBlock = `
  <!-- 三姿态 × 三线式:每行端点落在姿态轮廓上,零填充零裁剪 -->
${symbols}`;
h = h.replace('  <!-- G43 错位环', symBlock + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

r("{id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},",
`{id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},
  {id:'g53h1', code:'G53·S1', name:'巡弋·水平行', gene:'POSE g53 × 水平行 · 端点即轮廓', c:'写生①姿态的水平行扫描:行行端点落在展翼剪影上,最长行红。'},
  {id:'g53h2', code:'G53·S2', name:'巡弋·斜行束', gene:'POSE g53 × 斜行束 -20°', c:'线沿飞行轴平行排布,速度感;端点仍落在轮廓上。'},
  {id:'g53h3', code:'G53·S3', name:'巡弋·轮廓线描', gene:'POSE g53 × 轮廓线描', c:'一根线勾出展翼燕形,最长边(翼)红。'},
  {id:'g54h1', code:'G54·S1', name:'掠影·水平行', gene:'POSE g54 × 水平行 · 端点即轮廓', c:'写生②姿态的水平行扫描,最修长剪影。'},
  {id:'g54h2', code:'G54·S2', name:'掠影·斜行束', gene:'POSE g54 × 斜行束 -15°', c:'线沿飞行轴平行排布。'},
  {id:'g54h3', code:'G54·S3', name:'掠影·轮廓线描', gene:'POSE g54 × 轮廓线描', c:'一根线勾出侧滑燕形,最长边红。'},
  {id:'g55h1', code:'G55·S1', name:'归巢·水平行', gene:'POSE g55 × 水平行 · 端点即轮廓', c:'写生③姿态的水平行扫描,扇尾分层自然呈现。'},
  {id:'g55h2', code:'G55·S2', name:'归巢·斜行束', gene:'POSE g55 × 斜行束 -35°', c:'线沿俯冲轴平行排布,俯冲感。'},
  {id:'g55h3', code:'G55·S3', name:'归巢·轮廓线描', gene:'POSE g55 × 轮廓线描', c:'一根线勾出归巢燕形,最长边红。'},`);

r("  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],",
`  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],
  ['G53S1','巡弋水平行','横线象形',[4,4,4,4,4,4],0],
  ['G53S2','巡弋斜行束','斜线速度',[4,4,4,4,4,4],0],
  ['G53S3','巡弋轮廓','线描燕形',[4,4,4,4,4,4],0],
  ['G54S1','掠影水平行','横线象形',[4,4,4,4,4,4],0],
  ['G54S2','掠影斜行束','斜线速度',[4,4,4,4,4,4],0],
  ['G54S3','掠影轮廓','线描燕形',[4,4,4,4,4,4],0],
  ['G55S1','归巢水平行','横线象形',[4,4,4,4,4,4],0],
  ['G55S2','归巢斜行束','斜线俯冲',[4,4,4,4,4,4],0],
  ['G55S3','归巢轮廓','线描燕形',[4,4,4,4,4,4],0],`);

// VARIANTS family
r("['燕形横线象形 · 纯扫描线', 'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', ['g53p', 'g53q', 'g54p', 'g54q', 'g55p', 'g55q']],",
"['燕形横线象形 · 纯扫描线', 'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', ['g53p', 'g53q', 'g54p', 'g54q', 'g55p', 'g55q']],\n  ['三姿态 × 三线式', 'POSE × LINESTYLE · 水平行 / 斜行束 / 轮廓线描', ['g53h1', 'g53h2', 'g53h3', 'g54h1', 'g54h2', 'g54h3', 'g55h1', 'g55h2', 'g55h3']],");

fs.writeFileSync(P, h);
console.log('v11.1 nine pose×style marks added');
