// v8.1: replace G8 (square tiling diagram) with three superior Fibonacci expressions:
// G8 连续黄金螺旋 (tangent quarter-arc chain), G9 叶序点阵 (phyllotaxis), G10 黄金矩形极简
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- compute phyllotaxis dot positions ---
const dots = [];
const N = 21, GA = 137.507 * Math.PI / 180, C = 6.05;
for (let i = 1; i <= N; i++) {
  const a = i * GA, rad = C * Math.sqrt(i);
  const x = (50 + rad * Math.cos(a)).toFixed(1);
  const y = (50 + rad * Math.sin(a)).toFixed(1);
  let fill = 'var(--s1)';
  if (i === 21) fill = 'var(--s3)';
  else if (i === 13) fill = 'var(--s2)';
  else if (i === 8) fill = 'var(--s4)';
  const sz = (2.5 + (i / N) * 1.1).toFixed(1);
  dots.push(`<circle cx="${x}" cy="${y}" r="${sz}" fill="${fill}"/>`);
}
const g9 = `
  <!-- G9 叶序点阵:21 点按 137.5° 黄金角 + r=c√n 精确生成 -->
  <symbol id="mk-g9" viewBox="0 0 100 100">
    ${dots.join('\n    ')}
  </symbol>`;

const oldG8 = h.match(/  <!-- G8 斐波那契方[\s\S]*?<\/symbol>/);
if (!oldG8) { console.error('G8 block not found'); process.exit(1); }
r(oldG8[0], `
  <!-- G8 连续黄金螺旋:四分之一弧链 r 24/16/10/6,切线连续 -->
  <symbol id="mk-g8" viewBox="0 0 100 100">
    <path d="M26 50A24 24 0 0 0 50 74A16 16 0 0 0 66 58" fill="none" stroke="var(--s1)" stroke-width="8" stroke-linecap="round"/>
    <path d="M66 58A10 10 0 0 0 56 48A6 6 0 0 0 50 54" fill="none" stroke="var(--s2)" stroke-width="8" stroke-linecap="round"/>
    <circle cx="26" cy="50" r="4.4" fill="var(--s4)"/>
    <circle cx="50" cy="54" r="3.4" fill="var(--s3)"/>
  </symbol>
${g9}
  <!-- G10 黄金矩形极简:1:1.618 单矩形 + 内接弧 + 红点 -->
  <symbol id="mk-g10" viewBox="0 0 100 100">
    <rect x="28" y="34.4" width="44" height="27.2" rx="4" fill="none" stroke="var(--s1)" stroke-width="6"/>
    <path d="M28 61.6A27.2 27.2 0 0 1 55.2 34.4" fill="none" stroke="var(--s2)" stroke-width="6" stroke-linecap="round"/>
    <circle cx="45" cy="52" r="4" fill="var(--s3)"/>
  </symbol>`);

// --- marks array: replace G8 entry with three ---
r(`  {id:'g8', code:'G8', name:'斐波那契方', gene:'FIBONACCI TILING · 32/20/12 · 内接四分之一弧', c:'三个正方形按斐波那契精确拼贴,mint 四分之一弧贯穿直角,琥珀点收尾。语义:草稿按数学生长成稿。'},`,
`  {id:'g8', code:'G8', name:'连续黄金螺旋', gene:'GOLDEN SPIRAL · 弧链 r 24/16/10/6 · 切线连续', c:'四段四分之一弧收拢成一条不断线的螺旋,墨蓝两色在第三段交接,起点 mint、终点红钉在旋心。斐波那契的「生长」被还原成一笔动作,而非方块插图。'},
  {id:'g9', code:'G9', name:'叶序点阵', gene:'PHYLOTAXIS · 137.5° 黄金角 · r=c√n · n=21', c:'向日葵种子排布的数学直译:21 个点按黄金角与平方根半径精确分布,点径由内向外渐增,红/蓝/mint 标记生长前沿的三个「最新」点。斐波那契最自然、最不像图表的表达。'},
  {id:'g10', code:'G10', name:'黄金矩形', gene:'GOLDEN RECT · 1:1.618 · 单弧内接', c:'一个黄金矩形、一段内接四分之一弧、一枚红点——把比例本身压缩到三个元素。最克制的版本,适合当「工程制图」气质的备选。'},`);

// --- matrix rows ---
r(`  ['G8','斐波那契方','比例拼贴',[3,4,5,3,4,4],0],`,
`  ['G8','连续黄金螺旋','一笔生长',[4,4,5,4,5,5],0],
  ['G9','叶序点阵','自然数学',[5,5,5,3,4,5],4],
  ['G10','黄金矩形','比例极简',[3,3,5,4,3,4],0],`);

fs.writeFileSync(P, h);
console.log('gen8 → v8.1 patched');
