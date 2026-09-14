// v10.8: PURE scanline pictograph swallows — no clipPath; every line's endpoints lie exactly on the silhouette contour
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- pose polygons (same as G53/G54/G55 solid studies) ---
const POSES = {
  g53: [
    [[44,48],[24,36],[34,44],[48,52]],
    [[50,48],[72,32],[66,42],[52,52]],
    [[34,50],[52,46],[62,52],[56,58],[38,56]],
    [[38,56],[56,58],[52,63],[40,60]],
    [[30,52],[36,52],[34,57],[29,55]],
    [[28,52],[21,54],[29,55]],
    [[62,52],[76,56],[64,60]],
    [[60,56],[70,66],[58,62]],
  ],
  g54: [
    [[60,42],[34,28],[42,40],[50,48]],
    [[44,48],[70,44],[74,48],[70,52],[46,52]],
    [[46,52],[70,52],[68,57],[48,56]],
    [[68,48],[76,48],[74,54],[68,53]],
    [[70,44],[79,47.5],[71,51.5]],
    [[44,48],[24,42],[40,52]],
    [[44,52],[26,58],[42,56]],
  ],
  g55: [
    [[42,40],[30,30],[38,38],[48,44]],
    [[52,42],[70,28],[64,38],[50,44]],
    [[36,32],[50,42],[58,52],[44,46],[34,38]],
    [[40,38],[46,40],[44,46],[38,44]],
    [[44,46],[58,52],[52,58],[42,50]],
    [[56,52],[62,58],[58,72],[54,56]],
    [[58,54],[66,56],[66,72],[56,56]],
    [[60,52],[72,54],[72,66],[58,54]],
  ],
};

// --- scanline: for row y, even-odd intersections → spans; merge gaps < 3 ---
function scanRow(polys, y, mergeGap = 3) {
  const xs = [];
  for (const poly of polys) {
    for (let i = 0; i < poly.length; i++) {
      const [x1, y1] = poly[i], [x2, y2] = poly[(i + 1) % poly.length];
      if ((y1 <= y) !== (y2 <= y)) {
        xs.push(x1 + (y - y1) * (x2 - x1) / (y2 - y1));
      }
    }
  }
  xs.sort((a, b) => a - b);
  const spans = [];
  for (let i = 0; i + 1 < xs.length; i += 2) {
    if (spans.length && xs[i] - spans[spans.length - 1][1] < mergeGap) spans[spans.length - 1][1] = xs[i + 1];
    else spans.push([xs[i], xs[i + 1]]);
  }
  return spans.filter(s => s[1] - s[0] > 2.5);
}

function pictographLines(polys, y0, y1, n, colFn, sw) {
  const out = [];
  for (let i = 0; i < n; i++) {
    const y = y0 + (y1 - y0) * i / (n - 1);
    const spans = scanRow(polys, y);
    const col = colFn(i, y, spans);
    for (const [xa, xb] of spans) {
      out.push(`<path d="M${xa.toFixed(1)} ${y.toFixed(1)}L${xb.toFixed(1)} ${y.toFixed(1)}" stroke="${col}" stroke-width="${sw}" stroke-linecap="round"/>`);
    }
  }
  return out.join('\n    ');
}

// --- build six symbols ---
function buildPose(key) {
  const polys = POSES[key];
  const ys = polys.flat().map(p => p[1]);
  const y0 = Math.min(...ys) + 2.5, y1 = Math.max(...ys) - 2.5, n = 10;
  // mono: all ink + one red row (longest total span)
  let redRow = 0, best = -1;
  for (let i = 0; i < n; i++) {
    const y = y0 + (y1 - y0) * i / (n - 1);
    const tot = scanRow(polys, y).reduce((s, sp) => s + (sp[1] - sp[0]), 0);
    if (tot > best) { best = tot; redRow = i; }
  }
  const mono = pictographLines(polys, y0, y1, n, i => i === redRow ? 'var(--s3)' : 'var(--s1)', 4);
  // color: row-level plumage mapping (top=wing/back ink-or-blue, mid=body ink, bottom=belly cream, plus one rust row)
  const yMid = y0 + (y1 - y0) * 0.45;
  const color = pictographLines(polys, y0, y1, n, (i, y) => {
    const t = (y - y0) / (y1 - y0);
    if (i === Math.round(n * 0.35)) return 'var(--s3)';            // 喉行
    if (t > 0.72) return 'var(--s6)';                               // 腹行奶油
    if (t < 0.3) return 'var(--s2)';                                // 背翼行蓝
    return 'var(--s1)';
  }, 4);
  return { mono, color };
}

const built = {};
for (const key of ['g53', 'g54', 'g55']) built[key] = buildPose(key);

const newSymbols = `
  <!-- G53P/G53C 扫描线象形燕:每行端点精确落在姿态轮廓上,零裁剪 -->
  <symbol id="mk-g53p" viewBox="0 0 100 100">
    ${built.g53.mono}
  </symbol>
  <symbol id="mk-g53q" viewBox="0 0 100 100">
    ${built.g53.color}
  </symbol>

  <!-- G54P/G54C -->
  <symbol id="mk-g54p" viewBox="0 0 100 100">
    ${built.g54.mono}
  </symbol>
  <symbol id="mk-g54q" viewBox="0 0 100 100">
    ${built.g54.color}
  </symbol>

  <!-- G55P/G55C -->
  <symbol id="mk-g55p" viewBox="0 0 100 100">
    ${built.g55.mono}
  </symbol>
  <symbol id="mk-g55q" viewBox="0 0 100 100">
    ${built.g55.color}
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

// --- remove old clip-based pictograph symbols ---
for (const id of ['mk-g53m','mk-g53c','mk-g54m','mk-g54c','mk-g55m','mk-g55c']) {
  const a = h.indexOf('<symbol id="' + id + '"');
  if (a >= 0) {
    const b = h.indexOf('</symbol>', a) + '</symbol>'.length;
    h = h.slice(0, a) + h.slice(b);
  }
}

// --- remove old pictograph marks entries ---
for (const id of ['g53m','g53c','g54m','g54c','g55m','g55c']) {
  const re = new RegExp("\\s*\\{id:'" + id + "'[^}\\n]*\\},");
  h = h.replace(re, '');
}

// --- remove old pictograph matrix rows ---
for (const code of ['G53M','G53C','G54M','G54C','G55M','G55C']) {
  const re = new RegExp("\\s*\\['" + code + "'[^\\n]*\\],");
  h = h.replace(re, '');
}

// --- swap VARIANTS pictograph family to pure-scanline ---
r("['燕形横线象形 · 双配色', 'LINE PICTOGRAPH · 一图两配色', ['g53m', 'g53c', 'g54m', 'g54c', 'g55m', 'g55c']],",
"['燕形横线象形 · 纯扫描线', 'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', ['g53p', 'g53q', 'g54p', 'g54q', 'g55p', 'g55q']],");

// --- add new marks entries after g55 solid entry ---
r("{id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},",
`{id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},
  {id:'g53p', code:'G53P', name:'巡弋·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓 · 零裁剪', c:'G34 构造的忠实移植:每行横线的起止点由扫描线算法精确求得,落在姿态轮廓上——燕形由线的长短「长」出来,无任何裁切;最长行红。'},
  {id:'g53q', code:'G53Q', name:'巡弋·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版:背翼行蓝、腹行奶油、喉行朱砂——羽色按行映射。'},
  {id:'g54p', code:'G54P', name:'掠影·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓', c:'最修长姿态的扫描线象形:剪叉尾与收翼全由行的伸缩表达。'},
  {id:'g54q', code:'G54Q', name:'掠影·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版。'},
  {id:'g55p', code:'G55P', name:'归巢·扫描线', gene:'SCANLINE PICTOGRAPH · 端点即轮廓', c:'上扬宽 V 与下扇尾羽的扫描线象形;扇面深度由行间距自然呈现。'},
  {id:'g55q', code:'G55Q', name:'归巢·羽色扫描', gene:'SCANLINE PICTOGRAPH · 行级羽色', c:'同构配色版。'},`);

fs.writeFileSync(P, h);
console.log('v10.8 pure scanline pictograph swallows replaced clip-based ones');
