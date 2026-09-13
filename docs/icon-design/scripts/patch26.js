// v9.7: curve family — three beautified curved-line marks (mirroring G34R/X/O axes)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const px = (cx, cy, r, a) => (cx + r * Math.cos(rad(a))).toFixed(1);
const py = (cx, cy, r, a) => (cy + r * Math.sin(rad(a))).toFixed(1);

// --- G40 scallop ring: 8 outward-bulging arcs forming a wavy circle ---
const V = []; for (let k = 0; k < 8; k++) V.push([parseFloat(px(50, 50, 24, 90 + k * 45)), parseFloat(py(50, 50, 24, 90 + k * 45))]);
const c40 = 18.37, s40 = 6, Rs = ((c40 * c40 / 4) + s40 * s40) / (2 * s40);
const scallops = [];
for (let k = 0; k < 8; k++) {
  const a = V[k], b = V[(k + 1) % 8];
  let col = 'var(--s1)'; if (k === 0) col = 'var(--s3)'; else if (k === 4) col = 'var(--s2)';
  scallops.push(`<path d="M${a[0]} ${a[1]}A${Rs.toFixed(1)} ${Rs.toFixed(1)} 0 0 1 ${b[0]} ${b[1]}" fill="none" stroke="${col}" stroke-width="5.5" stroke-linecap="round"/>`);
}
const g40body = scallops.join('\n    ');

// --- G41 tapered crescent: single ribbon arc, width 9→1.5 over 200° sweep ---
const A = [210, 290, 370, 410];
const W = [9, 6.3, 3.6, 1];
const BASE = 24.5;
const o = A.map((a, i) => [parseFloat(px(50, 50, BASE + W[i] / 2, A[i] % 360)), parseFloat(py(50, 50, BASE + W[i] / 2, A[i] % 360))]);
const inn = A.map((a, i) => [parseFloat(px(50, 50, BASE - W[i] / 2, A[i] % 360)), parseFloat(py(50, 50, BASE - W[i] / 2, A[i] % 360))]);
const oR = [(o[0][1] === undefined ? 0 : (BASE + (W[0] + W[1]) / 4)), (BASE + (W[1] + W[2]) / 4), (BASE + (W[2] + W[3]) / 4)];
const iR = [(BASE - (W[0] + W[1]) / 4), (BASE - (W[1] + W[2]) / 4), (BASE - (W[2] + W[3]) / 4)];
const g41body = `<path fill="var(--s1)" d="M${o[0][0]} ${o[0][1]}A${oR[0].toFixed(1)} ${oR[0].toFixed(1)} 0 0 1 ${o[1][0]} ${o[1][1]}A${oR[1].toFixed(1)} ${oR[1].toFixed(1)} 0 0 1 ${o[2][0]} ${o[2][1]}A${oR[2].toFixed(1)} ${oR[2].toFixed(1)} 0 0 1 ${o[3][0]} ${o[3][1]}L${inn[3][0]} ${inn[3][1]}A${iR[2].toFixed(1)} ${iR[2].toFixed(1)} 0 0 0 ${inn[2][0]} ${inn[2][1]}A${iR[1].toFixed(1)} ${iR[1].toFixed(1)} 0 0 0 ${inn[1][0]} ${inn[1][1]}A${iR[0].toFixed(1)} ${iR[0].toFixed(1)} 0 0 0 ${inn[0][0]} ${inn[0][1]}Z"/>`;

// --- G42 vortex thirds: three 100° arcs, radii 26/20/14, staggered 120°+30° ---
const vArc = (r, a0, col) => `<path d="M${px(50, 50, r, a0)} ${py(50, 50, r, a0)}A${r} ${r} 0 0 1 ${px(50, 50, r, a0 + 100)} ${py(50, 50, r, a0 + 100)}" fill="none" stroke="${col}" stroke-width="8" stroke-linecap="round"/>`;
const g42body = [vArc(26, 15, 'var(--s1)'), vArc(20, 135, 'var(--s2)'), vArc(14, 255, 'var(--s3)')].join('\n    ');

const newSymbols = `
  <!-- G40 波瓣环:8 段外凸弧蓄成波浪圆,顶瓣红=预览中 -->
  <symbol id="mk-g40" viewBox="0 0 100 100">
    ${g40body}
  </symbol>

  <!-- G41 渐细新月:单条缎带弧,宽 9→1.5 跨 200°,开口朝左下 -->
  <symbol id="mk-g41" viewBox="0 0 100 100">
    ${g41body}
    <circle cx="45.5" cy="47.5" r="4" fill="var(--s3)"/>
  </symbol>

  <!-- G42 涡旋三弧:三段 100° 弧半径 26/20/14 逐级内旋,错位 120° -->
  <symbol id="mk-g42" viewBox="0 0 100 100">
    ${g42body}
  </symbol>`;
h = h.replace('  <!-- G34R 匀密九弦', newSymbols + '\n\n  <!-- G34R 匀密九弦');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- spec cards: insert after the G34 variants family block (before showcase marker) ---
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
function card(id, code, name, gene, concept, note) {
  const tiles = ['','on-ink','mono-layer'].map(c => `<span class="chip ${c}">${U(id)}</span>`).join('');
  const sizes = [96,48,29].map(s => `<figure><span class="chip" style="width:${s}px;height:${s}px;border-radius:var(--chip-r)">${U(id)}</span><figcaption>${s}px</figcaption></figure>`).join('');
  const floats = `<span class="float stage" style="width:120px;height:120px">${U(id)}</span><span class="float on-dark stage" style="width:120px;height:120px">${U(id)}</span>`;
  return `<article class="spec" id="s-${id}" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">${code}</span><div><h4>${name}</h4><p class="gene">${gene}</p></div><p class="concept">${concept}</p><div class="orig"><b>曲线注记</b> · ${note}</div></div>
  <div class="spec-grid">
    <figure class="bp"><svg viewBox="0 0 100 100"><rect width="100" height="100" fill="url(#bpgrid)"/>${U(id)}<use href="#guides" class="gd"/></svg><figcaption><span>GRID 8U</span><span>OPTICAL PASS ✓</span></figcaption></figure>
    <div class="simrows">
      <div class="sim"><span class="lab">FLOATING</span>${floats}</div>
      <div class="sim"><span class="lab">TILES</span>${tiles}</div>
      <div class="sim"><span class="lab">SIZES</span><div class="sizewrap">${sizes}</div></div>
    </div>
  </div>
</article>`;
}
const cards = [
  card('g40', 'G40', '波瓣环', 'SCALLOP RING · 8 段外凸弧 · 蓬高 6u', '曲线版的「弧蓄成圆」:8 段圆弧各自外凸 6u,连接处收成 8 个尖角,整体读作一朵波浪圆;顶瓣红=预览中的那一段,对侧蓝瓣平衡。', '①弧半径 10u 由弦长与蓬高解出(非随手画);②尖角处两弧相切级衔接,无断点;③红/蓝对角分配,重心平衡。'),
  card('g41', 'G41', '渐细新月', 'TAPERED CRESCENT · 缎带宽 9→1.5 · 跨 200°', 'G8 渐细螺旋的开放版:一条缎带弧从 9u 渐细到 1.5u,扫过 200° 停住,开口处悬一枚红点——「正在生成,未完待续」。宽度渐变即生长速率。', '①内外缘为三段不同半径的相切弧链,渐细连续;②红点悬在开口负空间中心;③纯单色缎带+红点,全场元素最少。'),
  card('g42', 'G42', '涡旋三弧', 'VORTEX THIRDS · 三段 100° · r26/20/14 内旋', '三段 100° 弧半径逐级内收、角位逐段前移 120°,形成向心的阶梯涡旋——比连续螺旋更几何,比同心环更有动势;内弧红色收束视线。', '①半径等差 6u、角位错位 120°+30°,旋进方向一致;②圆头端点的间隙均匀;③内弧红=定稿,外弧墨=草稿。'),
];
const anchor = h.indexOf('</article>', h.indexOf('id="s-g34o"'));
if (anchor < 0) { console.error('g34o article MISS'); process.exit(1); }
const close = anchor + '</article>'.length;
const family = [
  '',
  '  <div class="family-hd" data-component="family-header"><h3>曲线家族 · 三种弯线美化</h3><span class="caps mono">CURVE FAMILY</span></div>',
].concat(cards).join('\n');
h = h.slice(0, close) + family + h.slice(close);

// --- marks entries + matrix rows ---
r("{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},",
`{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},
  {id:'g40', code:'G40', name:'波瓣环', gene:'SCALLOP RING · 8 段外凸弧 · 蓬高 6u', c:'曲线版「弧蓄成圆」:8 段圆弧各自外凸成瓣,连接处收尖,整体是一朵波浪圆;顶瓣红。'},
  {id:'g41', code:'G41', name:'渐细新月', gene:'TAPERED CRESCENT · 缎带宽 9→1.5 · 200°', c:'一条渐细缎带弧扫过 200°,开口悬红点——「未完待续」的生成中。'},
  {id:'g42', code:'G42', name:'涡旋三弧', gene:'VORTEX THIRDS · 100° × 3 · r26/20/14 内旋', c:'三段弧半径逐级内收、角位前移,向心阶梯涡旋;内弧红。'},`);

r("  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],",
`  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],
  ['G40','波瓣环','弧蓄成圆',[5,4,4,5,4,4],0],
  ['G41','渐细新月','渐细动势',[4,4,5,4,4,4],0],
  ['G42','涡旋三弧','内旋构图',[4,4,5,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.7 curve family added');
