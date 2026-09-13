// v9.6: three G34 refinements — uniform density / accelerating fan / off-center peek
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (x, y) => { if (!h.includes(x)) { console.error('MISS:', String(x).slice(0, 70)); process.exitCode = 1; } else h = h.split(x).join(y); };

// --- chord generator ---
function chords(cx, cy, r0, angles, half) {
  return angles.map(th => {
    const t = th * Math.PI / 180;
    const tx = cx + r0 * Math.cos(t), ty = cy + r0 * Math.sin(t);
    const dx = -Math.sin(t), dy = Math.cos(t);
    return { x1: (tx + half * dx).toFixed(1), y1: (ty + half * dy).toFixed(1), x2: (tx - half * dx).toFixed(1), y2: (ty - half * dy).toFixed(1) };
  });
}
function lineEl(c, col, sw) { return `<path d="M${c.x1} ${c.y1}L${c.x2} ${c.y2}" stroke="${col}" stroke-width="${sw}" stroke-linecap="round"/>`; }

// G34R: 9 uniform chords, full 160° fan, k4 (horizontal top) red = previewed line, k1 blue
const R = chords(50, 50, 12.5, [0, 20, 40, 60, 80, 100, 120, 140, 160], 23.7);
const g34r = R.map((c, k) => {
  let col = 'var(--s1)';
  if (k === 4) col = 'var(--s3)'; else if (k === 1) col = 'var(--s2)';
  return lineEl(c, col, 3.8);
}).join('\n    ');

// G34X: accelerating fan — angular gaps 12→30u, opening motion toward the red end
const XA = [0, 12, 26, 44, 66, 92, 120, 150];
const X = chords(50, 50, 12.5, XA, 23.7);
const g34x = X.map((c, k) => {
  let col = 'var(--s1)';
  if (k === 7) col = 'var(--s3)'; else if (k === 3) col = 'var(--s2)';
  return lineEl(c, col, 4);
}).join('\n    ');

// G34O: off-center envelope — chords tangent to (59,41) r10.5, clipped by main circle r27
const O = chords(59, 41, 10.5, [10, 30, 50, 70, 90, 110, 130, 150, 170], 36);
const g34o = O.map((c, k) => {
  let col = 'var(--s1)';
  if (k === 3) col = 'var(--s2)';
  return lineEl(c, col, 3.6);
}).join('\n    ');

const newSymbols = `
  <!-- G34R 匀密九弦:9 弦均布 20° 全扇,顶部横线红=预览中 -->
  <symbol id="mk-g34r" viewBox="0 0 100 100">
    ${g34r}
  </symbol>

  <!-- G34X 开角疏密:角距 12→30 渐扩,疏端红=览的出口 -->
  <symbol id="mk-g34x" viewBox="0 0 100 100">
    ${g34x}
  </symbol>

  <!-- G34O 偏心窥圆:弦切于 (59,41) r10.5 偏心圆,外缘裁入主圆 r27 -->
  <symbol id="mk-g34o" viewBox="0 0 100 100">
    <clipPath id="cp-g34o"><circle cx="50" cy="50" r="27"/></clipPath>
    <g clip-path="url(#cp-g34o)">
    ${g34o}
    </g>
    <circle cx="59" cy="41" r="4" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G35', newSymbols + '\n\n  <!-- G35');

// --- spec cards (rendered, inserted after s-g34 article) ---
const U = id => `<svg viewBox="0 0 100 100"><use href="#mk-${id}"/></svg>`;
function card(id, code, name, gene, concept, note) {
  const tiles = ['','on-ink','mono-layer'].map(c => `<span class="chip ${c}">${U(id)}</span>`).join('');
  const sizes = [96,48,29].map(s => `<figure><span class="chip" style="width:${s}px;height:${s}px;border-radius:var(--chip-r)">${U(id)}</span><figcaption>${s}px</figcaption></figure>`).join('');
  const floats = `<span class="float stage" style="width:120px;height:120px">${U(id)}</span><span class="float on-dark stage" style="width:120px;height:120px">${U(id)}</span>`;
  return `<article class="spec" id="s-${id}" data-component="icon-spec-refined">
  <div class="spec-hd"><span class="code">${code}</span><div><h4>${name}</h4><p class="gene">${gene}</p></div><p class="concept">${concept}</p><div class="orig"><b>精修注记</b> · ${note}</div></div>
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
  card('g34r', 'G34R', '匀密九弦', 'LINE ENVELOPE · 9 弦均布 20° · 全扇覆盖', '把 7 弦 120° 的覆盖补全为 9 弦 160°,包络圆不再有暗缺角;顶部横线改为红色——「正在被预览的那一行」,蓝色第二弦保持破序。密度拉满后,中孔在 29px 依然成圆。', '①7→9 弦,角距 20° 全扇;②红色从端部移到顶部横线,语义归位;③线宽 4→3.8 配合密度。'),
  card('g34x', 'G34X', '开角疏密', 'LINE ENVELOPE · 角距 12→30 渐扩 · 开放动势', '角距按 12/14/18/22/26/28/30 渐扩:左密右疏,像一把缓缓打开的折扇——密处是「撰」,疏端红弦是「览」的出口。同样弦长下,不均匀节奏带来动势。', '①角度序列非均匀化(12→30 渐扩);②红色移至最疏端,「打开」的方向叙事;③其余保持纯墨,只留一枚蓝。'),
  card('g34o', 'G34O', '偏心窥圆', 'LINE ENVELOPE · 弦切偏心圆 · 裁入主圆 r27', '包络圆偏移到右上 (59,41),直线族裁入主圆——圆孔不在正中,窥视有了方位感;红点悬在偏心孔心。三种变体里最现代、最不对称的一版。', '①包络圆偏心至 (59,41) r10.5;②整族直线裁入主圆 r27,外缘成干净的圆;③红点=窥视焦点,悬于孔心。'),
];

const anchorArt = h.indexOf('</article>', h.indexOf('<article class="spec" id="s-g34"'));
const close = anchorArt + '</article>'.length;
h = h.slice(0, close) + '\n' + cards.join('\n') + h.slice(close);

// --- marks entries + matrix rows ---
r("{id:'g34', code:'G34', name:'直线包络', gene:'LINE ENVELOPE · 7 弦切于 r12 · 角距 20°', c:'九条等宽直线只因角度逐根旋变,就蓄出一枚圆——「纯文本渲染出圆滑预览」的产品逻辑被几何直接证明;蓝线破序,红线点睛。'},",
`{id:'g34', code:'G34', name:'直线包络', gene:'LINE ENVELOPE · 7 弦切于 r12 · 角距 20°', c:'九条等宽直线只因角度逐根旋变,就蓄出一枚圆——「纯文本渲染出圆滑预览」的产品逻辑被几何直接证明;蓝线破序,红线点睛。'},
  {id:'g34r', code:'G34R', name:'匀密九弦', gene:'LINE ENVELOPE · 9 弦均布 20° · 全扇覆盖', c:'G34 的密度精修:覆盖补全、红线归位到顶部横线,中孔 29px 成圆。'},
  {id:'g34x', code:'G34X', name:'开角疏密', gene:'LINE ENVELOPE · 角距 12→30 渐扩', c:'G34 的动势精修:疏密渐扩如折扇缓开,密处撰、疏端览。'},
  {id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},`);

r("  ['G34','直线包络','切线成圆',[5,5,5,4,4,5],2],",
`  ['G34','直线包络','切线成圆',[5,5,5,4,4,5],0],
  ['G34R','匀密九弦','包络精修',[5,5,4,5,4,5],2],
  ['G34X','开角疏密','动势精修',[4,4,5,4,4,4],0],
  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.6 G34 three refinements added');
