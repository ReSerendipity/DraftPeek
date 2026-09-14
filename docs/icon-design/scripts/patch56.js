// v11.2: composite marks — G34W/G34B chord structure + G53-family swallow parts + red eye dot (点睛)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// G34W chords (square-hole support, center (56,44), rot 20°, half 26, 9 lines) — ink, thin
let wCh = [];
for (let k = 0; k < 9; k++) {
  const th = rad2(k * 20);
  const d = 9 * Math.max(Math.abs(Math.cos(th - rad2(20))), Math.abs(Math.sin(th - rad2(20))));
  const tx = 56 + d * Math.cos(th), ty = 44 + d * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  wCh.push(`M${(tx + 26 * dx).toFixed(1)} ${(ty + 26 * dy).toFixed(1)}L${(tx - 26 * dx).toFixed(1)} ${(ty - 26 * dy).toFixed(1)}`);
}
function rad2(d) { return d * Math.PI / 180; }

// G34B chords (tangent to r11 circle at (64,44), 9 lines) — ink, thin
let bCh = [];
for (let k = 0; k < 9; k++) {
  const th = rad2(k * 20);
  const tx = 64 + 11 * Math.cos(th), ty = 44 + 11 * Math.sin(th);
  const dx = -Math.sin(th), dy = Math.cos(th);
  bCh.push(`M${(tx + 26 * dx).toFixed(1)} ${(ty + 26 * dy).toFixed(1)}L${(tx - 26 * dx).toFixed(1)} ${(ty - 26 * dy).toFixed(1)}`);
}

// G53 swallow parts scaled 0.8 about (50,50), facing left
const S53 = {
  farWing: 'M45.2 48.4L29.2 38.8L37.2 45.2L48.4 51.6Z',
  nearWing: 'M50 48.4L67.6 35.6L62.8 43.6L51.6 51.6Z',
  body: 'M37.2 50L51.6 46.8L59.6 51.6L54.8 56.4L40.4 54.8Z',
  belly: 'M40.4 54.8L54.8 56.4L51.6 60.4L42 58Z',
  head: 'M34 51.6L38.8 51.6L37.2 55.6L33.2 54Z',
  beak: 'M32.4 51.6L26.8 53.2L33.2 54Z',
  tail1: 'M59.6 51.6L70.8 54.8L61.2 58Z',
  tail2: 'M58 54.8L66 62.8L56.4 59.6Z',
};
// mirrored (facing right), scaled 0.8
const S53R = {
  farWing: 'M54.8 48.4L70.8 38.8L62.8 45.2L51.6 51.6Z',
  nearWing: 'M50 48.4L32.4 35.6L37.2 43.6L48.4 51.6Z',
  body: 'M62.8 50L48.4 46.8L40.4 51.6L45.2 56.4L59.6 54.8Z',
  belly: 'M59.6 54.8L45.2 56.4L48.4 60.4L58 58Z',
  head: 'M66 51.6L61.2 51.6L62.8 55.6L66.8 54Z',
  beak: 'M67.6 51.6L73.2 53.2L66.8 54Z',
  tail1: 'M40.4 51.6L29.2 54.8L38.8 58Z',
  tail2: 'M42 54.8L34 62.8L43.6 59.6Z',
};
const EYE = 2.6;

const newSymbols = `
  <clipPath id="cp-g58"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g59"><circle cx="50" cy="50" r="27"/></clipPath>

  <!-- G58 窗中燕:G34W 方孔弦窗为底,G53 燕身穿窗,红点=眼睛(点睛) -->
  <symbol id="mk-g58" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g58)">
    <path d="${wCh.join('')}" fill="none" stroke="var(--s1)" stroke-width="3.2" stroke-linecap="round"/>
    </g>
    <path d="${S53.farWing}" fill="var(--s2)"/>
    <path d="${S53.tail1}" fill="var(--s1)"/>
    <path d="${S53.tail2}" fill="var(--s1)"/>
    <path d="${S53.body}" fill="var(--s1)"/>
    <path d="${S53.nearWing}" fill="var(--s1)"/>
    <path d="${S53.belly}" fill="var(--s6)"/>
    <path d="${S53.head}" fill="var(--s1)"/>
    <path d="${S53.beak}" fill="var(--s1)"/>
    <circle cx="35.5" cy="53.3" r="${EYE}" fill="var(--s3)"/>
  </symbol>

  <!-- G59 咬边燕:G34B 咬边弦窗为底,燕身向缺口飞去,红点=眼睛 -->
  <symbol id="mk-g59" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g59)">
    <path d="${bCh.join('')}" fill="none" stroke="var(--s1)" stroke-width="3.2" stroke-linecap="round"/>
    </g>
    <path d="${S53R.farWing}" fill="var(--s2)"/>
    <path d="${S53R.tail1}" fill="var(--s1)"/>
    <path d="${S53R.tail2}" fill="var(--s1)"/>
    <path d="${S53R.body}" fill="var(--s1)"/>
    <path d="${S53R.nearWing}" fill="var(--s1)"/>
    <path d="${S53R.belly}" fill="var(--s6)"/>
    <path d="${S53R.head}" fill="var(--s1)"/>
    <path d="${S53R.beak}" fill="var(--s1)"/>
    <circle cx="65.5" cy="53.3" r="${EYE}" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

// marks entries
r("{id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},",
`{id:'g57', code:'G57', name:'燕蓄', gene:'SINGLE-VARIABLE · 九弦玫瑰结原样 · 仅孔轮廓圆→燕', c:'单变量迭代的答案:G34R 的九弦玫瑰结原样保留(9 弦/20° 扇/端点落外圆 r26.5/八墨一红),只把中心孔的轮廓从圆换成燕——每根弦的垂足精确落在燕形轮廓上,燕形由弦自己蓄出。与 G34R 的唯一差别,就是孔的形状。'},
  {id:'g58', code:'G58', name:'窗中燕', gene:'COMPOSITE · G34W 方孔弦窗 × G53 燕身 · 红点睛', c:'按您的配方合成:G34W 的方孔弦窗为底,写生燕(远翼蓝/体墨/腹奶油/喉朱砂)穿窗而过,头部一枚红点=眼睛的点睛之笔。弦线细至 3.2 让位给燕身。'},
  {id:'g59', code:'G59', name:'咬边燕', gene:'COMPOSITE · G34B 咬边弦窗 × G53 燕身(右飞) · 红点睛', c:'咬边版合成:燕身向缺口飞去,红点眼睛在头部——「从缺口窥入」的叙事闭环;弦线同样让位。'},`);

// matrix rows
r("  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],",
`  ['G57','燕蓄','单变量迭代',[5,5,5,4,4,5],0],
  ['G58','窗中燕','合成·点睛',[5,5,4,4,4,5],0],
  ['G59','咬边燕','合成·点睛',[4,5,4,4,4,4],0],`);

// VARIANTS family
r("['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],",
"['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],\n  ['弦窗飞燕 · 点睛', 'COMPOSITE · G34 弦窗 × G53 燕身', ['g58', 'g59']],");

fs.writeFileSync(P, h);
console.log('v11.2 two composite marks (G34W/B × G53 swallow + eye) added');
