// v9.2: five new candidates — dog-ear plate, diamond stack, phase sequence, vesica overlap, split shift
const fs = require('fs');
const P = 'C:/Users/Doro/.qwenworkcn/workspace/mtpvei2btqrqc5zn/gen8.js';
let h = fs.readFileSync(P, 'utf8');
const r = (a, b) => { if (!h.includes(a)) { console.error('MISS:', a.slice(0, 70)); process.exitCode = 1; } else h = h.split(a).join(b); };

const newSymbols = `
  <mask id="mx-g19a"><circle cx="50" cy="50" r="6" fill="white"/><circle cx="56" cy="44" r="5.5" fill="black"/></mask>
  <mask id="mx-g19b"><circle cx="68" cy="56" r="6" fill="white"/><circle cx="74" cy="50" r="6" fill="black"/></mask>

  <!-- G11 折角名牌:圆角方版切角,mint 折页盖缝,红点居中 -->
  <symbol id="mk-g11" viewBox="0 0 100 100">
    <path fill="var(--s1)" d="M35 22H56L74 40V61Q74 74 61 74H35Q22 74 22 61V35Q22 22 35 22Z"/>
    <path fill="var(--s4)" d="M56 22V40H74Z"/>
    <circle cx="48" cy="49" r="4.2" fill="var(--s3)"/>
  </symbol>

  <!-- G16 层叠菱形:三枚 45° 菱形沿对角线每层错位 6u -->
  <symbol id="mk-g16" viewBox="0 0 100 100">
    <path d="M44 24L58 38L44 52L30 38Z" fill="var(--s1)"/>
    <path d="M50 30L64 44L50 58L36 44Z" fill="var(--s2)"/>
    <path d="M56 36L70 50L56 64L42 50Z" fill="var(--s3)"/>
  </symbol>

  <!-- G19 月相序列:全圆 → 盈月 → 红新月,沿对角线推进 -->
  <symbol id="mk-g19" viewBox="0 0 100 100">
    <circle cx="32" cy="44" r="6" fill="var(--s1)"/>
    <g mask="url(#mx-g19a)"><circle cx="50" cy="50" r="6" fill="var(--s1)"/></g>
    <g mask="url(#mx-g19b)"><circle cx="68" cy="56" r="6" fill="var(--s3)"/></g>
  </symbol>

  <!-- G24 透镜交集:双环 r16 圆心距 18,透镜形交集填红 -->
  <symbol id="mk-g24" viewBox="0 0 100 100">
    <circle cx="41" cy="50" r="16" fill="none" stroke="var(--s1)" stroke-width="6.5"/>
    <circle cx="59" cy="50" r="16" fill="none" stroke="var(--s2)" stroke-width="6.5"/>
    <path d="M50 36.8A16 16 0 0 1 50 63.2A16 16 0 0 1 50 36.8Z" fill="var(--s3)"/>
  </symbol>

  <!-- G25 分屏位移:双圆角板错位 ±4u,蓝色游标居间 -->
  <symbol id="mk-g25" viewBox="0 0 100 100">
    <rect x="28" y="30" width="17" height="36" rx="6" fill="var(--s1)"/>
    <rect x="55" y="34" width="17" height="36" rx="6" fill="var(--s3)"/>
    <path d="M50 42V58" stroke="var(--s2)" stroke-width="4" stroke-linecap="round"/>
  </symbol>`;
r('  <symbol id="mk-old"', newSymbols + '\n\n  <symbol id="mk-old"');

r("{id:'g10', code:'G10', name:'黄金作图', gene:'GOLDEN CONSTRUCTION · 1:1.618 · 制图线 + 四段相切弧链', c:'把黄金矩形的完整作图过程画进图标:四级递减方格线以 22% 墨作底稿,四段相切弧链(墨→蓝→红)贯穿其上,弧链本身构成一条完整的螺旋。工程制图的仪式感。'},",
`{id:'g10', code:'G10', name:'黄金作图', gene:'GOLDEN CONSTRUCTION · 1:1.618 · 制图线 + 四段相切弧链', c:'把黄金矩形的完整作图过程画进图标:四级递减方格线以 22% 墨作底稿,四段相切弧链(墨→蓝→红)贯穿其上,弧链本身构成一条完整的螺旋。工程制图的仪式感。'},
  {id:'g11', code:'G11', name:'折角名牌', gene:'DOG-EAR PLATE · 切角 18u · 折页盖缝', c:'圆角方版右上折角,mint 折页盖在切缝上,红点钉在版心——「草稿折了一角」。名牌本身就是应用图标的原生形态,是所有系统里装裱成本最低的。'},
  {id:'g16', code:'G16', name:'层叠菱形', gene:'DIAMOND STACK · 45° 菱形 · 层间错位 6u', c:'三枚 45° 菱形沿对角线逐层错位 6u:墨、蓝、红——编辑、中间态、预览三层堆叠。零曲线,纯角度几何。'},
  {id:'g19', code:'G19', name:'月相序列', gene:'PHASE SEQUENCE · 全圆 → 盈月 → 红新月', c:'三个圆盘讲一个消减的过程:全圆是草稿,被咬掉的盈月是修改,红色新月是定稿。沿对角线推进,像时间轴被折叠进一个记号。'},
  {id:'g24', code:'G24', name:'透镜交集', gene:'VESICA OVERLAP · 双环 r16 · 圆心距 18 · 交叠红镜', c:'墨环与蓝环相交,透镜形交集填红——编辑与预览的重合处,正是 DraftPeek 站的位置。diff/merge 语义的直译,数学上是一个完美的 vesica piscis。'},
  {id:'g25', code:'G25', name:'分屏位移', gene:'SPLIT SHIFT · 双板错位 ±4u · 游标居间', c:'两块圆角板错位排布(左墨右红),中间一枚蓝色游标——左写右览的分屏对照。构成元素最少的一个候选。'},`);

r("  ['G10','黄金作图','制图过程',[3,4,5,4,3,4],0],",
`  ['G10','黄金作图','制图过程',[3,4,5,4,3,4],0],
  ['G11','折角名牌','页签折角',[4,4,4,4,5,3],0],
  ['G16','层叠菱形','层级堆叠',[4,4,4,4,4,4],0],
  ['G19','月相序列','阶段过程',[3,4,5,3,3,4],0],
  ['G24','透镜交集','对比合并',[5,5,4,4,4,5],2],
  ['G25','分屏位移','分屏对照',[4,5,3,4,5,4],0],`);

fs.writeFileSync(P, h);
console.log('v9.2 five new candidates added');
