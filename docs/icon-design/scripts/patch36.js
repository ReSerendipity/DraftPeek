// v10.1: four more G34 envelope variants — two-pin, page-crop, migrating hole, clashing fans
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const fx = v => v.toFixed(1);

// --- G34D two-pin envelope: lines tangent alternately to two r7.5 pins ---
let dLines = [];
for (let k = 0; k < 10; k++) {
  const th = k * 18 * Math.PI / 180;
  const cx = k % 2 === 0 ? 40 : 60, cy = 50, r0 = 7.5, half = 25.9;
  const tx = cx + r0 * Math.cos(th), ty = cy + r0 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  let col = 'var(--s1)';
  if (k === 9) col = 'var(--s3)'; else if (k === 4) col = 'var(--s2)';
  dLines.push(`<path d="M${fx(tx + half * dx)} ${fx(ty + half * dy)}L${fx(tx - half * dx)} ${fx(ty - half * dy)}" stroke="${col}" stroke-width="3.8" stroke-linecap="round"/>`);
}
const g34d = dLines.join('\n    ');

// --- G34E page-cropped envelope: G34R family clipped by rounded-square page ---
let eLines = [];
for (let k = 0; k < 9; k++) {
  const th = k * 20 * Math.PI / 180;
  const tx = 50 + 12.5 * Math.cos(th), ty = 50 + 12.5 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  let col = 'var(--s1)';
  if (k === 4) col = 'var(--s3)'; else if (k === 1) col = 'var(--s2)';
  eLines.push(`<path d="M${fx(tx + 36 * dx)} ${fx(ty + 36 * dy)}L${fx(tx - 36 * dx)} ${fx(ty - 36 * dy)}" stroke="${col}" stroke-width="3.8" stroke-linecap="round"/>`);
}
const g34e = eLines.join('\n    ');

// --- G34F migrating hole: each line tangent to a drifting r8 circle ---
let fLines = [];
for (let k = 0; k < 9; k++) {
  const t = k / 8;
  const cx = 38 + 24 * t, cy = 42 + 16 * t, r0 = 8, th = k * 20 * Math.PI / 180, half = 26;
  const tx = cx + r0 * Math.cos(th), ty = cy + r0 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  let col = 'var(--s1)';
  if (k === 8) col = 'var(--s3)'; else if (k === 0) col = 'var(--s2)';
  fLines.push(`<path d="M${fx(tx + half * dx)} ${fx(ty + half * dy)}L${fx(tx - half * dx)} ${fx(ty - half * dy)}" stroke="${col}" stroke-width="3.8" stroke-linecap="round"/>`);
}
const g34f = fLines.join('\n    ');

// --- G34H clashing fans: two opposite fans clipped by main circle r27 ---
let hLines = [];
for (let k = 0; k < 4; k++) {
  const th = k * 22 * Math.PI / 180;
  const tx = 36 + 10 * Math.cos(th), ty = 50 + 10 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  let col = 'var(--s1)'; if (k === 3) col = 'var(--s2)';
  hLines.push(`<path d="M${fx(tx + 32 * dx)} ${fx(ty + 32 * dy)}L${fx(tx - 32 * dx)} ${fx(ty - 32 * dy)}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
}
for (let k = 0; k < 4; k++) {
  const th = (114 + k * 22) * Math.PI / 180;
  const tx = 64 + 10 * Math.cos(th), ty = 50 + 10 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  let col = 'var(--s1)'; if (k === 1) col = 'var(--s3)';
  hLines.push(`<path d="M${fx(tx + 32 * dx)} ${fx(ty + 32 * dy)}L${fx(tx - 32 * dx)} ${fx(ty - 32 * dy)}" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
}
const g34h = hLines.join('\n    ');

const newSymbols = `
  <clipPath id="cp-g34e"><rect x="21" y="21" width="58" height="58" rx="14"/></clipPath>
  <clipPath id="cp-g34h"><circle cx="50" cy="50" r="27"/></clipPath>

  <!-- G34D 双孔包络:弦线交替切于两枚 r7.5 钉圆,蓄出双孔 -->
  <symbol id="mk-g34d" viewBox="0 0 100 100">
    ${g34d}
  </symbol>

  <!-- G34E 方幅包络:切线弦族裁入圆角方幅,页面上鼓出圆 -->
  <symbol id="mk-g34e" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34e)">
    ${g34e}
    </g>
  </symbol>

  <!-- G34F 漂移孔包络:每弦切于逐级漂移的 r8 圆,包络孔滑动 -->
  <symbol id="mk-g34f" viewBox="0 0 100 100">
    ${g34f}
  </symbol>

  <!-- G34H 对撞双扇:左右两扇相向,主圆裁切,中带对撞 -->
  <symbol id="mk-g34h" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34h)">
    ${g34h}
    </g>
  </symbol>`;
h = h.replace('  <!-- G40 波瓣环', newSymbols + '\n\n  <!-- G40 波瓣环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// extend VARIANTS first family
r("['g34r', 'g34x', 'g34o']", "['g34r', 'g34x', 'g34o', 'g34d', 'g34e', 'g34f', 'g34h']");

// marks entries after g34o entry
r("{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},",
`{id:'g34o', code:'G34O', name:'偏心窥圆', gene:'LINE ENVELOPE · 偏心包络 + 主圆裁切', c:'G34 的构图精修:包络圆偏心,窥视有了方位感;最现代的不对称版本。'},
  {id:'g34d', code:'G34D', name:'双孔包络', gene:'TWO-PIN ENVELOPE · 弦线交替切于双钉 r7.5', c:'包络几何的经典玩法:弦线交替切于两枚「图钉」,内缘蓄出两个小孔——编辑与预览两枚钉,缝线缠绕其间。'},
  {id:'g34e', name:'方幅包络', code:'G34E', gene:'PAGE-CROPPED ENVELOPE · 弦族裁入圆角方幅', c:'同样的切线弦族,裁进圆角方幅而非圆——「一页纸」上鼓出一个圆:装裱方式的改变彻底改变气质,方与圆互相成全。'},
  {id:'g34f', code:'G34F', name:'漂移孔包络', gene:'MIGRATING ENVELOPE · 每弦切于漂移 r8 圆', c:'九根弦各切于一颗逐级漂移的「图钉」,包络孔从左上滑向右下——窥视是移动的,一串连续动作被叠进一个静帧。'},
  {id:'g34h', code:'G34H', name:'对撞双扇', gene:'CLASHING FANS · 左右双扇相向 · 主圆裁切', c:'两组切线扇从左右相向推进,在对撞带交错出莫尔条纹般的张力——密度冲突产生能量,圆是它们的竞技场。'},`);

// matrix rows after G34O row
r("  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],",
`  ['G34O','偏心窥圆','不对称精修',[5,4,5,4,4,4],0],
  ['G34D','双孔包络','双钉缠线',[4,5,5,4,4,5],0],
  ['G34E','方幅包络','页面装裱',[4,4,4,5,4,4],0],
  ['G34F','漂移孔包络','动势包络',[4,4,5,4,4,5],0],
  ['G34H','对撞双扇','密度冲突',[4,4,4,5,3,4],0],`);

fs.writeFileSync(P, h);
console.log('v10.1 four more envelope variants added');
