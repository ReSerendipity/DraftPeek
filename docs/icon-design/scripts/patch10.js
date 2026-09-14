// v9.1: upgrade three "diagram-level" marks to crafted versions
// G8 spiral → tapered ribbon (width 9→1.5, four tangent centers)
// G6 quincunx → isometric blocks (30° axonometric, 3-face shading + red mini block)
// G10 golden rect → full golden construction (subdivision lines + 4-arc tangent chain)
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

// --- G8: tapered ribbon spiral ---
const g8old = h.match(/  <!-- G8 [^>]*-->[\s\S]*?<\/symbol>\n/);
if (!g8old) { console.error('G8 block missing'); process.exit(1); }
const g8new = `  <!-- G8 渐细黄金螺旋:缎带宽 9→1.5,四心相切(50,50)(50,58)(56,58)(56,54) -->
  <symbol id="mk-g8" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M21.5 50A28.5 28.5 0 0 0 50 77.5A19.5 19.5 0 0 0 68.5 58A11.5 11.5 0 0 0 56 46.5A6.75 6.75 0 0 0 49.25 54L50.75 54A5.25 5.25 0 0 1 56 49.5A8.5 8.5 0 0 1 63.5 58A12.5 12.5 0 0 1 50 70.5A19.5 19.5 0 0 1 30.5 50Z"/>
    <circle cx="53.1" cy="53.8" r="2.8" fill="var(--s3)"/>
  </symbol>
`;
h = h.replace(g8old[0], g8new);

// --- G6: isometric blocks ---
const g6old = h.match(/  <!-- G6 [^>]*-->[\s\S]*?<\/symbol>\n/);
if (!g6old) { console.error('G6 block missing'); process.exit(1); }
const g6new = `  <!-- G6 等距积木:30° 轴测,顶 mint/左墨/右蓝,红色小积木叠左肩 -->
  <symbol id="mk-g6" viewBox="0 0 100 100">
    <path d="M50 32L69 41.5L50 51L31 41.5Z" fill="var(--s4)"/>
    <path d="M31 41.5L50 51L50 69L31 59.5Z" fill="var(--s1)"/>
    <path d="M69 41.5L50 51L50 69L69 59.5Z" fill="var(--s2)"/>
    <path d="M40.5 34.7L47.5 38.2L40.5 41.7L33.5 38.2Z" fill="var(--s3)"/>
    <path d="M33.5 38.2L40.5 41.7L40.5 47.7L33.5 44.2Z" fill="var(--s3)"/>
    <path d="M47.5 38.2L40.5 41.7L40.5 47.7L47.5 44.2Z" fill="var(--s3)"/>
    <path d="M40.5 34.7L47.5 38.2M40.5 34.7L33.5 38.2" stroke="var(--pure-white)" stroke-width="1.1" opacity=".5"/>
  </symbol>
`;
h = h.replace(g6old[0], g6new);

// --- G10: golden construction ---
const g10old = h.match(/  <!-- G10 [^>]*-->[\s\S]*?<\/symbol>\n/);
if (!g10old) { console.error('G10 block missing'); process.exit(1); }
const g10new = `  <!-- G10 黄金作图:1:1.618 矩形 + 四级分割制图线(18% 墨)+ 四段相切弧链(墨→墨→蓝→红) -->
  <symbol id="mk-g10" viewBox="0 0 100 100">
    <rect x="28" y="36.4" width="44" height="27.2" rx="2.5" fill="none" stroke="var(--s1)" stroke-width="1.5" opacity=".35"/>
    <path d="M55.2 36.4V63.6M55.2 53.2H72M65.6 53.2V63.6M65.6 59.6H72" stroke="var(--s1)" stroke-width="1.2" opacity=".22"/>
    <path d="M55.2 36.4A16.8 16.8 0 0 1 72 53.2" fill="none" stroke="var(--s2)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M72 53.2A10.4 10.4 0 0 1 61.6 63.6" fill="none" stroke="var(--s2)" stroke-width="5.5" stroke-linecap="round"/>
    <path d="M61.6 63.6A6.4 6.4 0 0 1 55.2 57.2" fill="none" stroke="var(--s3)" stroke-width="5" stroke-linecap="round"/>
    <path d="M28 63.6A27.2 27.2 0 0 1 55.2 36.4" fill="none" stroke="var(--s1)" stroke-width="5.5" stroke-linecap="round"/>
  </symbol>
`;
h = h.replace(g10old[0], g10new);

// --- descriptions ---
r("{id:'g6', code:'G6', name:'五点矩阵', gene:'QUINCUNX · 9.5u 方格 · 对角 ±16u · 中心旋转 45°', c:'中心加四斜角的五点方格阵,四角按结构色分配,中心红格旋转 45° 破格。语义:模块与秩序里的一次「生成中」。'}",
"{id:'g6', code:'G6', name:'等距积木', gene:'ISOMETRIC BLOCKS · 30° 轴测 · 三面三色', c:'标准 30° 轴测积木:顶面 mint、左面墨、右面蓝,一枚红色小积木叠在左肩并带白色棱线高光——编辑器「搭建」的体块隐喻,所有斜面角度由轴测数学推出。'}");
r("{id:'g8', code:'G8', name:'连续黄金螺旋', gene:'GOLDEN SPIRAL · 弧链 r 24/16/10/6 · 切线连续', c:'四段四分之一弧收拢成一条不断线的螺旋,墨蓝两色在第三段交接,起点 mint、终点红钉在旋心。斐波那契的「生长」被还原成一笔动作,而非方块插图。'}",
"{id:'g8', code:'G8', name:'渐细黄金螺旋', gene:'GOLDEN SPIRAL RIBBON · 宽 9→1.5 渐细 · 四心相切', c:'螺旋第一次有了体积:实心缎带宽度从外端 9u 连续渐细到旋心 1.5u,四段圆弧切线连续无缝;红点钉在涡心。渐细的速率本身就是斐波那契的「生长」被画出来。'}");
r("{id:'g10', code:'G10', name:'黄金矩形', gene:'GOLDEN RECT · 1:1.618 · 单弧内接', c:'一个黄金矩形、一段内接四分之一弧、一枚红点——把比例本身压缩到三个元素。最克制的版本,适合当「工程制图」气质的备选。'}",
"{id:'g10', code:'G10', name:'黄金作图', gene:'GOLDEN CONSTRUCTION · 1:1.618 · 制图线 + 四段相切弧链', c:'把黄金矩形的完整作图过程画进图标:四级递减方格线以 22% 墨作底稿,四段相切弧链(墨→蓝→红)贯穿其上,弧链本身构成一条完整的螺旋。工程制图的仪式感。'}");
r("['G6','五点矩阵','网格破序',[4,3,5,4,4,5],0]", "['G6','等距积木','轴测搭建',[4,4,5,4,4,4],0]");
r("['G10','黄金矩形','比例极简',[3,3,5,4,3,4],0]", "['G10','黄金作图','制图过程',[3,4,5,4,3,4],0]");

fs.writeFileSync(P, h);
console.log('v9.1 three marks upgraded');
