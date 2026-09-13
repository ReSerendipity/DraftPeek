// v10.2: three G34O hole-alternatives — ellipse with focus dot / square window / edge-bitten hole
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const fx = v => v.toFixed(1);

function envelopeLines(support, cx, cy, count, step, startDeg, half, colorFn, sw) {
  const out = [];
  for (let k = 0; k < count; k++) {
    const th = rad(startDeg + k * step);
    const n = support(th);
    const tx = cx + n * Math.cos(th), ty = cy + n * Math.sin(th);
    const dx = -Math.sin(th), dy = Math.cos(th);
    out.push(`<path d="M${fx(tx + half * dx)} ${fx(ty + half * dy)}L${fx(tx - half * dx)} ${fx(ty - half * dy)}" stroke="${colorFn(k)}" stroke-width="${sw}" stroke-linecap="round"/>`);
  }
  return out.join('\n    ');
}
const colRhythm = (k, redAt, blueAt) => k === redAt ? 'var(--s3)' : (k === blueAt ? 'var(--s2)' : 'var(--s1)');

// G34L ellipse hole: a=13 b=8 tilt -30°, center (56,44); focus dot at center + c·û, c=√(a²−b²)=10.2
const supEllipse = th => { const d = th - rad(-30); return Math.hypot(13 * Math.cos(d), 8 * Math.sin(d)); };
const g34l = envelopeLines(supEllipse, 56, 44, 9, 20, 0, 26, k => colRhythm(k, 4, 1), 3.8);
// focus: û = (cos−30, sin−30) = (.866, −.5); focus = (56+8.87, 44−5.1) = (64.9, 38.9)

// G34W square hole: half-side 9, rotated 20°, center (56,44)
const supSquare = th => { const d = th - rad(20); return 9 * Math.max(Math.abs(Math.cos(d)), Math.abs(Math.sin(d))); };
const g34w = envelopeLines(supSquare, 56, 44, 9, 20, 0, 26, k => colRhythm(k, 4, 1), 3.8);

// G34B edge-bitten hole: r11 circle at (64,44) — distance 20 + 11 > 27, hole grazes rim
const supCircle11 = () => 11;
const g34b = envelopeLines(supCircle11, 64, 44, 9, 20, 0, 26, k => colRhythm(k, 4, 1), 3.8);

const newSymbols = `
  <clipPath id="cp-g34l"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34w"><circle cx="50" cy="50" r="27"/></clipPath>
  <clipPath id="cp-g34b"><circle cx="50" cy="50" r="27"/></clipPath>

  <!-- G34L 椭圆窥孔:支撑函数蓄出倾斜椭圆孔,红点在焦点 -->
  <symbol id="mk-g34l" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34l)">
    ${g34l}
    </g>
    <circle cx="64.9" cy="38.9" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G34W 方孔窗:弦族蓄出旋转 20° 的方形孔,红点居窗心 -->
  <symbol id="mk-g34w" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34w)">
    ${g34w}
    </g>
    <circle cx="56" cy="44" r="3.6" fill="var(--s3)"/>
  </symbol>

  <!-- G34B 半孔咬边:孔在边缘被主圆裁掉一半,成月牙缺口 -->
  <symbol id="mk-g34b" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g34b)">
    ${g34b}
    </g>
    <circle cx="64" cy="44" r="3.6" fill="var(--s3)"/>
  </symbol>`;
h = h.replace('  <!-- G40 波瓣环', newSymbols + '\n\n  <!-- G40 波瓣环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };
r("'g34d', 'g34e', 'g34f', 'g34h']", "'g34d', 'g34e', 'g34f', 'g34h', 'g34l', 'g34w', 'g34b']");

r("{id:'g34h', code:'G34H', name:'对撞双扇', gene:'CLASHING FANS · 左右双扇相向 · 主圆裁切', c:'两组切线扇从左右相向推进,在对撞带交错出莫尔条纹般的张力——密度冲突产生能量,圆是它们的竞技场。'},",
`{id:'g34h', code:'G34H', name:'对撞双扇', gene:'CLASHING FANS · 左右双扇相向 · 主圆裁切', c:'两组切线扇从左右相向推进,在对撞带交错出莫尔条纹般的张力——密度冲突产生能量,圆是它们的竞技场。'},
  {id:'g34l', code:'G34L', name:'椭圆窥孔', gene:'ELLIPSE HOLE · 支撑函数蓄椭圆 · 红点在焦点', c:'G34O 孔形替代一:同一套切线构造,把孔换成倾斜椭圆(半轴 13/8),红点不放在中心而放在焦点——「焦点」双关注意力的焦点,光学语义最强。'},
  {id:'g34w', code:'G34W', name:'方孔窗', gene:'SQUARE HOLE · 蓄出旋转 20° 的方孔', c:'孔形替代二:弦族蓄出一个旋转 20° 的正方形孔——「窥孔」升级为「窥窗」,窗就是预览窗,产品语义最准。'},
  {id:'g34b', code:'G34B', name:'半孔咬边', gene:'EDGE-BITEN HOLE · 孔在边缘被裁成月牙', c:'孔形替代三:把孔推到主圆边缘,被裁掉一半,留下一个月牙缺口——窥视发生在边界上,最大胆的不对称。'},`);

r("  ['G34H','对撞双扇','密度冲突',[4,4,4,5,3,4],0],",
`  ['G34H','对撞双扇','密度冲突',[4,4,4,5,3,4],0],
  ['G34L','椭圆窥孔','焦点双关',[5,5,5,4,4,5],2],
  ['G34W','方孔窗','窗=预览',[4,5,4,4,4,4],0],
  ['G34B','半孔咬边','边界缺口',[4,4,5,4,3,4],0],`);

fs.writeFileSync(P, h);
console.log('v10.2 three hole-alternatives added');
