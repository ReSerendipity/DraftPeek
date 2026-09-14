// v10.5: swallow built FROM LINES — contour stroke / horizontal raster / radial fan envelope
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const rad = d => d * Math.PI / 180;
const fx = v => v.toFixed(1);

// --- G50C 燕形线描: G50 angular vertices as one stroked contour, fork prongs red ---
const g50c = `<path d="M24 64L34 53L66 28L58 52L64 58L78 48L66 62L72 68L50 66Z" fill="none" stroke="var(--s1)" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M64 58L78 48L66 62" fill="none" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M66 62L72 68" fill="none" stroke="var(--s3)" stroke-width="4.5" stroke-linecap="round"/>`;

// --- G50B 横排纹燕: horizontal text-lines clipped by swallow silhouette (G51 scaled 0.92) ---
let bLines = [];
for (let i = 0; i < 9; i++) {
  const y = 27 + i * 5.5;
  let col = 'var(--s1)';
  if (i === 4) col = 'var(--s3)'; else if (i === 1) col = 'var(--s2)';
  bLines.push(`<path d="M21 ${y}H79" stroke="${col}" stroke-width="4" stroke-linecap="round"/>`);
}
const g50b = bLines.join('\n    ');
const g50bClip = 'M26.1 59.2Q29.2 49.7 34.8 49.7L42.3 46L60.9 27.6Q55.4 42.3 53.5 44.2L60.9 49.7L73.8 42.3L62.7 51.5L70.1 58.9Q57.2 57 49.8 55.2Q38.8 58.9 31.4 57Q26.1 57 26.1 59.2Z';

// --- G50A 羽轴扇燕: 12 rays from throat point (46,52), endpoints trace the swallow ---
const rays = [
  [-75, 20], [-55, 27], [-38, 27.5], [-22, 25], [-8, 22],
  [5, 26], [14, 15], [22, 24], [40, 18], [58, 13], [75, 9], [155, 24]
];
const OX = 46, OY = 52;
const fan = rays.map(([a, rr], i) => {
  let col = 'var(--s1)';
  if (a === -38) col = 'var(--s3)'; else if (a === 5) col = 'var(--s2)';
  const x = fx(OX + rr * Math.cos(rad(a))), y = fx(OY + rr * Math.sin(rad(a)));
  return `<path d="M${OX} ${OY}L${x} ${y}" stroke="${col}" stroke-width="4.2" stroke-linecap="round"/>`;
}).join('\n    ');

const newSymbols = `
  <clipPath id="cp-g50b"><path d="${g50bClip}"/></clipPath>

  <!-- G50C 燕形线描:一根线走完燕的轮廓,剪叉两羽红 -->
  <symbol id="mk-g50c" viewBox="0 0 100 100">
    ${g50c}
  </symbol>

  <!-- G50B 横排纹燕:水平文字行裁入燕形,红线=预览行 -->
  <symbol id="mk-g50b" viewBox="0 0 100 100">
    <g clip-path="url(#cp-g50b)">
    ${g50b}
    </g>
  </symbol>

  <!-- G50A 羽轴扇燕:12 线从喉部放射,线端包络出燕形(翼尖红) -->
  <symbol id="mk-g50a" viewBox="0 0 100 100">
    ${fan}
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- marks entries + matrix rows ---
r("{id:'g52', code:'G52', name:'燕掠弦窗', gene:'SWALLOW × ENVELOPE · 九弦窗 + 红燕掠过', c:'两个母题的嵌合:九弦弦窗是「窗」,红燕掠过是「览」——轻览的全部动作被一个瞬间收拢。'},",
`{id:'g52', code:'G52', name:'燕掠弦窗', gene:'SWALLOW × ENVELOPE · 九弦窗 + 红燕掠过', c:'两个母题的嵌合:九弦弦窗是「窗」,红燕掠过是「览」——轻览的全部动作被一个瞬间收拢。'},
  {id:'g50c', code:'G50C', name:'燕形线描', gene:'CONTOUR LINEART · 一根线走完整只燕', c:'燕子本身由一根连续的线画出:喙→头→翼尖→ back →剪叉两羽→腹,回到喙。没有任何填充——线就是燕。剪叉两羽红。'},
  {id:'g50b', code:'G50B', name:'横排纹燕', gene:'RASTER SWALLOW · 水平文字行裁入燕形', c:'最贴题的答案:燕子由九行水平「文字行」排成——文本行本身就是燕子的羽毛;红线=正在被预览的那一行。G34 横线基因与动物母题的直接合并。'},
  {id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},`);

r("  ['G52','燕掠弦窗','双母题嵌合',[4,5,4,4,4,4],0],",
`  ['G52','燕掠弦窗','双母题嵌合',[4,5,4,4,4,4],0],
  ['G50C','燕形线描','一根线成燕',[4,4,5,4,4,4],0],
  ['G50B','横排纹燕','文字行成燕',[5,5,4,4,4,4],2],
  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],`);

// --- add swallow lineart family to VARIANTS ---
r("['曲线家族 · 三种弯线美化', 'CURVE FAMILY · 蓬瓣 / 渐细 / 内旋', ['g40', 'g41', 'g42']],",
"['曲线家族 · 三种弯线美化', 'CURVE FAMILY · 蓬瓣 / 渐细 / 内旋', ['g40', 'g41', 'g42']],\n  ['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 放射', ['g50c', 'g50b', 'g50a']],");

fs.writeFileSync(P, h);
console.log('v10.5 three line-built swallows added');
