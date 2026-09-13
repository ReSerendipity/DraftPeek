// v10.6: three barn-swallow photo-study icons — one per reference photo pose
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// add cream slot
r('--s5:#F2A93B;', '--s5:#F2A93B; --s6:#F2E7D2;');

const newSymbols = `
  <!-- G53 张翼巡弋(写生①):双翼全展浅 V 滑翔,面向左,喉部朱砂 -->
  <symbol id="mk-g53" viewBox="0 0 100 100">
    <path d="M44 48L24 36L34 44L48 52Z" fill="var(--s1)"/>
    <path d="M50 48L72 32L66 42L52 52Z" fill="var(--s1)"/>
    <path d="M34 50L52 46L62 52L56 58L38 56Z" fill="var(--s1)"/>
    <path d="M38 56L56 58L52 63L40 60Z" fill="var(--s6)"/>
    <path d="M30 52L36 52L34 57L29 55Z" fill="var(--s3)"/>
    <path d="M28 52L21 54L29 55Z" fill="var(--s1)"/>
    <path d="M62 52L76 56L64 60Z" fill="var(--s1)"/>
    <path d="M60 56L70 66L58 62Z" fill="var(--s1)"/>
  </symbol>

  <!-- G54 侧滑掠影(写生②):收翼水平滑翔,面向右,剪叉尾在后 -->
  <symbol id="mk-g54" viewBox="0 0 100 100">
    <path d="M60 42L34 28L42 40L50 48Z" fill="var(--s1)"/>
    <path d="M44 48L70 44L74 48L70 52L46 52Z" fill="var(--s1)"/>
    <path d="M46 52L70 52L68 57L48 56Z" fill="var(--s6)"/>
    <path d="M68 48L76 48L74 54L68 53Z" fill="var(--s3)"/>
    <path d="M70 44L79 47.5L71 51.5Z" fill="var(--s1)"/>
    <path d="M44 48L24 42L40 52Z" fill="var(--s1)"/>
    <path d="M44 52L26 58L42 56Z" fill="var(--s1)"/>
  </symbol>

  <!-- G55 归巢收羽(写生③):双翼上扬宽 V,尾羽下扇,面向左上 -->
  <symbol id="mk-g55" viewBox="0 0 100 100">
    <path d="M42 40L30 30L38 38L48 44Z" fill="var(--s1)"/>
    <path d="M52 42L70 28L64 38L50 44Z" fill="var(--s1)"/>
    <path d="M36 32L50 42L58 52L44 46L34 38Z" fill="var(--s1)"/>
    <path d="M40 38L46 40L44 46L38 44Z" fill="var(--s3)"/>
    <path d="M44 46L58 52L52 58L42 50Z" fill="var(--s6)"/>
    <path d="M56 52L62 58L58 72L54 56Z" fill="var(--s1)"/>
    <path d="M58 54L66 56L66 72L56 56Z" fill="var(--s1)"/>
    <path d="M60 52L72 54L72 66L58 54Z" fill="var(--s1)" opacity=".85"/>
  </symbol>`;
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

// --- marks entries + matrix rows ---
r("{id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},",
`{id:'g50a', code:'G50A', name:'羽轴扇燕', gene:'RADIAL FAN · 12 线自喉部放射 · 线端包络出燕形', c:'G34 包络逻辑的完全体:所有线共享喉部一个原点,每根线的长度恰好到燕形轮廓为止——翼尖、剪叉缺口、腹线,全由线端点「包络」而出;翼尖线红。'},
  {id:'g53', code:'G53', name:'张翼巡弋', gene:'PHOTO STUDY ① · 双翼全展浅 V · 面向左', c:'写生①的姿态:双翼全展成浅 V 滑翔,喉部朱砂、奶油腹、剪叉尾在右下——家燕巡田的标准瞬间,平涂几何化。'},
  {id:'g54', code:'G54', name:'侧滑掠影', gene:'PHOTO STUDY ② · 收翼水平滑翔 · 面向右', c:'写生②的姿态:收翼水平滑翔,剪叉尾在后,橙喙前指——全场最修长的一版,速度感来自水平轴线。'},
  {id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},`);

r("  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],",
`  ['G50A','羽轴扇燕','包络成燕',[4,4,5,4,3,5],0],
  ['G53','张翼巡弋','写生①滑翔',[5,4,4,4,4,4],0],
  ['G54','侧滑掠影','写生②速度',[4,4,4,4,5,4],0],
  ['G55','归巢收羽','写生③归巢',[4,5,4,4,4,4],0],`);

// --- add photo-study family to VARIANTS ---
r("['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 放射', ['g50c', 'g50b', 'g50a']],",
"['燕形线稿 · G34 基因', 'SWALLOW LINEART · 线描 / 排纹 / 放射', ['g50c', 'g50b', 'g50a']],\n  ['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],");

fs.writeFileSync(P, h);
console.log('v10.6 three photo-study swallow icons added');
