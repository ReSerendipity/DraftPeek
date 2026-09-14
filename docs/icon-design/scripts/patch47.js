// v10.7: three photo poses as LINE PICTOGRAPHS — horizontal lines only, mono + color versions
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// horizontal rows y = 26.5 .. 70.5 (9 rows)
const ROWS = [26.5, 32, 37.5, 43, 48.5, 54, 59.5, 65, 70.5];
function rows(colFn) {
  return ROWS.map((y, i) => `<path d="M20 ${y}H80" stroke="${colFn(i)}" stroke-width="4" stroke-linecap="round"/>`).join('\n    ');
}
const monoFn = i => i === 5 ? 'var(--s3)' : 'var(--s1)'; // red row at y=54? no: red at wing/throat level per pose below

// pose silhouettes (from G53/G54/G55) as clip paths (multi-subpath)
const clip53 = 'M44 48L24 36L34 44L48 52Z M50 48L72 32L66 42L52 52Z M34 50L52 46L62 52L56 58L38 56Z M38 56L56 58L52 63L40 60Z M30 52L36 52L34 57L29 55Z M28 52L21 54L29 55Z M62 52L76 56L64 60Z M60 56L70 66L58 62Z';
const clip54 = 'M60 42L34 28L42 40L50 48Z M44 48L70 44L74 48L70 52L46 52Z M46 52L70 52L68 57L48 56Z M68 48L76 48L74 54L68 53Z M70 44L79 47.5L71 51.5Z M44 48L24 42L40 52Z M44 52L26 58L42 56Z';
const clip55 = 'M42 40L30 30L38 38L48 44Z M52 42L70 28L64 38L50 44Z M36 32L50 42L58 52L44 46L34 38Z M40 38L46 40L44 46L38 44Z M44 46L58 52L52 58L42 50Z M56 52L62 58L58 72L54 56Z M58 54L66 56L66 72L56 56Z M60 52L72 54L72 66L58 54Z';

function poseSymbol(id, clip, colFn, overlays) {
  return `
  <symbol id="${id}" viewBox="0 0 100 100">
    <clipPath id="${id}-clip"><path d="${clip}"/></clipPath>
    <g clip-path="url(#${id}-clip)">
    ${rows(colFn)}
    ${overlays || ''}
    </g>
  </symbol>`;
}

// mono versions: all ink + one red row
const s53m = poseSymbol('mk-g53m', clip53, i => i === 4 ? 'var(--s3)' : 'var(--s1)');
const s54m = poseSymbol('mk-g54m', clip54, i => i === 3 ? 'var(--s3)' : 'var(--s1)');
const s55m = poseSymbol('mk-g55m', clip55, i => i === 3 ? 'var(--s3)' : 'var(--s1)');

// color versions: rows ink + colored overlay rows (region-accurate via silhouette clip)
const ovr = (y, col) => `<path d="M20 ${y}H80" stroke="${col}" stroke-width="4.6" stroke-linecap="round"/>`;
const s53c = poseSymbol('mk-g53c', clip53, () => 'var(--s1)',
  ovr(26.5, 'var(--s2)') + '\n    ' + ovr(32, 'var(--s2)') + '\n    ' + ovr(48.5, 'var(--s3)') + '\n    ' + ovr(54, 'var(--s6)') + '\n    ' + ovr(59.5, 'var(--s6)'));
const s54c = poseSymbol('mk-g54c', clip54, () => 'var(--s1)',
  ovr(28, 'var(--s2)').replace('M20 28H80', 'M20 28H80') + '\n    ' + ovr(32, 'var(--s2)') + '\n    ' + ovr(48.5, 'var(--s3)') + '\n    ' + ovr(54, 'var(--s6)'));
const s55c = poseSymbol('mk-g55c', clip55, () => 'var(--s1)',
  ovr(32, 'var(--s2)') + '\n    ' + ovr(37.5, 'var(--s2)') + '\n    ' + ovr(43, 'var(--s3)') + '\n    ' + ovr(54, 'var(--s6)'));

const newSymbols = [s53m, s54m, s55m, s53c, s54c, s55c].join('\n');
h = h.replace('  <!-- G43 错位环', newSymbols + '\n\n  <!-- G43 错位环');

// marks entries
r("{id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},",
`{id:'g55', code:'G55', name:'归巢收羽', gene:'PHOTO STUDY ③ · 翼上扬宽 V · 尾羽下扇', c:'写生③的姿态:双翼上扬宽 V、尾羽下扇,面向左上巢口——「归巢」的叙事;尾羽透明度分层制造扇面深度。'},
  {id:'g53m', code:'G53M', name:'巡弋·墨线', gene:'LINE PICTOGRAPH ① · 水平行象形 · 墨+单红行', c:'写生①的纯横线象形:九行水平线裁入展翼剪影,行长短参差勾出燕形;红线=预览行。'},
  {id:'g53c', code:'G53C', name:'巡弋·本色', gene:'LINE PICTOGRAPH ① · 行级羽色映射', c:'同形配色版:顶部两行蓝(背羽光泽)、喉行朱砂、腹两行奶油——照片羽色映射到行上。'},
  {id:'g54m', code:'G54M', name:'掠影·墨线', gene:'LINE PICTOGRAPH ② · 水平行象形', c:'写生②的纯横线象形:最修长的姿态由行长短勾出,剪叉尾在左。'},
  {id:'g54c', code:'G54C', name:'掠影·本色', gene:'LINE PICTOGRAPH ② · 行级羽色映射', c:'同形配色版:翼行蓝、喉行朱砂、腹行奶油。'},
  {id:'g55m', code:'G55M', name:'归巢·墨线', gene:'LINE PICTOGRAPH ③ · 水平行象形', c:'写生③的纯横线象形:上扬宽 V 与下扇尾羽全由行的伸缩完成。'},
  {id:'g55c', code:'G55C', name:'归巢·本色', gene:'LINE PICTOGRAPH ③ · 行级羽色映射', c:'同形配色版:翼两行蓝、喉行朱砂、腹行奶油。'},`);

// matrix rows
r("  ['G55','归巢收羽','写生③归巢',[4,5,4,4,4,4],0],",
`  ['G55','归巢收羽','写生③归巢',[4,5,4,4,4,4],0],
  ['G53M','巡弋·墨线','横线象形',[5,5,4,4,4,4],0],
  ['G53C','巡弋·本色','行级羽色',[5,4,4,5,4,4],0],
  ['G54M','掠影·墨线','横线象形',[4,4,4,4,4,4],0],
  ['G54C','掠影·本色','行级羽色',[4,4,4,4,4,4],0],
  ['G55M','归巢·墨线','横线象形',[4,4,4,4,4,4],0],
  ['G55C','归巢·本色','行级羽色',[4,4,4,4,4,4],0],`);

// VARIANTS family
r("['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],",
"['家燕写生三连', 'PHOTO STUDY · 一图一态', ['g53', 'g54', 'g55']],\n  ['燕形横线象形 · 双配色', 'LINE PICTOGRAPH · 一图两配色', ['g53m', 'g53c', 'g54m', 'g54c', 'g55m', 'g55c']],");

fs.writeFileSync(P, h);
console.log('v10.7 six line-pictograph swallows added');
